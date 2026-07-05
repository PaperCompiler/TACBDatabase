package de.papercompiler.tacbdatabase.minestom;

import de.papercompiler.tacbdatabase.util.Scheduler;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.Task;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Minestom scheduler implementation.
 */
public class MinestomScheduler implements Scheduler {

    private final MinecraftServer minecraftServer;
    private final Executor asyncExecutor;

    public MinestomScheduler(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
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
        long millis = unit.toMillis(interval);
        Task scheduledTask = minecraftServer.getSchedulerManager().buildTask(task)
                .repeat(millis, ChronoUnit.MILLIS)
                .schedule();
        return new MinestomTaskHandle(scheduledTask);
    }

    @Override
    public CompletableFuture<Void> asyncLater(Runnable task, long delay, TimeUnit unit) {
        return CompletableFuture.runAsync(task, asyncExecutor);
    }

    @Override
    public Executor getExecutor() {
        return asyncExecutor;
    }

    private static class MinestomTaskHandle implements TaskHandle {
        private final Task task;

        MinestomTaskHandle(Task task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}
