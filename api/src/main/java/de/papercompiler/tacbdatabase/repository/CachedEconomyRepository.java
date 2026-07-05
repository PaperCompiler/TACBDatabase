package de.papercompiler.tacbdatabase.repository;

import com.j256.ormlite.dao.Dao;
import de.papercompiler.tacbdatabase.cache.CacheManager;
import de.papercompiler.tacbdatabase.entity.Economy;
import de.papercompiler.tacbdatabase.pubsub.PubSubManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Master-node repository for Economy: PostgreSQL + Redis cache.
 */
public class CachedEconomyRepository implements EconomyRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedEconomyRepository.class);
    private static final String CACHE_KEY_PREFIX = "economy:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final Dao<Economy, Long> economyDao;
    private final CacheManager cacheManager;
    private final PubSubManager pubSubManager;

    public CachedEconomyRepository(Dao<Economy, Long> economyDao, CacheManager cacheManager, PubSubManager pubSubManager) {
        this.economyDao = economyDao;
        this.cacheManager = cacheManager;
        this.pubSubManager = pubSubManager;
    }

    @Override
    public CompletableFuture<Optional<Economy>> findById(Long id) {
        return cacheManager.get(CACHE_KEY_PREFIX + id, Economy.class)
                .thenApply(optional -> optional.map(e -> {
                    e.setDirty(false);
                    return e;
                }))
                .exceptionally(e -> {
                    try {
                        Economy economy = economyDao.queryForId(id);
                        if (economy != null) {
                            cacheManager.put(CACHE_KEY_PREFIX + id, economy, CACHE_TTL);
                            economy.setDirty(false);
                        }
                        return Optional.ofNullable(economy);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to query economy from DB", ex);
                        return Optional.empty();
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<Economy>> findByUuid(UUID uuid) {
        return cacheManager.get(CACHE_KEY_PREFIX + "uuid:" + uuid, Economy.class)
                .thenApply(optional -> optional.map(e -> {
                    e.setDirty(false);
                    return e;
                }))
                .exceptionally(e -> {
                    try {
                        List<Economy> results = economyDao.queryForEq("uuid", uuid);
                        Economy economy = results.isEmpty() ? null : results.get(0);
                        if (economy != null) {
                            cacheManager.put(CACHE_KEY_PREFIX + "uuid:" + uuid, economy, CACHE_TTL);
                            cacheManager.put(CACHE_KEY_PREFIX + economy.getId(), economy, CACHE_TTL);
                            economy.setDirty(false);
                        }
                        return Optional.ofNullable(economy);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to query economy by UUID from DB", ex);
                        return Optional.empty();
                    }
                });
    }

    @Override
    public CompletableFuture<Economy> adjustBalance(UUID uuid, BigDecimal amount) {
        return findByUuid(uuid).thenCompose(optional -> {
            Economy economy = optional.orElseGet(() -> new Economy(uuid, BigDecimal.ZERO, "default"));
            economy.setBalance(economy.getBalance().add(amount));
            return update(economy);
        });
    }

    @Override
    public CompletableFuture<List<Economy>> findAll() {
        try {
            List<Economy> results = economyDao.queryForAll();
            return CompletableFuture.completedFuture(results);
        } catch (Exception e) {
            LOGGER.error("Failed to query all economies", e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    @Override
    public CompletableFuture<Economy> save(Economy economy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                economyDao.create(economy);
                cacheManager.put(CACHE_KEY_PREFIX + economy.getId(), economy, CACHE_TTL);
                cacheManager.put(CACHE_KEY_PREFIX + "uuid:" + economy.getUuid(), economy, CACHE_TTL);
                economy.setDirty(false);
                return economy;
            } catch (Exception e) {
                throw new RuntimeException("Failed to save economy", e);
            }
        });
    }

    @Override
    public CompletableFuture<Economy> update(Economy economy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                economyDao.update(economy);
                cacheManager.put(CACHE_KEY_PREFIX + economy.getId(), economy, CACHE_TTL);
                cacheManager.put(CACHE_KEY_PREFIX + "uuid:" + economy.getUuid(), economy, CACHE_TTL);
                economy.setDirty(false);
                return economy;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update economy", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(Economy economy) {
        return CompletableFuture.runAsync(() -> {
            try {
                economyDao.delete(economy);
                cacheManager.evict(CACHE_KEY_PREFIX + economy.getId());
                cacheManager.evict(CACHE_KEY_PREFIX + "uuid:" + economy.getUuid());
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete economy", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteById(Long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                economyDao.deleteById(id);
                cacheManager.evict(CACHE_KEY_PREFIX + id);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete economy by id", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return economyDao.idExists(id);
            } catch (Exception e) {
                LOGGER.error("Failed to check economy existence", e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Long> count() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return economyDao.countOf();
            } catch (Exception e) {
                LOGGER.error("Failed to count economies", e);
                return 0L;
            }
        });
    }
}
