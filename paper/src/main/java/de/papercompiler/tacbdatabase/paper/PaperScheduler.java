package de.papercompiler.tacbdatabase.paper;

import de.papercompiler.tacbdatabase.util.Scheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Paper scheduler implementation.
 */
public class PaperScheduler implements Scheduler {

    private final JavaPlugin plugin;
    private final Executor asyncExecutor;

    public PaperScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "tacb-async-" + r.hashCode());
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public CompletableFuture<Void> async(Runnable task) {
        return CompletableFuture.runAsync(task, asyncExecutor);
    }

    @Override
    public TaskHandle asyncRepeating(Runnable task, long interval, TimeUnit unit) {
        long ticks = Math.max(1, unit.toMillis(interval) / 50);
        BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, task, 0, ticks);
        return new PaperTaskHandle(bukkitTask);
    }

    @Override
    public CompletableFuture<Void> asyncLater(Runnable task, long delay, TimeUnit unit) {
        long ticks = Math.max(1, unit.toMillis(delay) / 50);
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, task, ticks);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Executor getExecutor() {
        return asyncExecutor;
    }

    private static class PaperTaskHandle implements TaskHandle {
        private final BukkitTask task;

        PaperTaskHandle(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}
