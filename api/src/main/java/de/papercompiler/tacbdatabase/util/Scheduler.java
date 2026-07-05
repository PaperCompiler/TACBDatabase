package de.papercompiler.tacbdatabase.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Abstraction over platform-specific task scheduling.
 */
public interface Scheduler {

    /**
     * Runs a task asynchronously.
     *
     * @param task the task to run
     * @return a future that completes when the task is done
     */
    CompletableFuture<Void> async(Runnable task);

    /**
     * Runs a task repeatedly at a fixed rate.
     *
     * @param task     the task to run
     * @param interval the interval between executions
     * @param unit      the time unit of the interval
     * @return a handle that can be used to cancel the task
     */
    TaskHandle asyncRepeating(Runnable task, long interval, TimeUnit unit);

    /**
     * Runs a task once after a delay.
     *
     * @param task  the task to run
     * @param delay the delay before execution
     * @param unit  the time unit of the delay
     * @return a future that completes when the task is done
     */
    CompletableFuture<Void> asyncLater(Runnable task, long delay, TimeUnit unit);

    /**
     * Returns the async executor for this scheduler.
     *
     * @return the executor
     */
    Executor getExecutor();

    /**
     * Handle for a repeating task that can be cancelled.
     */
    interface TaskHandle {
        /**
         * Cancels the repeating task.
         */
        void cancel();
    }
}
