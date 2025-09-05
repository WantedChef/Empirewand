package com.example.empirewand;

import com.example.empirewand.core.CooldownService;
import com.example.empirewand.core.ConfigService;
import com.example.empirewand.core.FxService;
import com.example.empirewand.core.PermissionService;
import com.example.empirewand.core.SpellRegistry;
import com.example.empirewand.core.WandData;
import com.example.empirewand.listeners.EntityListener;
import com.example.empirewand.listeners.ProjectileListener;
import com.example.empirewand.listeners.WandInteractionListener;
import com.example.empirewand.command.EmpireWandCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EmpireWandPlugin extends JavaPlugin {

    private SpellRegistry spellRegistry;
    private CooldownService cooldownService;
    private WandData wandData;
    private FxService fxService;
    private ConfigService configService;
    private PermissionService permissionService;

    @Override
    public void onEnable() {
        // Init services (skeletons)
        this.configService = new ConfigService(this);
        this.spellRegistry = new SpellRegistry();
        this.cooldownService = new CooldownService();
        this.wandData = new WandData(this);
        this.fxService = new FxService(this.configService);
        this.permissionService = new PermissionService();

        // Register listeners (skeletons)
        var pm = getServer().getPluginManager();
        pm.registerEvents(new WandInteractionListener(this), this);
        pm.registerEvents(new EntityListener(this), this);
        pm.registerEvents(new ProjectileListener(this), this);

        // Register commands
        getCommand("ew").setExecutor(new com.example.empirewand.command.EmpireWandCommand(this));

        getLogger().info("EmpireWand enabled on " + getServer().getVersion());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic (optional)
    }

    // Expose services to other components
    public SpellRegistry getSpellRegistry() { return spellRegistry; }
    public CooldownService getCooldownService() { return cooldownService; }
    public WandData getWandData() { return wandData; }
    public FxService getFxService() { return fxService; }
    public ConfigService getConfigService() { return configService; }
    public PermissionService getPermissionService() { return permissionService; }
}
