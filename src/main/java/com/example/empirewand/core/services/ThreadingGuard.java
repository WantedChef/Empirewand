package com.example.empirewand.core.services;

/**
 * Utility class for enforcing thread safety in Paper-based implementations.
 * Provides static methods to ensure operations are performed on the correct
 * thread.
 *
 * @since 2.0.0
 */
public final class ThreadingGuard {
    private ThreadingGuard() {
        // Utility class
    }

    /**
     * Ensures the current thread is the main (server) thread.
     * Throws IllegalStateException if called from async thread.
     *
     * @throws IllegalStateException if not on main thread
     */
    public static void ensureMain() {
        if (!org.bukkit.Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Must run on main thread (Paper).");
        }
    }

    /**
     * Ensures the current thread is NOT the main (server) thread.
     * Throws IllegalStateException if called from main thread.
     *
     * @throws IllegalStateException if on main thread
     */
    public static void ensureAsync() {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Must not run on main thread (Paper).");
        }
    }
}