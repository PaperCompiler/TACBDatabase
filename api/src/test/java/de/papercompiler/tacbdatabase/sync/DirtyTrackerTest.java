package de.papercompiler.tacbdatabase.sync;

import de.papercompiler.tacbdatabase.cache.CacheManager;
import de.papercompiler.tacbdatabase.entity.Player;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class DirtyTrackerTest {

    // Simple mock CacheManager for testing
    private static class MockCacheManager implements CacheManager {
        private final Set<String> keys = new HashSet<>();

        @Override
        public <T> CompletableFuture<Optional<T>> get(String key, Class<T> type) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        @Override
        public <T> CompletableFuture<Void> put(String key, T value, Duration ttl) {
            keys.add(key);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public <T> CompletableFuture<Void> put(String key, T value) {
            keys.add(key);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> evict(String key) {
            keys.remove(key);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> evictPattern(String pattern) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Set<String>> getKeysByPattern(String pattern) {
            Set<String> matching = new HashSet<>();
            for (String key : keys) {
                if (key.matches(pattern.replace("*", ".*"))) {
                    matching.add(key);
                }
            }
            return CompletableFuture.completedFuture(matching);
        }

        @Override
        public CompletableFuture<Void> flushAll() {
            keys.clear();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void close() {
        }
    }

    @Test
    void markDirty_AddsKeyToCache() {
        MockCacheManager cacheManager = new MockCacheManager();
        DirtyTracker tracker = new DirtyTracker(cacheManager);

        tracker.markDirty(Player.class, 42L).join();

        assertTrue(cacheManager.keys.contains("tacb:dirty:player:42"));
    }

    @Test
    void clearDirty_RemovesKeyFromCache() {
        MockCacheManager cacheManager = new MockCacheManager();
        DirtyTracker tracker = new DirtyTracker(cacheManager);

        tracker.markDirty(Player.class, 42L).join();
        assertTrue(cacheManager.keys.contains("tacb:dirty:player:42"));

        tracker.clearDirty(Player.class, 42L).join();
        assertFalse(cacheManager.keys.contains("tacb:dirty:player:42"));
    }

    @Test
    void getDirtyIds_ReturnsMarkedIds() {
        MockCacheManager cacheManager = new MockCacheManager();
        DirtyTracker tracker = new DirtyTracker(cacheManager);

        tracker.markDirty(Player.class, 1L).join();
        tracker.markDirty(Player.class, 2L).join();
        tracker.markDirty(Player.class, 3L).join();

        Set<Long> dirtyIds = tracker.getDirtyIds(Player.class).join();

        assertEquals(new HashSet<>(Set.of(1L, 2L, 3L)), dirtyIds);
    }

    @Test
    void getDirtyIds_ReturnsEmptySet_WhenNoDirtyEntities() {
        MockCacheManager cacheManager = new MockCacheManager();
        DirtyTracker tracker = new DirtyTracker(cacheManager);

        Set<Long> dirtyIds = tracker.getDirtyIds(Player.class).join();

        assertTrue(dirtyIds.isEmpty());
    }

    @Test
    void getDirtyIds_OnlyReturnsIdsForRequestedEntityType() {
        MockCacheManager cacheManager = new MockCacheManager();
        DirtyTracker tracker = new DirtyTracker(cacheManager);

        tracker.markDirty(Player.class, 1L).join();
        tracker.markDirty(de.papercompiler.tacbdatabase.entity.Guild.class, 10L).join();

        Set<Long> playerDirtyIds = tracker.getDirtyIds(Player.class).join();
        Set<Long> guildDirtyIds = tracker.getDirtyIds(de.papercompiler.tacbdatabase.entity.Guild.class).join();

        assertEquals(Set.of(1L), playerDirtyIds);
        assertEquals(Set.of(10L), guildDirtyIds);
    }

    @Test
    void getDirtyIds_IgnoresMalformedKeys() {
        MockCacheManager cacheManager = new MockCacheManager();
        // Manually add a malformed key
        cacheManager.keys.add("tacb:dirty:player:notanumber");

        DirtyTracker tracker = new DirtyTracker(cacheManager);

        Set<Long> dirtyIds = tracker.getDirtyIds(Player.class).join();

        assertTrue(dirtyIds.isEmpty());
    }
}
