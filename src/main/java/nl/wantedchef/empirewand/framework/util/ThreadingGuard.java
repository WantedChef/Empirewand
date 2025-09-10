package nl.wantedchef.empirewand.framework.util;

/**
 * A utility class for enforcing thread safety in Paper/Bukkit plugins.
 * This class provides static methods to ensure that critical operations are performed on the correct thread,
 * preventing common concurrency issues.
 *
 * @since 2.0.0
 */
public final class ThreadingGuard {
    private ThreadingGuard() {
        // Utility class
    }

    /**
     * Ensures that the current thread is the main server thread.
     *
     * @throws IllegalStateException if the current thread is not the main server thread.
     */
    public static void ensureMain() {
        if (!org.bukkit.Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("This operation must be performed on the main server thread.");
        }
    }

    /**
     * Ensures that the current thread is an asynchronous thread (not the main server thread).
     *
     * @throws IllegalStateException if the current thread is the main server thread.
     */
    public static void ensureAsync() {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("This operation must be performed on an asynchronous thread.");
        }
    }
}





