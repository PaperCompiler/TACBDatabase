package de.papercompiler.tacbdatabase.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.papercompiler.tacbdatabase.cache.CacheManager;
import de.papercompiler.tacbdatabase.config.RedisConfig;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lettuce-based implementation of {@link CacheManager}.
 */
public class LettuceCacheManager implements CacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LettuceCacheManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final io.lettuce.core.RedisClient client;
    private final RedisConfig config;
    private final ExecutorService executor;
    private volatile StatefulRedisConnection<String, String> connection;
    private volatile RedisCommands<String, String> syncCommands;

    public LettuceCacheManager(io.lettuce.core.RedisClient client, RedisConfig config) {
        this.client = client;
        this.config = config;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "tacb-cache-" + r.hashCode());
            t.setDaemon(true);
            return t;
        });
    }

    private synchronized void ensureConnected() {
        if (connection == null || !connection.isOpen()) {
            // Close existing connection if it exists but is not open
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    LOGGER.debug("Error closing stale Redis connection", e);
                }
            }
            connection = client.connect(StringCodec.UTF8);
            syncCommands = connection.sync();
            LOGGER.debug("Connected to Redis for cache operations");
        }
    }

    @Override
    public <T> CompletableFuture<Optional<T>> get(String key, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConnected();
                String value = syncCommands.get(key);
                if (value == null) {
                    return Optional.empty();
                }
                return Optional.of(MAPPER.readValue(value, type));
            } catch (Exception e) {
                LOGGER.error("Failed to get cache key: {}", key, e);
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public <T> CompletableFuture<Void> put(String key, T value, Duration ttl) {
        return CompletableFuture.runAsync(() -> {
            try {
                ensureConnected();
                String json = MAPPER.writeValueAsString(value);
                if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                    syncCommands.setex(key, ttl.getSeconds(), json);
                } else {
                    syncCommands.set(key, json);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize cache value", e);
            } catch (Exception e) {
                LOGGER.error("Failed to put cache key: {}", key, e);
            }
        }, executor);
    }

    @Override
    public <T> CompletableFuture<Void> put(String key, T value) {
        return put(key, value, null);
    }

    @Override
    public CompletableFuture<Void> evict(String key) {
        return CompletableFuture.runAsync(() -> {
            try {
                ensureConnected();
                syncCommands.del(key);
            } catch (Exception e) {
                LOGGER.error("Failed to evict cache key: {}", key, e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> evictPattern(String pattern) {
        return CompletableFuture.runAsync(() -> {
            try {
                ensureConnected();
                // Use SCAN to find matching keys and delete them
                var scanArgs = io.lettuce.core.ScanArgs.Builder.matches(pattern);
                var scanCursor = syncCommands.scan(scanArgs);
                while (!scanCursor.isFinished()) {
                    for (String key : scanCursor.getKeys()) {
                        syncCommands.del(key);
                    }
                    scanCursor = syncCommands.scan(scanCursor);
                }
                // Process final batch of keys
                for (String key : scanCursor.getKeys()) {
                    syncCommands.del(key);
                }
                LOGGER.debug("Evicted all keys matching pattern: {}", pattern);
            } catch (Exception e) {
                LOGGER.error("Failed to evict cache pattern: {}", pattern, e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> flushAll() {
        return CompletableFuture.runAsync(() -> {
            try {
                ensureConnected();
                syncCommands.flushall();
                LOGGER.info("Flushed all cache data");
            } catch (Exception e) {
                LOGGER.error("Failed to flush cache", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Set<String>> getKeysByPattern(String pattern) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConnected();
                Set<String> keys = new HashSet<>();
                var scanArgs = io.lettuce.core.ScanArgs.Builder.matches(pattern);
                var scanCursor = syncCommands.scan(scanArgs);
                while (!scanCursor.isFinished()) {
                    keys.addAll(scanCursor.getKeys());
                    scanCursor = syncCommands.scan(scanCursor);
                }
                // Add final batch of keys
                keys.addAll(scanCursor.getKeys());
                return keys;
            } catch (Exception e) {
                LOGGER.error("Failed to get keys by pattern: {}", pattern, e);
                return new HashSet<>();
            }
        }, executor);
    }

    @Override
    public void close() {
        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
            // Note: We do NOT shut down the client here as it's shared with PubSubManager
            executor.shutdown();
            LOGGER.info("LettuceCacheManager closed");
        } catch (Exception e) {
            LOGGER.error("Error closing LettuceCacheManager", e);
        }
    }
}
