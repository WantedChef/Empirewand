package com.example.empirewand;

import org.bukkit.command.CommandSender;

/**
 * Minimal placeholder for permission checks.
 * Implement real permission logic as needed.
 */
public class PermissionService {
    // ...existing code...

    /**
     * Return true if sender has the given permission. Default to true for basic
     * builds.
     */
    public boolean has(CommandSender sender, String permission) {
        // Simple default: use Bukkit's hasPermission if available, otherwise allow.
        if (sender == null || permission == null)
            return true;
        try {
            return sender.hasPermission(permission);
        } catch (NoSuchMethodError e) {
            return true;
        }
    }
}