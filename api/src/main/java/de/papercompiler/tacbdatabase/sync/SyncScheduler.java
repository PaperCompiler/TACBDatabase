package de.papercompiler.tacbdatabase.sync;

import de.papercompiler.tacbdatabase.cache.CacheManager;
import de.papercompiler.tacbdatabase.entity.Entity;
import de.papercompiler.tacbdatabase.repository.Repository;
import de.papercompiler.tacbdatabase.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler that periodically flushes dirty Redis data to PostgreSQL.
 * <p>
 * Only runs on the master node (Velocity proxy).
 */
public class SyncScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncScheduler.class);

    private final Map<Class<?>, Repository<?, ?>> repositories;
    private final CacheManager cacheManager;
    private final Duration interval;
    private final Scheduler scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Scheduler.TaskHandle taskHandle;

    public SyncScheduler(Map<Class<?>, Repository<?, ?>> repositories, CacheManager cacheManager, Duration interval, Scheduler scheduler) {
        this.repositories = repositories;
        this.cacheManager = cacheManager;
        this.interval = interval;
        this.scheduler = scheduler;
    }

    /**
     * Starts the periodic sync scheduler.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            taskHandle = scheduler.asyncRepeating(this::flush, interval.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            LOGGER.info("SyncScheduler started with interval: {}", interval);
        }
    }

    /**
     * Stops the sync scheduler.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (taskHandle != null) {
                taskHandle.cancel();
            }
            LOGGER.info("SyncScheduler stopped");
        }
    }

    /**
     * Manually triggers a flush.
     *
     * @return a future that completes when the flush is done
     */
    public CompletableFuture<Void> flushNow() {
        return flush();
    }

    private CompletableFuture<Void> flush() {
        if (!running.get()) {
            return CompletableFuture.completedFuture(null);
        }

        LOGGER.debug("Starting sync flush to PostgreSQL...");

        CompletableFuture<Void> all = CompletableFuture.allOf(
                repositories.values().stream()
                        .map(repo -> flushRepository((Repository<?, ?>) repo))
                        .toArray(CompletableFuture[]::new)
        );

        return all.thenRun(() -> LOGGER.debug("Sync flush completed"));
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> CompletableFuture<Void> flushRepository(Repository<T, ?> repository) {
        // For cached repositories, we need to flush dirty entities to PostgreSQL
        // The cached repositories (CachedPlayerRepository, etc.) already handle
        // the database sync in their update() method. We just need to ensure
        // all dirty entities are written.
        //
        // In a production implementation, we would:
        // 1. Use Redis SCAN to find all keys matching "tacb:dirty:*"
        // 2. For each dirty key, get the entity from cache
        // 3. Update the entity in PostgreSQL
        // 4. Clear the dirty flag
        //
        // For now, we just log that the repository is being flushed.
        // The actual dirty tracking would be implemented via the DirtyTracker.
        return CompletableFuture.runAsync(() -> {
            try {
                LOGGER.debug("Flushing repository: {}", repository.getClass().getSimpleName());
                // Note: The cached repositories handle their own sync in update()
                // This method is a placeholder for future dirty tracking implementation
            } catch (Exception e) {
                LOGGER.error("Failed to flush repository", e);
            }
        });
    }
}
