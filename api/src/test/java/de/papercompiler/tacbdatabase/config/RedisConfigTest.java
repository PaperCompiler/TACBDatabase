package de.papercompiler.tacbdatabase.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RedisConfigTest {

    @Test
    void constructor_WithValidInputs_SetsFields() {
        RedisConfig config = new RedisConfig(
                "localhost",
                6379,
                "secret",
                1,
                20,
                5000
        );

        assertEquals("localhost", config.getHost());
        assertEquals(6379, config.getPort());
        assertEquals("secret", config.getPassword());
        assertEquals(1, config.getDatabase());
        assertEquals(20, config.getMaxConnections());
        assertEquals(5000, config.getTimeoutMillis());
    }

    @Test
    void constructor_PortBelowOne_ClampsToOne() {
        RedisConfig config = new RedisConfig("localhost", 0, null, 0, 10, 5000);

        assertEquals(1, config.getPort());
    }

    @Test
    void constructor_PortAbove65535_ClampsTo65535() {
        RedisConfig config = new RedisConfig("localhost", 70000, null, 0, 10, 5000);

        assertEquals(65535, config.getPort());
    }

    @Test
    void constructor_DatabaseBelowZero_ClampsToZero() {
        RedisConfig config = new RedisConfig("localhost", 6379, null, -1, 10, 5000);

        assertEquals(0, config.getDatabase());
    }

    @Test
    void constructor_MaxConnectionsBelowOne_ClampsToOne() {
        RedisConfig config = new RedisConfig("localhost", 6379, null, 0, 0, 5000);

        assertEquals(1, config.getMaxConnections());
    }

    @Test
    void constructor_TimeoutBelow100_ClampsTo100() {
        RedisConfig config = new RedisConfig("localhost", 6379, null, 0, 10, 50);

        assertEquals(100, config.getTimeoutMillis());
    }

    @Test
    void of_WithHostAndPort_CreatesConfigWithDefaults() {
        RedisConfig config = RedisConfig.of("redis.example.com", 6380);

        assertEquals("redis.example.com", config.getHost());
        assertEquals(6380, config.getPort());
        assertNull(config.getPassword());
        assertEquals(0, config.getDatabase());
        assertEquals(10, config.getMaxConnections());
        assertEquals(5000, config.getTimeoutMillis());
    }

    @Test
    void of_WithAllOptions_ConvertsTimeoutFromSecondsToMillis() {
        RedisConfig config = RedisConfig.of(
                "localhost",
                6379,
                "pass",
                2,
                15,
                10
        );

        assertEquals(TimeUnit.SECONDS.toMillis(10), config.getTimeoutMillis());
    }

    @Test
    void toUri_WithoutPassword_ReturnsStandardUri() {
        RedisConfig config = RedisConfig.of("localhost", 6379);

        String uri = config.toUri();

        assertEquals("redis://localhost:6379/0", uri);
    }

    @Test
    void toUri_WithPassword_ReturnsPasswordInUri() {
        RedisConfig config = new RedisConfig("localhost", 6379, "mypassword", 0, 10, 5000);

        String uri = config.toUri();

        assertEquals("redis://:mypassword@localhost:6379/0", uri);
    }

    @Test
    void toUri_WithDatabase_IncludesDatabaseInPath() {
        RedisConfig config = new RedisConfig("localhost", 6379, null, 3, 10, 5000);

        String uri = config.toUri();

        assertEquals("redis://localhost:6379/3", uri);
    }

    @Test
    void toUri_WithCustomHostAndPort_FormatsCorrectly() {
        RedisConfig config = new RedisConfig("redis.example.com", 6380, null, 1, 10, 5000);

        String uri = config.toUri();

        assertEquals("redis://redis.example.com:6380/1", uri);
    }
}
