package de.papercompiler.tacbdatabase.repository;

import com.j256.ormlite.dao.Dao;
import de.papercompiler.tacbdatabase.cache.CacheManager;
import de.papercompiler.tacbdatabase.entity.Home;
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
 * Master-node repository for Home: PostgreSQL + Redis cache.
 */
public class CachedHomeRepository implements HomeRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedHomeRepository.class);
    private static final String CACHE_KEY_PREFIX = "home:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final Dao<Home, Long> homeDao;
    private final CacheManager cacheManager;
    private final PubSubManager pubSubManager;

    public CachedHomeRepository(Dao<Home, Long> homeDao, CacheManager cacheManager, PubSubManager pubSubManager) {
        this.homeDao = homeDao;
        this.cacheManager = cacheManager;
        this.pubSubManager = pubSubManager;
    }

    @Override
    public CompletableFuture<Optional<Home>> findById(Long id) {
        return cacheManager.get(CACHE_KEY_PREFIX + id, Home.class)
                .thenApply(optional -> optional.map(h -> {
                    h.setDirty(false);
                    return h;
                }))
                .exceptionally(e -> {
                    try {
                        Home home = homeDao.queryForId(id);
                        if (home != null) {
                            cacheManager.put(CACHE_KEY_PREFIX + id, home, CACHE_TTL);
                            home.setDirty(false);
                        }
                        return Optional.ofNullable(home);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to query home from DB", ex);
                        return Optional.empty();
                    }
                });
    }

    @Override
    public CompletableFuture<List<Home>> findByPlayer(UUID uuid) {
        try {
            List<Home> results = homeDao.queryForEq("uuid", uuid);
            results.forEach(h -> {
                try {
                    cacheManager.put(CACHE_KEY_PREFIX + h.getId(), h, CACHE_TTL);
                    h.setDirty(false);
                } catch (Exception ex) {
                    LOGGER.error("Failed to cache home", ex);
                }
            });
            return CompletableFuture.completedFuture(results);
        } catch (Exception e) {
            LOGGER.error("Failed to query homes by player from DB", e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    @Override
    public CompletableFuture<Optional<Home>> findByName(UUID uuid, String name) {
        try {
            List<Home> results = homeDao.queryBuilder()
                    .where()
                    .eq("uuid", uuid)
                    .and()
                    .eq("name", name)
                    .query();
            Home home = results.isEmpty() ? null : results.get(0);
            if (home != null) {
                cacheManager.put(CACHE_KEY_PREFIX + home.getId(), home, CACHE_TTL);
                home.setDirty(false);
            }
            return CompletableFuture.completedFuture(Optional.ofNullable(home));
        } catch (Exception e) {
            LOGGER.error("Failed to query home by name from DB", e);
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    @Override
    public CompletableFuture<List<Home>> findAll() {
        try {
            List<Home> results = homeDao.queryForAll();
            return CompletableFuture.completedFuture(results);
        } catch (Exception e) {
            LOGGER.error("Failed to query all homes", e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    @Override
    public CompletableFuture<Home> save(Home home) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                homeDao.create(home);
                if (home.getId() != null) {
                    cacheManager.put(CACHE_KEY_PREFIX + home.getId(), home, CACHE_TTL);
                }
                home.setDirty(false);
                return home;
            } catch (Exception e) {
                throw new RuntimeException("Failed to save home", e);
            }
        });
    }

    @Override
    public CompletableFuture<Home> update(Home home) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                homeDao.update(home);
                if (home.getId() != null) {
                    cacheManager.put(CACHE_KEY_PREFIX + home.getId(), home, CACHE_TTL);
                }
                home.setDirty(false);
                return home;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update home", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(Home home) {
        return CompletableFuture.runAsync(() -> {
            try {
                homeDao.delete(home);
                if (home.getId() != null) {
                    cacheManager.evict(CACHE_KEY_PREFIX + home.getId());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete home", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteById(Long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                homeDao.deleteById(id);
                if (id != null) {
                    cacheManager.evict(CACHE_KEY_PREFIX + id);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete home by id", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return homeDao.idExists(id);
            } catch (Exception e) {
                LOGGER.error("Failed to check home existence", e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Long> count() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return homeDao.countOf();
            } catch (Exception e) {
                LOGGER.error("Failed to count homes", e);
                return 0L;
            }
        });
    }
}
