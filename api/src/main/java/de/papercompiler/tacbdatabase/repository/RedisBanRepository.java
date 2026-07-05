package de.papercompiler.tacbdatabase.repository;

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
 * Slave-node repository for Ban: Redis only (no PostgreSQL).
 */
public class RedisBanRepository implements BanRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisBanRepository.class);
    private static final String CACHE_KEY_PREFIX = "ban:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final CacheManager cacheManager;
    private final PubSubManager pubSubManager;

    public RedisBanRepository(CacheManager cacheManager, PubSubManager pubSubManager) {
        this.cacheManager = cacheManager;
        this.pubSubManager = pubSubManager;
    }

    @Override
    public CompletableFuture<Optional<Ban>> findById(Long id) {
        return cacheManager.get(CACHE_KEY_PREFIX + id, Ban.class)
                .thenApply(optional -> optional.map(b -> {
                    b.setDirty(false);
                    return b;
                }));
    }

    @Override
    public CompletableFuture<Optional<Ban>> findActiveByUuid(UUID uuid) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Optional<Ban>> findActiveByIp(String ip) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<List<Ban>> findAll() {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public CompletableFuture<Ban> save(Ban ban) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ban.setDirty(true);
                cacheManager.put(CACHE_KEY_PREFIX + ban.getId(), ban, CACHE_TTL);
                return ban;
            } catch (Exception e) {
                throw new RuntimeException("Failed to save ban to cache", e);
            }
        });
    }

    @Override
    public CompletableFuture<Ban> update(Ban ban) {
        return save(ban);
    }

    @Override
    public CompletableFuture<Void> delete(Ban ban) {
        return CompletableFuture.runAsync(() -> {
            cacheManager.evict(CACHE_KEY_PREFIX + ban.getId());
        });
    }

    @Override
    public CompletableFuture<Void> deleteById(Long id) {
        return CompletableFuture.runAsync(() -> {
            cacheManager.evict(CACHE_KEY_PREFIX + id);
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(Long id) {
        return cacheManager.get(CACHE_KEY_PREFIX + id, Ban.class)
                .thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Long> count() {
        return CompletableFuture.completedFuture(0L);
    }
}
