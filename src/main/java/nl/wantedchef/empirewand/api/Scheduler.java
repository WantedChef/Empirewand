package nl.wantedchef.empirewand.api;

import org.jetbrains.annotations.NotNull;

/**
 * Simple scheduler facade for thread-safe task execution.
 * Provides main thread and async execution capabilities.
 *
 * @since 2.0.0
 */
public interface Scheduler {

    /**
     * Runs a task on the main thread.
     * If already on main thread, runs immediately.
     * Otherwise, schedules for next tick.
     *
     * @param runnable the task to run
     */
    void runMain(@NotNull Runnable runnable);

    /**
     * Runs a task asynchronously.
     *
     * @param runnable the task to run
     */
    void runAsync(@NotNull Runnable runnable);
}





