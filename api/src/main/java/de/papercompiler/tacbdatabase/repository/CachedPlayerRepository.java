package de.papercompiler.tacbdatabase.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
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

/**
 * Master-node repository for Player: PostgreSQL + Redis cache.
 */
public class CachedPlayerRepository implements PlayerRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedPlayerRepository.class);
    private static final String CACHE_KEY_PREFIX = "player:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final Dao<Player, Long> playerDao;
    private final CacheManager cacheManager;
    private final PubSubManager pubSubManager;

    public CachedPlayerRepository(Dao<Player, Long> playerDao, CacheManager cacheManager, PubSubManager pubSubManager) {
        this.playerDao = playerDao;
        this.cacheManager = cacheManager;
        this.pubSubManager = pubSubManager;
    }

    @Override
    public CompletableFuture<Optional<Player>> findById(Long id) {
        return cacheManager.get(CACHE_KEY_PREFIX + id, Player.class)
                .thenApply(optional -> optional.map(p -> {
                    p.setDirty(false);
                    return p;
                }))
                .exceptionally(e -> {
                    LOGGER.error("Cache miss, falling back to DB for player id={}", id, e);
                    try {
                        Player player = playerDao.queryForId(id);
                        if (player != null) {
                            cacheManager.put(CACHE_KEY_PREFIX + id, player, CACHE_TTL);
                            player.setDirty(false);
                        }
                        return Optional.ofNullable(player);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to query player from DB", ex);
                        return Optional.empty();
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<Player>> findByUuid(UUID uuid) {
        return cacheManager.get(CACHE_KEY_PREFIX + "uuid:" + uuid, Player.class)
                .thenApply(optional -> optional.map(p -> {
                    p.setDirty(false);
                    return p;
                }))
                .exceptionally(e -> {
                    try {
                        List<Player> results = playerDao.queryForEq("uuid", uuid);
                        Player player = results.isEmpty() ? null : results.get(0);
                        if (player != null) {
                            cacheManager.put(CACHE_KEY_PREFIX + "uuid:" + uuid, player, CACHE_TTL);
                            cacheManager.put(CACHE_KEY_PREFIX + player.getId(), player, CACHE_TTL);
                            player.setDirty(false);
                        }
                        return Optional.ofNullable(player);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to query player by UUID from DB", ex);
                        return Optional.empty();
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<Player>> findByName(String name) {
        return cacheManager.get(CACHE_KEY_PREFIX + "name:" + name, Player.class)
                .thenApply(optional -> optional.map(p -> {
                    p.setDirty(false);
                    return p;
                }))
                .exceptionally(e -> {
                    try {
                        List<Player> results = playerDao.queryForEq("name", name);
                        Player player = results.isEmpty() ? null : results.get(0);
                        if (player != null) {
                            cacheManager.put(CACHE_KEY_PREFIX + player.getId(), player, CACHE_TTL);
                            cacheManager.put(CACHE_KEY_PREFIX + "uuid:" + player.getUuid(), player, CACHE_TTL);
                            cacheManager.put(CACHE_KEY_PREFIX + "name:" + name, player, CACHE_TTL);
                            player.setDirty(false);
                        }
                        return Optional.ofNullable(player);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to query player by name from DB", ex);
                        return Optional.empty();
                    }
                });
    }

    @Override
    public CompletableFuture<List<Player>> findOnline() {
        try {
            List<Player> results = playerDao.queryBuilder()
                    .where()
                    .isNotNull("lastServer")
                    .query();
            return CompletableFuture.completedFuture(results);
        } catch (Exception e) {
            LOGGER.error("Failed to query online players", e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    @Override
    public CompletableFuture<List<Player>> findAll() {
        try {
            List<Player> results = playerDao.queryForAll();
            return CompletableFuture.completedFuture(results);
        } catch (Exception e) {
            LOGGER.error("Failed to query all players", e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    @Override
    public CompletableFuture<Player> save(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                playerDao.create(player);
                cacheManager.put(CACHE_KEY_PREFIX + player.getId(), player, CACHE_TTL);
                cacheManager.put(CACHE_KEY_PREFIX + "uuid:" + player.getUuid(), player, CACHE_TTL);
                cacheManager.put(CACHE_KEY_PREFIX + "name:" + player.getName(), player, CACHE_TTL);
                player.setDirty(false);
                return player;
            } catch (Exception e) {
                throw new RuntimeException("Failed to save player", e);
            }
        });
    }

    @Override
    public CompletableFuture<Player> update(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                playerDao.update(player);
                cacheManager.put(CACHE_KEY_PREFIX + player.getId(), player, CACHE_TTL);
                cacheManager.put(CACHE_KEY_PREFIX + "uuid:" + player.getUuid(), player, CACHE_TTL);
                player.setDirty(false);
                return player;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update player", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(Player player) {
        return CompletableFuture.runAsync(() -> {
            try {
                playerDao.delete(player);
                cacheManager.evict(CACHE_KEY_PREFIX + player.getId());
                cacheManager.evict(CACHE_KEY_PREFIX + "uuid:" + player.getUuid());
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete player", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteById(Long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                playerDao.deleteById(id);
                cacheManager.evict(CACHE_KEY_PREFIX + id);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete player by id", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return playerDao.idExists(id);
            } catch (Exception e) {
                LOGGER.error("Failed to check player existence", e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Long> count() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return playerDao.countOf();
            } catch (Exception e) {
                LOGGER.error("Failed to count players", e);
                return 0L;
            }
        });
    }
}
