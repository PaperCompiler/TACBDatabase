package de.papercompiler.tacbdatabase.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.papercompiler.tacbdatabase.util.Scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Velocity scheduler implementation.
 */
public class VelocityScheduler implements Scheduler {

    private final ProxyServer proxyServer;
    private final Executor asyncExecutor;

    public VelocityScheduler(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "tacb-async-" + r.hashCode());
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public CompletableFuture<Void> async(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ScheduledTask scheduledTask = proxyServer.getScheduler().buildTask(this, () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }).schedule();
        return future;
    }

    @Override
    public TaskHandle asyncRepeating(Runnable task, long interval, TimeUnit unit) {
        ScheduledTask scheduledTask = proxyServer.getScheduler().buildTask(this, task)
                .repeat(interval, unit)
                .schedule();
        return new VelocityTaskHandle(scheduledTask);
    }

    @Override
    public CompletableFuture<Void> asyncLater(Runnable task, long delay, TimeUnit unit) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        proxyServer.getScheduler().buildTask(this, () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }).delay(delay, unit)
          .schedule();
        return future;
    }

    @Override
    public Executor getExecutor() {
        return asyncExecutor;
    }

    private static class VelocityTaskHandle implements TaskHandle {
        private final ScheduledTask task;

        VelocityTaskHandle(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}
