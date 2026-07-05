package de.papercompiler.tacbdatabase.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @Test
    void constructor_WithValidInputs_SetsFields() {
        DatabaseConfig config = new DatabaseConfig(
                "jdbc:postgresql://localhost:5432/tacb",
                "user",
                "pass",
                10,
                2
        );

        assertEquals("jdbc:postgresql://localhost:5432/tacb", config.getJdbcUrl());
        assertEquals("user", config.getUsername());
        assertEquals("pass", config.getPassword());
        assertEquals(10, config.getMaximumPoolSize());
        assertEquals(2, config.getMinimumIdle());
    }

    @Test
    void constructor_NullJdbcUrl_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            new DatabaseConfig(null, "user", "pass", 10, 2);
        });
    }

    @Test
    void constructor_NullUsername_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            new DatabaseConfig("jdbc:postgresql://localhost/tacb", null, "pass", 10, 2);
        });
    }

    @Test
    void constructor_NullPassword_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            new DatabaseConfig("jdbc:postgresql://localhost/tacb", "user", null, 10, 2);
        });
    }

    @Test
    void constructor_PoolSizeLessThanOne_ClampsToMinimum() {
        DatabaseConfig config = new DatabaseConfig(
                "jdbc:postgresql://localhost/tacb",
                "user",
                "pass",
                0,
                0
        );

        assertEquals(1, config.getMaximumPoolSize());
        assertEquals(0, config.getMinimumIdle());
    }

    @Test
    void constructor_NegativePoolSize_ClampsToMinimum() {
        DatabaseConfig config = new DatabaseConfig(
                "jdbc:postgresql://localhost/tacb",
                "user",
                "pass",
                -5,
                -3
        );

        assertEquals(1, config.getMaximumPoolSize());
        assertEquals(0, config.getMinimumIdle());
    }

    @Test
    void of_WithJdbcUrl_CreatesConfigWithDefaultMinIdle() {
        DatabaseConfig config = DatabaseConfig.of(
                "jdbc:postgresql://localhost/tacb",
                "user",
                "pass",
                20
        );

        assertEquals(20, config.getMaximumPoolSize());
        assertEquals(5, config.getMinimumIdle()); // 20 / 4 = 5
    }

    @Test
    void of_WithHostPortDatabase_CreatesCorrectJdbcUrl() {
        DatabaseConfig config = DatabaseConfig.of(
                "db.example.com",
                5433,
                "mydb",
                "admin",
                "secret",
                15
        );

        assertEquals("jdbc:postgresql://db.example.com:5433/mydb", config.getJdbcUrl());
        assertEquals(15, config.getMaximumPoolSize());
        assertEquals(3, config.getMinimumIdle()); // Math.max(1, 15/4) = Math.max(1, 3) = 3
    }
}
