package de.papercompiler.tacbdatabase.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.papercompiler.tacbdatabase.cache.CacheManager;
import de.papercompiler.tacbdatabase.config.DatabaseConfig;
import de.papercompiler.tacbdatabase.entity.*;
import de.papercompiler.tacbdatabase.platform.PlatformType;
import de.papercompiler.tacbdatabase.pubsub.PubSubManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
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
     * Result of creating repositories, including optional resources to manage.
     */
    public static final class RepositoryResult {
        private final Map<Class<?>, Repository<?, ?>> repositories;
        private final HikariDataSource dataSource;
        private final ConnectionSource connectionSource;

        public RepositoryResult(Map<Class<?>, Repository<?, ?>> repositories, HikariDataSource dataSource, ConnectionSource connectionSource) {
            this.repositories = repositories;
            this.dataSource = dataSource;
            this.connectionSource = connectionSource;
        }

        public Map<Class<?>, Repository<?, ?>> getRepositories() {
            return repositories;
        }

        public HikariDataSource getDataSource() {
            return dataSource;
        }

        public ConnectionSource getConnectionSource() {
            return connectionSource;
        }
    }

    /**
     * Creates all repositories based on the platform type.
     *
     * @param type        the platform type
     * @param config      the configuration
     * @param cacheManager the cache manager
     * @param pubSubManager the pub/sub manager
     * @return a result containing the repositories and optional data source
     */
    public static RepositoryResult create(
            PlatformType type,
            de.papercompiler.tacbdatabase.config.TACBConfig config,
            CacheManager cacheManager,
            PubSubManager pubSubManager) {

        Map<Class<?>, Repository<?, ?>> repositories = new HashMap<>();
        HikariDataSource dataSource = null;
        ConnectionSource connectionSource = null;

        if (type == PlatformType.VELOCITY) {
            // Master node: PostgreSQL + Redis cache
            MasterRepoResult masterResult = createMasterRepositories(config, cacheManager, pubSubManager, repositories);
            dataSource = masterResult.dataSource;
            connectionSource = masterResult.connectionSource;
        } else {
            // Slave node: Redis only
            createSlaveRepositories(cacheManager, pubSubManager, repositories);
        }

        return new RepositoryResult(repositories, dataSource, connectionSource);
    }

    private static MasterRepoResult createMasterRepositories(
            de.papercompiler.tacbdatabase.config.TACBConfig config,
            CacheManager cacheManager,
            PubSubManager pubSubManager,
            Map<Class<?>, Repository<?, ?>> repositories) {

        HikariDataSource dataSource = null;
        ConnectionSource connectionSource = null;

        try {
            DatabaseConfig dbConfig = config.getDatabase();

            // Create HikariCP connection pool
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(dbConfig.getJdbcUrl());
            hikariConfig.setUsername(dbConfig.getUsername());
            hikariConfig.setPassword(dbConfig.getPassword());
            hikariConfig.setMaximumPoolSize(dbConfig.getMaximumPoolSize());
            hikariConfig.setMinimumIdle(dbConfig.getMinimumIdle());
            hikariConfig.setPoolName("tacb-postgres-pool");

            dataSource = new HikariDataSource(hikariConfig);

            // Create ORMLite connection source backed by HikariCP pool
            connectionSource = new DataSourceConnectionSource((DataSource) dataSource, dbConfig.getJdbcUrl());

            // Create ORMLite DAOs
            Dao<TACBPlayer, Long> playerDao = DaoManager.createDao(connectionSource, TACBPlayer.class);
            Dao<Guild, Long> guildDao = DaoManager.createDao(connectionSource, Guild.class);
            Dao<Economy, Long> economyDao = DaoManager.createDao(connectionSource, Economy.class);
            Dao<Home, Long> homeDao = DaoManager.createDao(connectionSource, Home.class);
            Dao<Ban, Long> banDao = DaoManager.createDao(connectionSource, Ban.class);

            // Create cached repositories
            repositories.put(TACBPlayer.class, new CachedPlayerRepository(playerDao, cacheManager, pubSubManager));
            repositories.put(Guild.class, new CachedGuildRepository(guildDao, cacheManager, pubSubManager));
            repositories.put(Economy.class, new CachedEconomyRepository(economyDao, cacheManager, pubSubManager));
            repositories.put(Home.class, new CachedHomeRepository(homeDao, cacheManager, pubSubManager));
            repositories.put(Ban.class, new CachedBanRepository(banDao, cacheManager, pubSubManager));

            LOGGER.info("Created master repositories with PostgreSQL backend");
            return new MasterRepoResult(dataSource, connectionSource);
        } catch (SQLException e) {
            closeQuietly(connectionSource, "connection source");
            closeQuietly(dataSource, "HikariCP data source");
            throw new RuntimeException("Failed to create master repositories", e);
        }
    }

    private static void createSlaveRepositories(
            CacheManager cacheManager,
            PubSubManager pubSubManager,
            Map<Class<?>, Repository<?, ?>> repositories) {

        // Slave nodes use Redis-only repositories
        repositories.put(TACBPlayer.class, new RedisPlayerRepository(cacheManager, pubSubManager));
        repositories.put(Guild.class, new RedisGuildRepository(cacheManager, pubSubManager));
        repositories.put(Economy.class, new RedisEconomyRepository(cacheManager, pubSubManager));
        repositories.put(Home.class, new RedisHomeRepository(cacheManager, pubSubManager));
        repositories.put(Ban.class, new RedisBanRepository(cacheManager, pubSubManager));

        LOGGER.info("Created slave repositories with Redis-only backend");
    }

    private static void closeQuietly(AutoCloseable resource, String name) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                LOGGER.error("Failed to close {}", name, e);
            }
        }
    }

    /**
     * Helper class to return multiple values from createMasterRepositories.
     */
    private static final class MasterRepoResult {
        final HikariDataSource dataSource;
        final ConnectionSource connectionSource;

        MasterRepoResult(HikariDataSource dataSource, ConnectionSource connectionSource) {
            this.dataSource = dataSource;
            this.connectionSource = connectionSource;
        }
    }
}
