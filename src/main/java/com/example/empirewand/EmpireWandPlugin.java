package com.example.empirewand;

import org.bukkit.plugin.java.JavaPlugin;

public final class EmpireWandPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("EmpireWand enabled on " + getServer().getVersion());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic (optional)
    }
}

