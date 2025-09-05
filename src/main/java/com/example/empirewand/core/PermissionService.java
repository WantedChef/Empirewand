package com.example.empirewand.core;

/**
 * Skeleton permission helper for readable checks.
 */
public class PermissionService {
    public boolean has(org.bukkit.command.CommandSender sender, String node) {
        return sender.hasPermission(node);
    }
}

