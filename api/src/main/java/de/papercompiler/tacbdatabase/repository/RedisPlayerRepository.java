package de.papercompiler.tacbdatabase.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.papercompiler.tacbdatabase.cache.CacheManager;
import de.papercompiler.tacbdatabase.entity.Player;
import de.papercompiler.tacbdatabase.pubsub.PubSubManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Slave-node repository for Player: Redis only (no PostgreSQL).
 */
public class RedisPlayerRepository implements PlayerRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPlayerRepository.class);
    private static final String CACHE_KEY_PREFIX = "player:";
    private static final String ID_COUNTER_KEY = "player:id:counter";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final CacheManager cacheManager;
    private final PubSubManager pubSubManager;
    private final AtomicLong localIdCounter = new AtomicLong(System.currentTimeMillis());

    public RedisPlayerRepository(CacheManager cacheManager, PubSubManager pubSubManager) {
        this.cacheManager = cacheManager;
        this.pubSubManager = pubSubManager;
    }

    @Override
    public CompletableFuture<Optional<Player>> findById(Long id) {
        return cacheManager.get(CACHE_KEY_PREFIX + id, Player.class)
                .thenApply(optional -> optional.map(p -> {
                    p.setDirty(false);
                    return p;
                }));
    }

    @Override
    public CompletableFuture<Optional<Player>> findByUuid(UUID uuid) {
        return cacheManager.get(CACHE_KEY_PREFIX + "uuid:" + uuid, Player.class)
                .thenApply(optional -> optional.map(p -> {
                    p.setDirty(false);
                    return p;
                }));
    }

    @Override
    public CompletableFuture<Optional<Player>> findByName(String name) {
        // On slave nodes, we can't query by name efficiently from Redis
        // This would require a secondary index or scanning
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<List<Player>> findOnline() {
        // On slave nodes, we can't efficiently query online players from Redis
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public CompletableFuture<List<Player>> findAll() {
        // On slave nodes, we can't efficiently query all players from Redis
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public CompletableFuture<Player> save(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Generate a temporary ID if not set (will be replaced by master node)
                if (player.getId() == null) {
                    player.setId(localIdCounter.incrementAndGet());
                }
                player.setDirty(true);
                cacheManager.put(CACHE_KEY_PREFIX + player.getId(), player, CACHE_TTL);
                cacheManager.put(CACHE_KEY_PREFIX + "uuid:" + player.getUuid(), player, CACHE_TTL);
                cacheManager.put(CACHE_KEY_PREFIX + "name:" + player.getName(), player, CACHE_TTL);
                return player;
            } catch (Exception e) {
                throw new RuntimeException("Failed to save player to cache", e);
            }
        });
    }

    @Override
    public CompletableFuture<Player> update(Player player) {
        return save(player);
    }

    @Override
    public CompletableFuture<Void> delete(Player player) {
        return CompletableFuture.runAsync(() -> {
            cacheManager.evict(CACHE_KEY_PREFIX + player.getId());
            cacheManager.evict(CACHE_KEY_PREFIX + "uuid:" + player.getUuid());
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
        return cacheManager.get(CACHE_KEY_PREFIX + id, Player.class)
                .thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Long> count() {
        // On slave nodes, we can't count from Redis efficiently
        return CompletableFuture.completedFuture(0L);
    }
}
