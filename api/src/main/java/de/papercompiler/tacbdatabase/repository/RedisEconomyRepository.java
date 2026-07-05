package de.papercompiler.tacbdatabase.repository;

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
 * Slave-node repository for Economy: Redis only (no PostgreSQL).
 */
public class RedisEconomyRepository implements EconomyRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisEconomyRepository.class);
    private static final String CACHE_KEY_PREFIX = "economy:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final CacheManager cacheManager;
    private final PubSubManager pubSubManager;

    public RedisEconomyRepository(CacheManager cacheManager, PubSubManager pubSubManager) {
        this.cacheManager = cacheManager;
        this.pubSubManager = pubSubManager;
    }

    @Override
    public CompletableFuture<Optional<Economy>> findById(Long id) {
        return cacheManager.get(CACHE_KEY_PREFIX + id, Economy.class)
                .thenApply(optional -> optional.map(e -> {
                    e.setDirty(false);
                    return e;
                }));
    }

    @Override
    public CompletableFuture<Optional<Economy>> findByUuid(UUID uuid) {
        return cacheManager.get(CACHE_KEY_PREFIX + "uuid:" + uuid, Economy.class)
                .thenApply(optional -> optional.map(e -> {
                    e.setDirty(false);
                    return e;
                }));
    }

    @Override
    public CompletableFuture<Economy> adjustBalance(UUID uuid, BigDecimal amount) {
        return findByUuid(uuid).thenCompose(optional -> {
            Economy economy = optional.orElseGet(() -> new Economy(uuid, BigDecimal.ZERO, "default"));
            economy.setBalance(economy.getBalance().add(amount));
            return save(economy);
        });
    }

    @Override
    public CompletableFuture<List<Economy>> findAll() {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public CompletableFuture<Economy> save(Economy economy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                economy.setDirty(true);
                cacheManager.put(CACHE_KEY_PREFIX + economy.getId(), economy, CACHE_TTL);
                cacheManager.put(CACHE_KEY_PREFIX + "uuid:" + economy.getUuid(), economy, CACHE_TTL);
                return economy;
            } catch (Exception e) {
                throw new RuntimeException("Failed to save economy to cache", e);
            }
        });
    }

    @Override
    public CompletableFuture<Economy> update(Economy economy) {
        return save(economy);
    }

    @Override
    public CompletableFuture<Void> delete(Economy economy) {
        return CompletableFuture.runAsync(() -> {
            cacheManager.evict(CACHE_KEY_PREFIX + economy.getId());
            cacheManager.evict(CACHE_KEY_PREFIX + "uuid:" + economy.getUuid());
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
        return cacheManager.get(CACHE_KEY_PREFIX + id, Economy.class)
                .thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Long> count() {
        return CompletableFuture.completedFuture(0L);
    }
}
