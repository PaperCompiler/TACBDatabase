package de.papercompiler.tacbdatabase.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Factory for creating Lettuce Redis components.
 */
public final class LettuceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(LettuceFactory.class);

    private LettuceFactory() {
    }

    /**
     * Creates Redis components (client + connection).
     *
     * @param redisConfig the Redis configuration
     * @return the components
     */
    public static RedisComponents create(de.papercompiler.tacbdatabase.config.RedisConfig redisConfig) {
        Objects.requireNonNull(redisConfig, "redisConfig");

        RedisURI uri = RedisURI.create(redisConfig.toUri());

        RedisClient client = RedisClient.create(uri);

        LOGGER.info("Created Lettuce RedisClient for {}:{}", redisConfig.getHost(), redisConfig.getPort());

        return new RedisComponents(client, uri);
    }

    /**
     * Holds created Redis components.
     */
    public record RedisComponents(RedisClient client, RedisURI uri) {
    }
}
