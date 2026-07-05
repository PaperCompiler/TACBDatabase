package de.papercompiler.tacbdatabase.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.zaxxer.hikari.HikariConfig;
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
     * Result of creating repositories, including optional resources to manage.
     */
    public static final class RepositoryResult {
        private final Map<Class<?>, Repository<?, ?>> repositories;
        private final HikariDataSource dataSource;

        public RepositoryResult(Map<Class<?>, Repository<?, ?>> repositories, HikariDataSource dataSource) {
            this.repositories = repositories;
            this.dataSource = dataSource;
        }

        public Map<Class<?>, Repository<?, ?>> getRepositories() {
            return repositories;
        }

        public HikariDataSource getDataSource() {
            return dataSource;
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

        if (type == PlatformType.VELOCITY) {
            // Master node: PostgreSQL + Redis cache
            dataSource = createMasterRepositories(config, cacheManager, pubSubManager, repositories);
        } else {
            // Slave node: Redis only
            createSlaveRepositories(cacheManager, pubSubManager, repositories);
        }

        return new RepositoryResult(repositories, dataSource);
    }

    private static HikariDataSource createMasterRepositories(
            de.papercompiler.tacbdatabase.config.TACBConfig config,
            CacheManager cacheManager,
            PubSubManager pubSubManager,
            Map<Class<?>, Repository<?, ?>> repositories) {

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

            HikariDataSource dataSource = new HikariDataSource(hikariConfig);

            // Create connection source - we use the URL directly and keep the pool open
            // The HikariDataSource will be closed in TACBDatabase.shutdown()
            ConnectionSource connectionSource = new JdbcConnectionSource(
                    dbConfig.getJdbcUrl(),
                    dbConfig.getUsername(),
                    dbConfig.getPassword()
            );

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
            return dataSource;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create master repositories", e);
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
