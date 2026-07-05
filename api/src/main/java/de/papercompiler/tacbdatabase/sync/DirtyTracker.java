package de.papercompiler.tacbdatabase.sync;

import de.papercompiler.tacbdatabase.cache.CacheManager;
import de.papercompiler.tacbdatabase.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Tracks dirty entities in Redis for sync to PostgreSQL.
 */
public class DirtyTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirtyTracker.class);
    private static final String DIRTY_KEY_PREFIX = "tacb:dirty:";
    private static final Duration DIRTY_TTL = Duration.ofHours(2);

    private final CacheManager cacheManager;

    public DirtyTracker(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Marks an entity as dirty for sync.
     *
     * @param entityType the entity class
     * @param entityId   the entity ID
     */
    public CompletableFuture<Void> markDirty(Class<? extends Entity> entityType, Long entityId) {
        String key = DIRTY_KEY_PREFIX + entityType.getSimpleName().toLowerCase() + ":" + entityId;
        return cacheManager.put(key, System.currentTimeMillis(), DIRTY_TTL);
    }

    /**
     * Gets all dirty entity IDs for a given entity type.
     *
     * @param entityType the entity class
     * @return a set of dirty IDs
     */
    public CompletableFuture<Set<Long>> getDirtyIds(Class<? extends Entity> entityType) {
        String pattern = DIRTY_KEY_PREFIX + entityType.getSimpleName().toLowerCase() + ":*";
        return cacheManager.getKeysByPattern(pattern)
            .thenApply(keys -> {
                Set<Long> ids = new java.util.HashSet<>();
                String prefix = DIRTY_KEY_PREFIX + entityType.getSimpleName().toLowerCase() + ":";
                for (String key : keys) {
                    // Parse the ID from the key: tacb:dirty:entitytype:id
                    if (key.startsWith(prefix)) {
                        String idStr = key.substring(prefix.length());
                        try {
                            ids.add(Long.parseLong(idStr));
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Failed to parse dirty key: {}", key);
                        }
                    }
                }
                return ids;
            });
    }

    /**
     * Clears the dirty flag for an entity.
     *
     * @param entityType the entity class
     * @param entityId   the entity ID
     */
    public CompletableFuture<Void> clearDirty(Class<? extends Entity> entityType, Long entityId) {
        String key = DIRTY_KEY_PREFIX + entityType.getSimpleName().toLowerCase() + ":" + entityId;
        return cacheManager.evict(key);
    }
}
