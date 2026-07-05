package de.papercompiler.tacbdatabase.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.papercompiler.tacbdatabase.config.RedisConfig;
import de.papercompiler.tacbdatabase.pubsub.MessageHandler;
import de.papercompiler.tacbdatabase.pubsub.PubSubManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.ConcurrentHashMap.newKeySet;

/**
 * Lettuce-based implementation of {@link PubSubManager}.
 */
public class LettucePubSubManager implements PubSubManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LettucePubSubManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final RedisClient client;
    private final RedisConfig config;
    private final ExecutorService executor;
    private final Map<String, MessageHandler> handlers = new HashMap<>();
    private final Set<String> subscribedChannels = newKeySet();
    private volatile StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private volatile RedisPubSubListener<String, String> listener;
    private volatile StatefulRedisConnection<String, String> publishConnection;

    public LettucePubSubManager(RedisClient client, RedisConfig config) {
        this.client = client;
        this.config = config;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "tacb-pubsub-" + r.hashCode());
            t.setDaemon(true);
            return t;
        });
    }

    private synchronized void ensureConnected() {
        if (pubSubConnection == null || !pubSubConnection.isOpen()) {
            pubSubConnection = client.connectPubSub(StringCodec.UTF8);
            listener = new RedisPubSubListener<>() {
                @Override
                public void message(String channel, String message) {
                    MessageHandler handler = handlers.get(channel);
                    if (handler != null) {
                        executor.execute(() -> handler.onMessage(channel, message));
                    }
                }

                @Override
                public void message(String pattern, String channel, String message) {
                    // Pattern messages not used in this implementation
                }

                @Override
                public void subscribed(String channel, long count) {
                    LOGGER.debug("Subscribed to channel: {} (count={})", channel, count);
                }

                @Override
                public void psubscribed(String pattern, long count) {
                    // Pattern subscriptions not used
                }

                @Override
                public void unsubscribed(String channel, long count) {
                    LOGGER.debug("Unsubscribed from channel: {} (count={})", channel, count);
                }

                @Override
                public void punsubscribed(String pattern, long count) {
                    // Pattern unsubscriptions not used
                }
            };
            pubSubConnection.addListener(listener);
            LOGGER.debug("Connected to Redis for pub/sub");
        }
    }

    private synchronized void ensurePublishConnected() {
        if (publishConnection == null || !publishConnection.isOpen()) {
            publishConnection = client.connect(StringCodec.UTF8);
        }
    }

    @Override
    public CompletableFuture<Void> subscribe(String channel, MessageHandler handler) {
        return CompletableFuture.runAsync(() -> {
            try {
                ensureConnected();
                handlers.put(channel, handler);
                subscribedChannels.add(channel);
                RedisPubSubCommands<String, String> commands = pubSubConnection.sync();
                commands.subscribe(channel);
            } catch (Exception e) {
                LOGGER.error("Failed to subscribe to channel: {}", channel, e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> unsubscribe(String channel) {
        return CompletableFuture.runAsync(() -> {
            try {
                handlers.remove(channel);
                subscribedChannels.remove(channel);
                if (pubSubConnection != null && pubSubConnection.isOpen()) {
                    RedisPubSubCommands<String, String> commands = pubSubConnection.sync();
                    commands.unsubscribe(channel);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to unsubscribe from channel: {}", channel, e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> publish(String channel, Object message) {
        return CompletableFuture.runAsync(() -> {
            try {
                ensurePublishConnected();
                String json = MAPPER.writeValueAsString(message);
                RedisCommands<String, String> commands = publishConnection.sync();
                commands.publish(channel, json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize pub/sub message", e);
            } catch (Exception e) {
                LOGGER.error("Failed to publish to channel: {}", channel, e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> publish(String channel, byte[] data) {
        return CompletableFuture.runAsync(() -> {
            try {
                ensurePublishConnected();
                RedisCommands<String, String> commands = publishConnection.sync();
                commands.publish(channel, new String(data, java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception e) {
                LOGGER.error("Failed to publish to channel: {}", channel, e);
            }
        }, executor);
    }

    @Override
    public void close() {
        try {
            // Unsubscribe from all channels
            if (pubSubConnection != null && pubSubConnection.isOpen()) {
                RedisPubSubCommands<String, String> commands = pubSubConnection.sync();
                for (String channel : subscribedChannels) {
                    commands.unsubscribe(channel);
                }
                pubSubConnection.close();
            }
            if (publishConnection != null && publishConnection.isOpen()) {
                publishConnection.close();
            }
            // Note: We do NOT shut down the client here as it's shared with CacheManager
            executor.shutdown();
            LOGGER.info("LettucePubSubManager closed");
        } catch (Exception e) {
            LOGGER.error("Error closing LettucePubSubManager", e);
        }
    }
}
