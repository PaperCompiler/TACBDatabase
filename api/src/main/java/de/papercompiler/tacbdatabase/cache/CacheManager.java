package de.papercompiler.tacbdatabase.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Abstraction over a Redis-backed cache.
 */
public interface CacheManager {

    /**
     * Gets a value from the cache.
     *
     * @param key  the cache key
     * @param type the expected type
     * @param <T>  the type
     * @return the value, or empty if not found
     */
    <T> CompletableFuture<Optional<T>> get(String key, Class<T> type);

    /**
     * Puts a value into the cache with a TTL.
     *
     * @param key  the cache key
     * @param value the value to cache
     * @param ttl  the time-to-live
     * @param <T>  the type
     * @return a future that completes when done
     */
    <T> CompletableFuture<Void> put(String key, T value, Duration ttl);

    /**
     * Puts a value into the cache without expiration.
     *
     * @param key  the cache key
     * @param value the value to cache
     * @param <T>  the type
     * @return a future that completes when done
     */
    <T> CompletableFuture<Void> put(String key, T value);

    /**
     * Evicts a single key from the cache.
     *
     * @param key the cache key
     * @return a future that completes when done
     */
    CompletableFuture<Void> evict(String key);

    /**
     * Evicts all keys matching a pattern.
     *
     * @param pattern the key pattern (e.g., "player:*")
     * @return a future that completes when done
     */
    CompletableFuture<Void> evictPattern(String pattern);

    /**
     * Gets all keys matching a pattern.
     *
     * @param pattern the key pattern (e.g., "player:*")
     * @return a set of matching keys
     */
    CompletableFuture<Set<String>> getKeysByPattern(String pattern);

    /**
     * Flushes the entire cache.
     *
     * @return a future that completes when done
     */
    CompletableFuture<Void> flushAll();

    /**
     * Closes the cache manager and releases resources.
     */
    void close();
}
