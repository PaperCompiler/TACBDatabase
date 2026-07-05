package de.papercompiler.tacbdatabase;

import com.zaxxer.hikari.HikariDataSource;
import de.papercompiler.tacbdatabase.cache.CacheManager;
import de.papercompiler.tacbdatabase.config.TACBConfig;
import de.papercompiler.tacbdatabase.entity.Ban;
import de.papercompiler.tacbdatabase.entity.Economy;
import de.papercompiler.tacbdatabase.entity.Entity;
import de.papercompiler.tacbdatabase.entity.Guild;
import de.papercompiler.tacbdatabase.entity.Home;
import de.papercompiler.tacbdatabase.entity.Player;
import de.papercompiler.tacbdatabase.platform.Platform;
import de.papercompiler.tacbdatabase.platform.PlatformType;
import de.papercompiler.tacbdatabase.platform.Server;
import de.papercompiler.tacbdatabase.packet.LettucePacketManager;
import de.papercompiler.tacbdatabase.packet.PacketManager;
import de.papercompiler.tacbdatabase.pubsub.PubSubManager;
import de.papercompiler.tacbdatabase.repository.BanRepository;
import de.papercompiler.tacbdatabase.repository.CachedBanRepository;
import de.papercompiler.tacbdatabase.repository.CachedEconomyRepository;
import de.papercompiler.tacbdatabase.repository.CachedGuildRepository;
import de.papercompiler.tacbdatabase.repository.CachedHomeRepository;
import de.papercompiler.tacbdatabase.repository.CachedPlayerRepository;
import de.papercompiler.tacbdatabase.repository.EconomyRepository;
import de.papercompiler.tacbdatabase.repository.GuildRepository;
import de.papercompiler.tacbdatabase.repository.HomeRepository;
import de.papercompiler.tacbdatabase.repository.PlayerRepository;
import de.papercompiler.tacbdatabase.repository.Repository;
import de.papercompiler.tacbdatabase.repository.RepositoryFactory;
import de.papercompiler.tacbdatabase.redis.LettuceCacheManager;
import de.papercompiler.tacbdatabase.redis.LettuceFactory;
import de.papercompiler.tacbdatabase.redis.LettucePubSubManager;
import de.papercompiler.tacbdatabase.sync.SyncScheduler;
import de.papercompiler.tacbdatabase.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Main entry point for TACBDatabase.
 *
 * <p>Bootstrap pattern:
 * <pre>
 *     TACBDatabase database = TACBDatabase.bootstrap(platform, config);
 * </pre>
 *
 * <p>Architecture:
 * <ul>
 *   <li><b>Master node (Velocity)</b>: Has PostgreSQL access. Flushes Redis cache to DB periodically.</li>
 *   <li><b>Slave nodes (Paper, Minestom)</b>: Redis-only. No direct PostgreSQL access.</li>
 * </ul>
 */
public final class TACBDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TACBDatabase.class);

    private final Platform platform;
    private final PlatformType platformType;
    private final CacheManager cacheManager;
    private final PubSubManager pubSubManager;
    private final PacketManager packetManager;
    private final Map<Class<?>, Repository<?, ?>> repositories;
    private final SyncScheduler syncScheduler;
    private final Scheduler scheduler;
    private final io.lettuce.core.RedisClient redisClient;
    private final HikariDataSource dataSource;
    private final com.j256.ormlite.support.ConnectionSource connectionSource;

    private TACBDatabase(
            Platform platform,
            PlatformType platformType,
            CacheManager cacheManager,
            PubSubManager pubSubManager,
            PacketManager packetManager,
            Map<Class<?>, Repository<?, ?>> repositories,
            SyncScheduler syncScheduler,
            Scheduler scheduler,
            io.lettuce.core.RedisClient redisClient,
            HikariDataSource dataSource,
            com.j256.ormlite.support.ConnectionSource connectionSource) {
        this.platform = platform;
        this.platformType = platformType;
        this.cacheManager = cacheManager;
        this.pubSubManager = pubSubManager;
        this.packetManager = packetManager;
        this.repositories = repositories;
        this.syncScheduler = syncScheduler;
        this.scheduler = scheduler;
        this.redisClient = redisClient;
        this.dataSource = dataSource;
        this.connectionSource = connectionSource;
    }

    /**
     * Bootstraps TACBDatabase with the given platform and configuration.
     *
     * @param platform the platform implementation
     * @param config   the configuration
     * @return the initialized TACBDatabase instance
     */
    public static TACBDatabase bootstrap(Platform platform, TACBConfig config) {
        PlatformType type = PlatformType.fromPlatform(platform);

        LOGGER.info("Bootstrapping TACBDatabase on {} (type={})", platform.getServer().getName(), type);

        // Initialize Redis (required on all nodes)
        LettuceFactory.RedisComponents redisComponents = LettuceFactory.create(config.getRedis());
        CacheManager cacheManager = new LettuceCacheManager(redisComponents.client(), config.getRedis());
        PubSubManager pubSubManager = new LettucePubSubManager(redisComponents.client(), config.getRedis());

        // Initialize repositories
        RepositoryFactory.RepositoryResult result = RepositoryFactory.create(
                type, config, cacheManager, pubSubManager
        );

        // Initialize packet manager
        PacketManager packetManager = new LettucePacketManager(pubSubManager);

        // Initialize sync scheduler (master only)
        SyncScheduler syncScheduler = null;
        if (type == PlatformType.VELOCITY) {
            syncScheduler = new SyncScheduler(result.getRepositories(), cacheManager, config.getSyncInterval(), platform.getScheduler());
            syncScheduler.start();
            LOGGER.info("SyncScheduler started with interval: {}", config.getSyncInterval());
        }

        return new TACBDatabase(
                platform, type, cacheManager, pubSubManager, packetManager, result.getRepositories(), syncScheduler, platform.getScheduler(), redisComponents.client(), result.getDataSource(), result.getConnectionSource()
        );
    }

    /**
     * @return the platform this instance is running on
     */
    public Platform getPlatform() {
        return platform;
    }

    /**
     * @return the detected platform type
     */
    public PlatformType getPlatformType() {
        return platformType;
    }

    /**
     * @return true if this node is the master (Velocity proxy)
     */
    public boolean isMaster() {
        return platformType == PlatformType.VELOCITY;
    }

    /**
     * @return the Redis cache manager
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * @return the Redis pub/sub manager
     */
    public PubSubManager getPubSubManager() {
        return pubSubManager;
    }

    /**
     * @return the packet manager for cross-server communication
     */
    public PacketManager getPacketManager() {
        return packetManager;
    }

    /**
     * Returns a repository for the given entity class.
     *
     * @param entityClass the entity class
     * @param <T>         the entity type
     * @param <ID>        the entity ID type
     * @return the repository
     */
    @SuppressWarnings("unchecked")
    public <T extends Entity, ID> Repository<T, ID> getRepository(Class<T> entityClass) {
        return (Repository<T, ID>) repositories.get(entityClass);
    }

    /**
     * @return the PlayerRepository
     */
    public PlayerRepository getPlayerRepository() {
        return (PlayerRepository) repositories.get(Player.class);
    }

    /**
     * @return the GuildRepository
     */
    public GuildRepository getGuildRepository() {
        return (GuildRepository) repositories.get(Guild.class);
    }

    /**
     * @return the EconomyRepository
     */
    public EconomyRepository getEconomyRepository() {
        return (EconomyRepository) repositories.get(Economy.class);
    }

    /**
     * @return the HomeRepository
     */
    public HomeRepository getHomeRepository() {
        return (HomeRepository) repositories.get(Home.class);
    }

    /**
     * @return the BanRepository
     */
    public BanRepository getBanRepository() {
        return (BanRepository) repositories.get(Ban.class);
    }

    /**
     * Manually triggers a flush of dirty Redis data to PostgreSQL.
     * Only effective on the master node (Velocity).
     *
     * @return a future that completes when the flush is done
     */
    public java.util.concurrent.CompletableFuture<Void> flushToDatabase() {
        if (syncScheduler != null) {
            return syncScheduler.flushNow();
        }
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }

    /**
     * Shuts down all resources (connections, schedulers, etc.).
     */
    public void shutdown() {
        LOGGER.info("Shutting down TACBDatabase...");

        if (syncScheduler != null) {
            syncScheduler.stop();
        }

        packetManager.close();
        pubSubManager.close();
        cacheManager.close();

        // Close the ORMLite connection source (master only)
        // This also closes the underlying HikariCP connection pool
        if (connectionSource != null) {
            try {
                connectionSource.close();
                LOGGER.info("Closed ORMLite connection source and PostgreSQL connection pool");
            } catch (Exception e) {
                LOGGER.error("Error closing connection source", e);
            }
        }

        // Shutdown the shared Redis client last, after all connections are closed
        if (redisClient != null) {
            try {
                redisClient.shutdown();
            } catch (Exception e) {
                LOGGER.error("Error shutting down Redis client", e);
            }
        }

        LOGGER.info("TACBDatabase shut down complete.");
    }
}
