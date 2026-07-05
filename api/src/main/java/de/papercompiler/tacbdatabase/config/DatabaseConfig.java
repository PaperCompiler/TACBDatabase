package de.papercompiler.tacbdatabase.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Configuration for the PostgreSQL database connection.
 *
 * <p>Only used on the master node (Velocity). Slave nodes ignore this.
 */
public class DatabaseConfig {

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int maximumPoolSize;
    private final int minimumIdle;

    public DatabaseConfig(String jdbcUrl, String username, String password, int maximumPoolSize, int minimumIdle) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        this.username = Objects.requireNonNull(username, "username");
        this.password = Objects.requireNonNull(password, "password");
        this.maximumPoolSize = Math.max(1, maximumPoolSize);
        this.minimumIdle = Math.max(0, minimumIdle);
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public int getMinimumIdle() {
        return minimumIdle;
    }

    /**
     * Creates a DatabaseConfig from a JDBC URL string.
     *
     * <p>Format: {@code jdbc:postgresql://host:port/database}
     *
     * @param jdbcUrl     the JDBC URL
     * @param username     the database username
     * @param password     the database password
     * @param poolSize     the maximum pool size
     * @return the config
     */
    public static DatabaseConfig of(String jdbcUrl, String username, String password, int poolSize) {
        return new DatabaseConfig(jdbcUrl, username, password, poolSize, Math.max(1, poolSize / 4));
    }

    /**
     * Creates a DatabaseConfig from individual host components.
     *
     * @param host     the database host
     * @param port     the database port
     * @param database the database name
     * @param username the username
     * @param password the password
     * @param poolSize the maximum pool size
     * @return the config
     */
    public static DatabaseConfig of(String host, int port, String database, String username, String password, int poolSize) {
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        return of(jdbcUrl, username, password, poolSize);
    }
}
