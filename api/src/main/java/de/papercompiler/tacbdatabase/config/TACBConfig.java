package de.papercompiler.tacbdatabase.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

/**
 * Top-level configuration for TACBDatabase.
 */
public class TACBConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(TACBConfig.class);

    private final DatabaseConfig database;
    private final RedisConfig redis;
    private final Duration syncInterval;

    public TACBConfig(DatabaseConfig database, RedisConfig redis, Duration syncInterval) {
        this.database = database;
        this.redis = redis;
        this.syncInterval = syncInterval != null ? syncInterval : Duration.ofMinutes(5);
    }

    /**
     * @return the PostgreSQL configuration (only used on master node)
     */
    public DatabaseConfig getDatabase() {
        return database;
    }

    /**
     * @return the Redis configuration (used on all nodes)
     */
    public RedisConfig getRedis() {
        return redis;
    }

    /**
     * @return the interval at which the master node flushes dirty Redis data to PostgreSQL
     */
    public Duration getSyncInterval() {
        return syncInterval;
    }

    /**
     * Creates a new config builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Loads configuration from a file.
     *
     * @param configFile the path to the config file
     * @return the loaded configuration
     */
    public static TACBConfig load(Path configFile) {
        Properties props = new Properties();
        try {
            if (Files.exists(configFile)) {
                props.load(Files.newInputStream(configFile));
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load config file: {}, using defaults", configFile, e);
        }

        // Parse Redis config
        String redisHost = props.getProperty("redis.host", "localhost");
        int redisPort = Integer.parseInt(props.getProperty("redis.port", "6379"));
        String redisPassword = props.getProperty("redis.password", null);
        int redisDatabase = Integer.parseInt(props.getProperty("redis.database", "0"));
        RedisConfig redisConfig = RedisConfig.of(redisHost, redisPort, redisPassword, redisDatabase, 10, 5);

        // Parse database config (optional, only for master)
        DatabaseConfig databaseConfig = null;
        String dbUrl = props.getProperty("database.url");
        if (dbUrl != null) {
            String dbUser = props.getProperty("database.user", "user");
            String dbPass = props.getProperty("database.password", "password");
            int dbPoolSize = Integer.parseInt(props.getProperty("database.pool-size", "10"));
            databaseConfig = DatabaseConfig.of(dbUrl, dbUser, dbPass, dbPoolSize);
        }

        // Parse sync interval
        int syncSeconds = Integer.parseInt(props.getProperty("sync.interval-seconds", "300"));
        Duration syncInterval = Duration.ofSeconds(syncSeconds);

        return new TACBConfig(databaseConfig, redisConfig, syncInterval);
    }

    /**
     * Builder for {@link TACBConfig}.
     */
    public static class Builder {
        private DatabaseConfig database;
        private RedisConfig redis;
        private Duration syncInterval = Duration.ofMinutes(5);

        public Builder database(DatabaseConfig database) {
            this.database = database;
            return this;
        }

        public Builder redis(RedisConfig redis) {
            this.redis = redis;
            return this;
        }

        public Builder syncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
            return this;
        }

        public TACBConfig build() {
            if (redis == null) {
                throw new IllegalStateException("Redis config is required");
            }
            return new TACBConfig(database, redis, syncInterval);
        }
    }
}
