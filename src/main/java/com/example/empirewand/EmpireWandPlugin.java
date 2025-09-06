package com.example.empirewand;

import com.example.empirewand.api.EmpireWandAPI;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.example.empirewand.core.CooldownService;
import com.example.empirewand.core.ConfigService;
import com.example.empirewand.core.FxService;
import com.example.empirewand.core.PermissionServiceImpl;
import com.example.empirewand.core.SpellRegistryImpl;
import com.example.empirewand.core.WandData;
import com.example.empirewand.core.text.TextService;
import com.example.empirewand.core.MetricsService;
import com.example.empirewand.listeners.EntityListener;
import com.example.empirewand.listeners.ProjectileListener;
import com.example.empirewand.listeners.WandInteractionListener;
import com.example.empirewand.listeners.PlayerLifecycleListener;
import com.example.empirewand.visual.Afterimages;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressFBWarnings(value = { "EI_EXPOSE_REP",
        "EI_EXPOSE_REP2" }, justification = "Service getters intentionally expose internal singletons (Bukkit plugin architecture). Wrapping or copying would break shared state semantics and add overhead; consumers treat them as services not data containers.")
public final class EmpireWandPlugin extends JavaPlugin implements EmpireWandAPI.EmpireWandProvider {

    private SpellRegistryImpl spellRegistry;
    private CooldownService cooldownService;
    private WandData wandData;
    private FxService fxService;
    private ConfigService configService;
    private PermissionServiceImpl permissionService;
    private TextService textService;
    private MetricsService metricsService;

    @Override
    public void onEnable() {
        try {
            // Init services (skeletons)
            this.configService = new ConfigService(this);
            this.textService = new TextService();
            this.spellRegistry = new SpellRegistryImpl();
            this.cooldownService = new CooldownService();
            this.wandData = new WandData(this);
            this.fxService = new FxService(this.configService, this.textService);
            this.permissionService = new PermissionServiceImpl();
            this.metricsService = new MetricsService(this, this.configService);

            // Register listeners (skeletons)
            var pm = getServer().getPluginManager();
            pm.registerEvents(new WandInteractionListener(this), this);
            pm.registerEvents(new EntityListener(this), this);
            pm.registerEvents(new ProjectileListener(this), this);
            pm.registerEvents(new PlayerLifecycleListener(this), this);

            // Register commands
            var command = getCommand("ew");
            if (command != null) {
                command.setExecutor(new com.example.empirewand.command.EmpireWandCommand(this));
                command.setTabCompleter(new com.example.empirewand.command.EmpireWandTabCompleter(this));
            } else {
                getLogger().warning("Could not find command 'ew' - command registration failed");
            }

            getLogger().info(String.format("EmpireWand enabled on %s", getServer().getVersion()));

            // Initialize global afterimage system (configurable) under spells.yml root
            // 'afterimages'
            var spellsCfg = this.configService.getSpellsConfig();
            int aiMax = spellsCfg.getInt("afterimages.max-size", 32);
            int aiLifetime = spellsCfg.getInt("afterimages.lifetime-ticks", 18);
            long aiPeriod = spellsCfg.getLong("afterimages.period-ticks", 2L);
            boolean aiEnabled = spellsCfg.getBoolean("afterimages.enabled", true);
            if (aiEnabled) {
                Afterimages.initialize(this, aiMax, aiLifetime, aiPeriod);
            } else {
                getLogger().info("Afterimages disabled via config");
            }

            // Initialize metrics
            if (this.metricsService != null) {
                this.metricsService.initialize();
            }

            // Register API provider
            EmpireWandAPI.setProvider(this);
        } catch (Exception e) {
            getLogger().severe(String.format("Failed to enable EmpireWand: %s", e.getMessage()));
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Shutdown metrics
        if (this.metricsService != null) {
            try {
                this.metricsService.shutdown();
            } catch (Exception e) {
                getLogger().warning(String.format("Error shutting down metrics: %s", e.getMessage()));
            }
        }

        // Reset API provider to no-op
        EmpireWandAPI.clearProvider();

        getLogger().info("EmpireWand has been disabled");
    }

    // Expose services to internal components
    public SpellRegistryImpl getInternalSpellRegistry() {
        return spellRegistry;
    }

    public CooldownService getCooldownService() {
        return cooldownService;
    }

    public WandData getWandData() {
        return wandData;
    }

    public FxService getFxService() {
        return fxService;
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public PermissionServiceImpl getInternalPermissionService() {
        return permissionService;
    }

    public TextService getTextService() {
        return textService;
    }

    public MetricsService getMetricsService() {
        return metricsService;
    }

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
