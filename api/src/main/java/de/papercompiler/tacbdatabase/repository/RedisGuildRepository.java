package de.papercompiler.tacbdatabase.repository;

import de.papercompiler.tacbdatabase.cache.CacheManager;
import de.papercompiler.tacbdatabase.entity.Guild;
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
 * Slave-node repository for Guild: Redis only (no PostgreSQL).
 */
public class RedisGuildRepository implements GuildRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisGuildRepository.class);
    private static final String CACHE_KEY_PREFIX = "guild:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final CacheManager cacheManager;
    private final PubSubManager pubSubManager;

    public RedisGuildRepository(CacheManager cacheManager, PubSubManager pubSubManager) {
        this.cacheManager = cacheManager;
        this.pubSubManager = pubSubManager;
    }

    @Override
    public CompletableFuture<Optional<Guild>> findById(Long id) {
        return cacheManager.get(CACHE_KEY_PREFIX + id, Guild.class)
                .thenApply(optional -> optional.map(g -> {
                    g.setDirty(false);
                    return g;
                }));
    }

    @Override
    public CompletableFuture<Optional<Guild>> findByOwner(UUID ownerUuid) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Optional<Guild>> findByTag(String tag) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<List<Guild>> findAll() {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public CompletableFuture<Guild> save(Guild guild) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                guild.setDirty(true);
                cacheManager.put(CACHE_KEY_PREFIX + guild.getId(), guild, CACHE_TTL);
                return guild;
            } catch (Exception e) {
                throw new RuntimeException("Failed to save guild to cache", e);
            }
        });
    }

    @Override
    public CompletableFuture<Guild> update(Guild guild) {
        return save(guild);
    }

    @Override
    public CompletableFuture<Void> delete(Guild guild) {
        return CompletableFuture.runAsync(() -> {
            cacheManager.evict(CACHE_KEY_PREFIX + guild.getId());
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
        return cacheManager.get(CACHE_KEY_PREFIX + id, Guild.class)
                .thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Long> count() {
        return CompletableFuture.completedFuture(0L);
    }
}
