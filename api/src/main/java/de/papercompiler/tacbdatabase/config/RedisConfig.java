package de.papercompiler.tacbdatabase.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for the Redis connection.
 */
public class RedisConfig {

    private final String host;
    private final int port;
    private final String password;
    private final int database;
    private final int maxConnections;
    private final long timeoutMillis;

    public RedisConfig(String host, int port, String password, int database, int maxConnections, long timeoutMillis) {
        this.host = Objects.requireNonNull(host, "host");
        this.port = Math.max(1, Math.min(65535, port));
        this.password = password; // nullable
        this.database = Math.max(0, database);
        this.maxConnections = Math.max(1, maxConnections);
        this.timeoutMillis = Math.max(100, timeoutMillis);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public int getDatabase() {
        return database;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * @return the Redis URI for Lettuce
     */
    public String toUri() {
        try {
            URI uri = new URI("redis", null, host, port, "/" + database, null, null);
            if (password != null && !password.isEmpty()) {
                return "redis://:" + password + "@" + host + ":" + port + "/" + database;
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid Redis config", e);
        }
    }

    /**
     * Creates a RedisConfig with default values.
     *
     * @param host the Redis host
     * @param port the Redis port
     * @return the config
     */
    public static RedisConfig of(String host, int port) {
        return new RedisConfig(host, port, null, 0, 10, 5000);
    }

    /**
     * Creates a RedisConfig with all options.
     *
     * @param host           the Redis host
     * @param port           the Redis port
     * @param password       the Redis password (nullable)
     * @param database       the Redis database index
     * @param maxConnections the max connections in the pool
     * @param timeoutSeconds the command timeout in seconds
     * @return the config
     */
    public static RedisConfig of(String host, int port, String password, int database, int maxConnections, long timeoutSeconds) {
        return new RedisConfig(host, port, password, database, maxConnections, TimeUnit.SECONDS.toMillis(timeoutSeconds));
    }
}
