package de.papercompiler.tacbdatabase.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.zaxxer.hikari.HikariDataSource;
import de.papercompiler.tacbdatabase.cache.CacheManager;
import de.papercompiler.tacbdatabase.config.DatabaseConfig;
import de.papercompiler.tacbdatabase.entity.Ban;
import de.papercompiler.tacbdatabase.entity.Economy;
import de.papercompiler.tacbdatabase.entity.Guild;
import de.papercompiler.tacbdatabase.entity.Home;
import de.papercompiler.tacbdatabase.entity.Player;
import de.papercompiler.tacbdatabase.platform.PlatformType;
import de.papercompiler.tacbdatabase.pubsub.PubSubManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating repository instances.
 */
public final class RepositoryFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryFactory.class);

    private RepositoryFactory() {
    }

    /**
     * Creates all repositories based on the platform type.
     *
     * @param type        the platform type
     * @param config      the configuration
     * @param cacheManager the cache manager
     * @param pubSubManager the pub/sub manager
     * @return a map of entity class to repository
     */
    public static Map<Class<?>, Repository<?, ?>> create(
            PlatformType type,
            de.papercompiler.tacbdatabase.config.TACBConfig config,
            CacheManager cacheManager,
            PubSubManager pubSubManager) {

        Map<Class<?>, Repository<?, ?>> repositories = new HashMap<>();

        if (type == PlatformType.VELOCITY) {
            // Master node: PostgreSQL + Redis cache
            createMasterRepositories(config, cacheManager, pubSubManager, repositories);
        } else {
            // Slave node: Redis only
            createSlaveRepositories(cacheManager, pubSubManager, repositories);
        }

        return repositories;
    }

    private static void createMasterRepositories(
            de.papercompiler.tacbdatabase.config.TACBConfig config,
            CacheManager cacheManager,
            PubSubManager pubSubManager,
            Map<Class<?>, Repository<?, ?>> repositories) {

        HikariDataSource dataSource = null;
        ConnectionSource connectionSource = null;

        try {
            DatabaseConfig dbConfig = config.getDatabase();
            dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(dbConfig.getJdbcUrl());
            dataSource.setUsername(dbConfig.getUsername());
            dataSource.setPassword(dbConfig.getPassword());
            dataSource.setMaximumPoolSize(dbConfig.getMaximumPoolSize());
            dataSource.setMinimumIdle(dbConfig.getMinimumIdle());

            // Use HikariCP DataSource with JdbcConnectionSource
            connectionSource = new JdbcConnectionSource(dataSource);

            // Create ORMLite DAOs
            Dao<Player, Long> playerDao = DaoManager.createDao(connectionSource, Player.class);
            Dao<Guild, Long> guildDao = DaoManager.createDao(connectionSource, Guild.class);
            Dao<Economy, Long> economyDao = DaoManager.createDao(connectionSource, Economy.class);
            Dao<Home, Long> homeDao = DaoManager.createDao(connectionSource, Home.class);
            Dao<Ban, Long> banDao = DaoManager.createDao(connectionSource, Ban.class);

            // Create cached repositories
            repositories.put(Player.class, new CachedPlayerRepository(playerDao, cacheManager, pubSubManager));
            repositories.put(Guild.class, new CachedGuildRepository(guildDao, cacheManager, pubSubManager));
            repositories.put(Economy.class, new CachedEconomyRepository(economyDao, cacheManager, pubSubManager));
            repositories.put(Home.class, new CachedHomeRepository(homeDao, cacheManager, pubSubManager));
            repositories.put(Ban.class, new CachedBanRepository(banDao, cacheManager, pubSubManager));

            LOGGER.info("Created master repositories with PostgreSQL backend");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create master repositories", e);
        } finally {
            // Close connection source but keep dataSource alive for the lifetime of the app
            if (connectionSource != null) {
                try {
                    connectionSource.close();
                } catch (Exception e) {
                    LOGGER.error("Failed to close connection source", e);
                }
            }
        }
    }

    private static void createSlaveRepositories(
            CacheManager cacheManager,
            PubSubManager pubSubManager,
            Map<Class<?>, Repository<?, ?>> repositories) {

        // Slave nodes use Redis-only repositories
        repositories.put(Player.class, new RedisPlayerRepository(cacheManager, pubSubManager));
        repositories.put(Guild.class, new RedisGuildRepository(cacheManager, pubSubManager));
        repositories.put(Economy.class, new RedisEconomyRepository(cacheManager, pubSubManager));
        repositories.put(Home.class, new RedisHomeRepository(cacheManager, pubSubManager));
        repositories.put(Ban.class, new RedisBanRepository(cacheManager, pubSubManager));

        LOGGER.info("Created slave repositories with Redis-only backend");
    }
}
