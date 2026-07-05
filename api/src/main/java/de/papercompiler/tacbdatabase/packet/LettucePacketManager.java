package de.papercompiler.tacbdatabase.packet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.papercompiler.tacbdatabase.pubsub.PubSubManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Lettuce-based implementation of {@link PacketManager}.
 */
public class LettucePacketManager implements PacketManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LettucePacketManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final PubSubManager pubSubManager;
    private final Map<Class<?>, PacketHandler<?>> handlers = new HashMap<>();

    public LettucePacketManager(PubSubManager pubSubManager) {
        this.pubSubManager = pubSubManager;
    }

    @Override
        public <T extends Packet> CompletableFuture<Void> send(T packet) {
            try {
                return pubSubManager.publish(packet.getChannel(), packet.serialize());
            } catch (Exception e) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new RuntimeException("Failed to serialize packet", e));
                return future;
            }
        }

    @Override
    public <T extends Packet> void registerHandler(Class<T> type, PacketHandler<T> handler) {
        handlers.put(type, handler);

        // Subscribe to the channel of a sample packet instance
        try {
            T sample = type.getDeclaredConstructor().newInstance();
            String channel = sample.getChannel();

            pubSubManager.subscribe(channel, (ch, message) -> {
                try {
                    T received = MAPPER.readValue(message, type);
                    handler.handle(received);
                } catch (Exception e) {
                    LOGGER.error("Failed to deserialize packet on channel {}: {}", ch, message, e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to register handler for packet type: " + type.getName(), e);
        }
    }

    @Override
    public <T extends Packet> void unregisterHandler(Class<T> type) {
        handlers.remove(type);
    }

    @Override
    public void close() {
        // Handlers are cleaned up by PubSubManager
    }
}
