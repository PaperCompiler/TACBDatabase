package de.papercompiler.tacbdatabase.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class TACBConfigTest {

    @Test
    void builder_WithAllFields_BuildsSuccessfully() {
        RedisConfig redis = RedisConfig.of("localhost", 6379);
        DatabaseConfig database = DatabaseConfig.of(
                "jdbc:postgresql://localhost:5432/tacb",
                "user",
                "pass",
                10
        );

        TACBConfig config = TACBConfig.builder()
                .redis(redis)
                .database(database)
                .syncInterval(Duration.ofMinutes(10))
                .build();

        assertNotNull(config.getRedis());
        assertNotNull(config.getDatabase());
        assertEquals(Duration.ofMinutes(10), config.getSyncInterval());
    }

    @Test
    void builder_WithoutRedis_ThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> {
            TACBConfig.builder().build();
        });
    }

    @Test
    void builder_WithoutDatabase_UsesNullDatabase() {
        RedisConfig redis = RedisConfig.of("localhost", 6379);

        TACBConfig config = TACBConfig.builder()
                .redis(redis)
                .build();

        assertNotNull(config.getRedis());
        assertNull(config.getDatabase());
    }

    @Test
    void constructor_WithNullSyncInterval_UsesDefault() {
        RedisConfig redis = RedisConfig.of("localhost", 6379);

        TACBConfig config = new TACBConfig(null, redis, null);

        assertEquals(Duration.ofMinutes(5), config.getSyncInterval());
    }

    @Test
    void load_FromPropertiesFile_LoadsCorrectly() throws IOException {
        Path tempFile = Files.createTempFile("tacb-config", ".properties");
        Files.writeString(tempFile, """
                redis.host=redis.example.com
                redis.port=6380
                redis.password=secret
                redis.database=1
                database.url=jdbc:postgresql://db.example.com:5432/tacb
                database.user=admin
                database.password=adminpass
                database.pool-size=20
                sync.interval-seconds=600
                """);

        TACBConfig config = TACBConfig.load(tempFile);

        assertEquals("redis.example.com", config.getRedis().getHost());
        assertEquals(6380, config.getRedis().getPort());
        assertEquals("secret", config.getRedis().getPassword());
        assertEquals(1, config.getRedis().getDatabase());
        assertNotNull(config.getDatabase());
        assertEquals("jdbc:postgresql://db.example.com:5432/tacb", config.getDatabase().getJdbcUrl());
        assertEquals("admin", config.getDatabase().getUsername());
        assertEquals(Duration.ofMinutes(10), config.getSyncInterval());

        Files.deleteIfExists(tempFile);
    }

    @Test
    void load_FromNonExistentFile_UsesDefaults() {
        Path nonExistent = Path.of("/tmp/nonexistent-tacb-config.properties");

        TACBConfig config = TACBConfig.load(nonExistent);

        assertEquals("localhost", config.getRedis().getHost());
        assertEquals(6379, config.getRedis().getPort());
        assertNull(config.getRedis().getPassword());
        assertEquals(0, config.getRedis().getDatabase());
        assertNull(config.getDatabase());
        assertEquals(Duration.ofMinutes(5), config.getSyncInterval());
    }

    @Test
    void load_WithOnlyRedis_UsesDefaultsForOthers() throws IOException {
        Path tempFile = Files.createTempFile("tacb-config", ".properties");
        Files.writeString(tempFile, """
                redis.host=myredis
                redis.port=6379
                """);

        TACBConfig config = TACBConfig.load(tempFile);

        assertEquals("myredis", config.getRedis().getHost());
        assertNull(config.getDatabase());
        assertEquals(Duration.ofMinutes(5), config.getSyncInterval());

        Files.deleteIfExists(tempFile);
    }
}
