package de.papercompiler.tacbdatabase.repository;

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
 * Slave-node repository for Home: Redis only (no PostgreSQL).
 */
public class RedisHomeRepository implements HomeRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisHomeRepository.class);
    private static final String CACHE_KEY_PREFIX = "home:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final CacheManager cacheManager;
    private final PubSubManager pubSubManager;

    public RedisHomeRepository(CacheManager cacheManager, PubSubManager pubSubManager) {
        this.cacheManager = cacheManager;
        this.pubSubManager = pubSubManager;
    }

    @Override
    public CompletableFuture<Optional<Home>> findById(Long id) {
        return cacheManager.get(CACHE_KEY_PREFIX + id, Home.class)
                .thenApply(optional -> optional.map(h -> {
                    h.setDirty(false);
                    return h;
                }));
    }

    @Override
    public CompletableFuture<List<Home>> findByPlayer(UUID uuid) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public CompletableFuture<Optional<Home>> findByName(UUID uuid, String name) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<List<Home>> findAll() {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public CompletableFuture<Home> save(Home home) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                home.setDirty(true);
                if (home.getId() != null) {
                    cacheManager.put(CACHE_KEY_PREFIX + home.getId(), home, CACHE_TTL);
                }
                return home;
            } catch (Exception e) {
                throw new RuntimeException("Failed to save home to cache", e);
            }
        });
    }

    @Override
    public CompletableFuture<Home> update(Home home) {
        return save(home);
    }

    @Override
    public CompletableFuture<Void> delete(Home home) {
        return CompletableFuture.runAsync(() -> {
            if (home.getId() != null) {
                cacheManager.evict(CACHE_KEY_PREFIX + home.getId());
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteById(Long id) {
        return CompletableFuture.runAsync(() -> {
            if (id != null) {
                cacheManager.evict(CACHE_KEY_PREFIX + id);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(Long id) {
        return cacheManager.get(CACHE_KEY_PREFIX + id, Home.class)
                .thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Long> count() {
        return CompletableFuture.completedFuture(0L);
    }
}
