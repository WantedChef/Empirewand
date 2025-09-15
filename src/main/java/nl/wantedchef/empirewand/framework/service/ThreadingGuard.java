package nl.wantedchef.empirewand.framework.service;

import org.bukkit.Bukkit;

/**
 * Optimized utility class for enforcing thread safety in Paper/Bukkit plugins.
 * This class provides static methods to ensure that critical operations are performed on the correct thread,
 * preventing common concurrency issues.
 *
 * Performance optimizations:
 * - Cached main thread reference to avoid repeated lookups
 * - JIT-friendly hot path optimization
 * - Minimal object allocation
 *
 * @since 2.0.0
 */
public final class ThreadingGuard {

    // Cache the main thread for performance (avoids repeated Thread.currentThread() calls in Bukkit)
    private static volatile Thread mainThread;
    private static volatile boolean initialized = false;

    // Initialize once when first accessed
    static {
        initialize();
    }

    private ThreadingGuard() {
        // Utility class
    }

    /**
     * Initialize the threading guard by caching the main thread reference.
     * This method is thread-safe and will only initialize once.
     */
    private static void initialize() {
        if (!initialized) {
            synchronized (ThreadingGuard.class) {
                if (!initialized) {
                    // Get main thread reference once
                    mainThread = Thread.currentThread();
                    initialized = true;
                }
            }
        }
    }

    /**
     * Ensures that the current thread is the main server thread.
     * Optimized version that uses cached thread reference when possible.
     *
     * @throws IllegalStateException if the current thread is not the main server thread.
     */
    public static void ensureMain() {
        // Fast path: check cached thread first (most common case)
        if (Thread.currentThread() == mainThread) {
            return;
        }

        // Fallback to Bukkit check (handles edge cases like server restart)
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("This operation must be performed on the main server thread.");
        }
    }

    /**
     * Ensures that the current thread is the main server thread.
     * Lightweight version that skips all checks in production builds.
     * Use this for very hot paths where thread safety is guaranteed by caller.
     */
    public static void ensureMainLightweight() {
        // In debug mode, perform full check
        assert Bukkit.isPrimaryThread() : "This operation must be performed on the main server thread.";

        // In production, this is a no-op for maximum performance
    }

    /**
     * Ensures that the current thread is an asynchronous thread (not the main server thread).
     *
     * @throws IllegalStateException if the current thread is the main server thread.
     */
    public static void ensureAsync() {
        if (Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("This operation must be performed on an asynchronous thread.");
        }
    }

    /**
     * Checks if the current thread is the main server thread without throwing an exception.
     *
     * @return true if on main thread, false otherwise
     */
    public static boolean isMainThread() {
        // Fast path using cached reference
        if (Thread.currentThread() == mainThread) {
            return true;
        }

        // Fallback to Bukkit check
        return Bukkit.isPrimaryThread();
    }
}