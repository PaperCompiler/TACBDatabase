package de.papercompiler.tacbdatabase.repository;

import com.j256.ormlite.dao.Dao;
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
 * Master-node repository for Guild: PostgreSQL + Redis cache.
 */
public class CachedGuildRepository implements GuildRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedGuildRepository.class);
    private static final String CACHE_KEY_PREFIX = "guild:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final Dao<Guild, Long> guildDao;
    private final CacheManager cacheManager;
    private final PubSubManager pubSubManager;

    public CachedGuildRepository(Dao<Guild, Long> guildDao, CacheManager cacheManager, PubSubManager pubSubManager) {
        this.guildDao = guildDao;
        this.cacheManager = cacheManager;
        this.pubSubManager = pubSubManager;
    }

    @Override
    public CompletableFuture<Optional<Guild>> findById(Long id) {
        return cacheManager.get(CACHE_KEY_PREFIX + id, Guild.class)
                .thenApply(optional -> optional.map(g -> {
                    g.setDirty(false);
                    return g;
                }))
                .exceptionally(e -> {
                    try {
                        Guild guild = guildDao.queryForId(id);
                        if (guild != null) {
                            cacheManager.put(CACHE_KEY_PREFIX + id, guild, CACHE_TTL);
                            guild.setDirty(false);
                        }
                        return Optional.ofNullable(guild);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to query guild from DB", ex);
                        return Optional.empty();
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<Guild>> findByOwner(UUID ownerUuid) {
        try {
            List<Guild> results = guildDao.queryForEq("ownerUuid", ownerUuid);
            Guild guild = results.isEmpty() ? null : results.get(0);
            if (guild != null) {
                cacheManager.put(CACHE_KEY_PREFIX + guild.getId(), guild, CACHE_TTL);
                guild.setDirty(false);
            }
            return CompletableFuture.completedFuture(Optional.ofNullable(guild));
        } catch (Exception e) {
            LOGGER.error("Failed to query guild by owner from DB", e);
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    @Override
    public CompletableFuture<Optional<Guild>> findByTag(String tag) {
        try {
            List<Guild> results = guildDao.queryForEq("tag", tag);
            Guild guild = results.isEmpty() ? null : results.get(0);
            if (guild != null) {
                cacheManager.put(CACHE_KEY_PREFIX + guild.getId(), guild, CACHE_TTL);
                guild.setDirty(false);
            }
            return CompletableFuture.completedFuture(Optional.ofNullable(guild));
        } catch (Exception e) {
            LOGGER.error("Failed to query guild by tag from DB", e);
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    @Override
    public CompletableFuture<List<Guild>> findAll() {
        try {
            List<Guild> results = guildDao.queryForAll();
            return CompletableFuture.completedFuture(results);
        } catch (Exception e) {
            LOGGER.error("Failed to query all guilds", e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }

    @Override
    public CompletableFuture<Guild> save(Guild guild) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                guildDao.create(guild);
                if (guild.getId() != null) {
                    cacheManager.put(CACHE_KEY_PREFIX + guild.getId(), guild, CACHE_TTL);
                }
                guild.setDirty(false);
                return guild;
            } catch (Exception e) {
                throw new RuntimeException("Failed to save guild", e);
            }
        });
    }

    @Override
    public CompletableFuture<Guild> update(Guild guild) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                guildDao.update(guild);
                if (guild.getId() != null) {
                    cacheManager.put(CACHE_KEY_PREFIX + guild.getId(), guild, CACHE_TTL);
                }
                guild.setDirty(false);
                return guild;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update guild", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(Guild guild) {
        return CompletableFuture.runAsync(() -> {
            try {
                guildDao.delete(guild);
                if (guild.getId() != null) {
                    cacheManager.evict(CACHE_KEY_PREFIX + guild.getId());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete guild", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteById(Long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                guildDao.deleteById(id);
                if (id != null) {
                    cacheManager.evict(CACHE_KEY_PREFIX + id);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete guild by id", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return guildDao.idExists(id);
            } catch (Exception e) {
                LOGGER.error("Failed to check guild existence", e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Long> count() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return guildDao.countOf();
            } catch (Exception e) {
                LOGGER.error("Failed to count guilds", e);
                return 0L;
            }
        });
    }
}
