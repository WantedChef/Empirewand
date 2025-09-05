package com.example.empirewand;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.core.CooldownService;
import com.example.empirewand.core.ConfigService;
import com.example.empirewand.core.FxService;
import com.example.empirewand.core.Keys;
import com.example.empirewand.core.PermissionService;
import com.example.empirewand.core.SpellRegistry;
import com.example.empirewand.core.WandData;
import com.example.empirewand.core.text.TextService;
import com.example.empirewand.core.MetricsService;
import com.example.empirewand.listeners.EntityListener;
import com.example.empirewand.listeners.ProjectileListener;
import com.example.empirewand.listeners.WandInteractionListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class EmpireWandPlugin extends JavaPlugin implements EmpireWandAPI.EmpireWandProvider {

    private SpellRegistry spellRegistry;
    private CooldownService cooldownService;
    private WandData wandData;
    private FxService fxService;
    private ConfigService configService;
    private PermissionService permissionService;
    private TextService textService;
    private MetricsService metricsService;

    @Override
    public void onEnable() {
        // Initialize Keys with plugin instance
        Keys.initialize(this);

        // Init services (skeletons)
        this.configService = new ConfigService(this);
        this.textService = new TextService();
        this.spellRegistry = new SpellRegistry();
        this.cooldownService = new CooldownService();
        this.wandData = new WandData(this);
        this.fxService = new FxService(this.configService, this.textService);
        this.permissionService = new PermissionService();
        this.metricsService = new MetricsService(this, this.configService);

        // Register listeners (skeletons)
        var pm = getServer().getPluginManager();
        pm.registerEvents(new WandInteractionListener(this), this);
        pm.registerEvents(new EntityListener(this), this);
        pm.registerEvents(new ProjectileListener(this), this);

        // Register commands
        var command = getCommand("ew");
        if (command != null) {
            command.setExecutor(new com.example.empirewand.command.EmpireWandCommand(this));
        }

        getLogger().info(String.format("EmpireWand enabled on %s", getServer().getVersion()));

        // Initialize metrics
        this.metricsService.initialize();

        // Register API provider
        EmpireWandAPI.setProvider(this);
    }

    @Override
    public void onDisable() {
        // Shutdown metrics
        if (this.metricsService != null) {
            this.metricsService.shutdown();
        }
        // Plugin shutdown logic (optional)
    }

    // Expose services to internal components
    public SpellRegistry getInternalSpellRegistry() { return spellRegistry; }
    public CooldownService getCooldownService() { return cooldownService; }
    public WandData getWandData() { return wandData; }
    public FxService getFxService() { return fxService; }
    public ConfigService getConfigService() { return configService; }
    public PermissionService getInternalPermissionService() { return permissionService; }
    public TextService getTextService() { return textService; }
    public MetricsService getMetricsService() { return metricsService; }

    // API Provider implementation
    @Override
    public com.example.empirewand.api.SpellRegistry getSpellRegistry() {
        return spellRegistry;
    }

    @Override
    public com.example.empirewand.api.PermissionService getPermissionService() {
        return permissionService;
    }

    @Override
    public com.example.empirewand.api.WandService getWandService() {
        return wandData;
    }
}
