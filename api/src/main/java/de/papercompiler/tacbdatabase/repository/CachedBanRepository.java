package de.papercompiler.tacbdatabase.repository;

import com.j256.ormlite.dao.Dao;
import de.papercompiler.tacbdatabase.cache.CacheManager;
import de.papercompiler.tacbdatabase.entity.Ban;
import de.papercompiler.tacbdatabase.pubsub.PubSubManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Master-node repository for Ban: PostgreSQL + Redis cache.
 */
public class CachedBanRepository implements BanRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedBanRepository.class);
    private static final String CACHE_KEY_PREFIX = "ban:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final Dao<Ban, Long> banDao;
    private final CacheManager cacheManager;
    private final PubSubManager pubSubManager;

    public CachedBanRepository(Dao<Ban, Long> banDao, CacheManager cacheManager, PubSubManager pubSubManager) {
        this.banDao = banDao;
        this.cacheManager = cacheManager;
        this.pubSubManager = pubSubManager;
    }

    @Override
    public CompletableFuture<Optional<Ban>> findById(Long id) {
        return cacheManager.get(CACHE_KEY_PREFIX + id, Ban.class)
                .thenApply(optional -> optional.map(b -> {
                    b.setDirty(false);
                    return b;
                }))
                .exceptionally(e -> {
                    try {
                        Ban ban = banDao.queryForId(id);
                        if (ban != null) {
                            cacheManager.put(CACHE_KEY_PREFIX + id, ban, CACHE_TTL);
                            ban.setDirty(false);
                        }
                        return Optional.ofNullable(ban);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to query ban from DB", ex);
                        return Optional.empty();
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<Ban>> findActiveByUuid(UUID uuid) {
        try {
            List<Ban> results = banDao.queryForEq("targetUuid", uuid);
            Ban activeBan = results.stream()
                    .filter(b -> b.getExpiresAt() == null || b.getExpiresAt().isAfter(java.time.Instant.now()))
                    .findFirst()
                    .orElse(null);
            if (activeBan != null) {
                cacheManager.put(CACHE_KEY_PREFIX + activeBan.getId(), activeBan, CACHE_TTL);
                activeBan.setDirty(false);
            }
            return CompletableFuture.completedFuture(Optional.ofNullable(activeBan));
        } catch (Exception e) {
            LOGGER.error("Failed to query active ban by UUID from DB", e);
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    @Override
    public CompletableFuture<Optional<Ban>> findActiveByIp(String ip) {
        try {
            List<Ban> results = banDao.queryForEq("targetIp", ip);
            Ban activeBan = results.stream()
                    .filter(b -> b.getExpiresAt() == null || b.getExpiresAt().isAfter(java.time.Instant.now()))
                    .findFirst()
                    .orElse(null);
            if (activeBan != null) {
                cacheManager.put(CACHE_KEY_PREFIX + activeBan.getId(), activeBan, CACHE_TTL);
                activeBan.setDirty(false);
            }
            return CompletableFuture.completedFuture(Optional.ofNullable(activeBan));
        } catch (Exception e) {
            LOGGER.error("Failed to query active ban by IP from DB", e);
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    @Override
    public CompletableFuture<List<Ban>> findAll() {
        try {
            List<Ban> results = banDao.queryForAll();
            return CompletableFuture.completedFuture(results);
        } catch (Exception e) {
            LOGGER.error("Failed to query all bans", e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    @Override
    public CompletableFuture<Ban> save(Ban ban) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                banDao.create(ban);
                cacheManager.put(CACHE_KEY_PREFIX + ban.getId(), ban, CACHE_TTL);
                ban.setDirty(false);
                return ban;
            } catch (Exception e) {
                throw new RuntimeException("Failed to save ban", e);
            }
        });
    }

    @Override
    public CompletableFuture<Ban> update(Ban ban) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                banDao.update(ban);
                cacheManager.put(CACHE_KEY_PREFIX + ban.getId(), ban, CACHE_TTL);
                ban.setDirty(false);
                return ban;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update ban", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(Ban ban) {
        return CompletableFuture.runAsync(() -> {
            try {
                banDao.delete(ban);
                cacheManager.evict(CACHE_KEY_PREFIX + ban.getId());
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete ban", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteById(Long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                banDao.deleteById(id);
                cacheManager.evict(CACHE_KEY_PREFIX + id);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete ban by id", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return banDao.idExists(id);
            } catch (Exception e) {
                LOGGER.error("Failed to check ban existence", e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Long> count() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return banDao.countOf();
            } catch (Exception e) {
                LOGGER.error("Failed to count bans", e);
                return 0L;
            }
        });
    }
}
