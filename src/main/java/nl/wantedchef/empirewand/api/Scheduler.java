package nl.wantedchef.empirewand.api;

/**
 * Scheduler interface for task management.
 */
public interface Scheduler {
    
    /**
     * Schedules a task to run later.
     */
    void runTaskLater(Runnable task, long delayTicks);
    
    /**
     * Schedules a repeating task.
     */
    void runTaskTimer(Runnable task, long delayTicks, long periodTicks);
    
    /**
     * Schedules an async task.
     */
    void runTaskAsynchronously(Runnable task);
}