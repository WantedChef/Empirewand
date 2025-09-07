package com.example.empirewand;

import com.example.empirewand.api.EmpireWandAPI;
import com.example.empirewand.api.PermissionService;
import com.example.empirewand.api.SpellRegistry;
import com.example.empirewand.api.WandService;
import com.example.empirewand.command.EmpireWandCommand;
import com.example.empirewand.command.MephidantesZeistCommand;
import com.example.empirewand.core.services.ConfigService;
import com.example.empirewand.core.services.CooldownService;
import com.example.empirewand.core.services.FxService;
import com.example.empirewand.core.services.SpellRegistryImpl;
import com.example.empirewand.core.services.metrics.DebugMetricsService;
import com.example.empirewand.core.services.metrics.MetricsService;
import com.example.empirewand.core.text.TextService;
import com.example.empirewand.core.util.PerformanceMonitor;
import com.example.empirewand.core.wand.WandServiceImpl;
import com.example.empirewand.listeners.combat.DeathSyncPolymorphListener;
import com.example.empirewand.listeners.combat.ExplosionControlListener;
import com.example.empirewand.listeners.combat.FallDamageEtherealListener;
import com.example.empirewand.listeners.player.PlayerJoinQuitListener;
import com.example.empirewand.listeners.projectile.ProjectileHitListener;
import com.example.empirewand.listeners.wand.WandCastListener;
import com.example.empirewand.listeners.wand.WandDropGuardListener;
import com.example.empirewand.listeners.wand.WandSwapHandListener;
import com.example.empirewand.visual.Afterimages;
// import com.example.empirewand.api.EffectService; // Removed: unused import
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressFBWarnings(value = { "EI_EXPOSE_REP",
        "EI_EXPOSE_REP2" }, justification = "Service getters intentionally expose internal singletons (Bukkit plugin architecture).")
public final class EmpireWandPlugin extends JavaPlugin {

    private com.example.empirewand.core.services.ConfigService configService;
    private TextService textService;
    private PerformanceMonitor performanceMonitor;
    private DebugMetricsService debugMetricsService;
    SpellRegistry spellRegistry;
    com.example.empirewand.core.services.CooldownService cooldownService;
    WandService wandService;
    FxService fxService;
    PermissionService permissionService;
    com.example.empirewand.core.services.metrics.MetricsService metricsService;

    @Override
    public void onEnable() {
        try {
            // Initialize services
            this.configService = new ConfigService(this);
            this.textService = new TextService();
            this.performanceMonitor = new PerformanceMonitor(getLogger());
            this.debugMetricsService = new DebugMetricsService(1000); // 1000 samples
            this.spellRegistry = new SpellRegistryImpl();
            this.cooldownService = new CooldownService();
            this.wandService = new WandServiceImpl(this);
            this.fxService = new FxService(this.textService, this.performanceMonitor);
            this.permissionService = new com.example.empirewand.core.services.PermissionServiceImpl();
            this.metricsService = new MetricsService(this, this.configService, this.spellRegistry,
                    this.debugMetricsService);

            // Register listeners
            registerListeners();

            // Register commands
            registerCommands();

            getLogger().info(String.format("EmpireWand enabled on %s", getServer().getVersion()));

            // Initialize global afterimage system
            initializeAfterimages();

            // Initialize metrics
            if (this.metricsService != null) {
                this.metricsService.initialize();
            }

            // Register API provider
            EmpireWandAPI.setProvider(new EmpireWandProviderImpl(this));
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

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoinQuitListener(this), this);
        pm.registerEvents(new WandCastListener(this), this);
        pm.registerEvents(new WandSwapHandListener(this), this);
        pm.registerEvents(new WandDropGuardListener(this), this);
        pm.registerEvents(new ProjectileHitListener(this), this);
        pm.registerEvents(new FallDamageEtherealListener(this), this);
        pm.registerEvents(new ExplosionControlListener(this), this);
        pm.registerEvents(new DeathSyncPolymorphListener(this), this);
    }

    private void registerCommands() {
        var ewCommand = getCommand("ew");
        if (ewCommand != null) {
            var commandExecutor = new EmpireWandCommand(this);
            ewCommand.setExecutor(commandExecutor);
            ewCommand.setTabCompleter(commandExecutor);
        } else {
            getLogger().warning("Could not find command 'ew' - command registration failed");
        }

        var mzCommand = getCommand("mz");
        if (mzCommand != null) {
            var commandExecutor = new MephidantesZeistCommand(this);
            mzCommand.setExecutor(commandExecutor);
            mzCommand.setTabCompleter(commandExecutor);
        } else {
            getLogger().warning("Could not find command 'mz' - MephidantesZeist command registration failed");
        }
    }

    private void initializeAfterimages() {
        var spellsCfg = this.configService.getSpellsConfig();
        boolean aiEnabled = spellsCfg.getBoolean("afterimages.enabled", true);
        if (aiEnabled) {
            int aiMax = spellsCfg.getInt("afterimages.max-size", 32);
            int aiLifetime = spellsCfg.getInt("afterimages.lifetime-ticks", 18);
            long aiPeriod = spellsCfg.getLong("afterimages.period-ticks", 2L);
            Afterimages.initialize(this, aiMax, aiLifetime, aiPeriod);
        } else {
            getLogger().info("Afterimages disabled via config");
        }
    }

    // Service Getters for internal use
    public SpellRegistry getSpellRegistry() {
        return spellRegistry;
    }

    public WandService getWandService() {
        return wandService;
    }

    public PermissionService getPermissionService() {
        return permissionService;
    }

    public com.example.empirewand.core.services.ConfigService getConfigService() {
        return configService;
    }

    public com.example.empirewand.core.services.CooldownService getCooldownService() {
        return cooldownService;
    }

    public FxService getFxService() {
        return fxService;
    }

    public com.example.empirewand.core.services.metrics.MetricsService getMetricsService() {
        return metricsService;
    }

    public DebugMetricsService getDebugMetricsService() {
        return debugMetricsService;
    }

    public TextService getTextService() {
        return textService;
    }
}

// ===== API PROVIDER IMPLEMENTATION =====

class EmpireWandProviderImpl implements EmpireWandAPI.EmpireWandProvider {
    private final EmpireWandPlugin plugin;

    EmpireWandProviderImpl(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public SpellRegistry getSpellRegistry() {
        return plugin.spellRegistry;
    }

    @Override
    public WandService getWandService() {
        return plugin.wandService;
    }

    @Override
    public PermissionService getPermissionService() {
        return plugin.permissionService;
    }

    @Override
    public com.example.empirewand.api.ConfigService getConfigService() {
        return new com.example.empirewand.api.impl.ConfigServiceAdapter(plugin.getConfigService());
    }

    @Override
    public com.example.empirewand.api.CooldownService getCooldownService() {
        return new com.example.empirewand.api.impl.CooldownServiceAdapter(plugin.getCooldownService());
    }

    @Override
    public com.example.empirewand.api.EffectService getEffectService() {
        return new com.example.empirewand.api.impl.EffectServiceAdapter(plugin.getFxService());
    }

    @Override
    public com.example.empirewand.api.MetricsService getMetricsService() {
        return new com.example.empirewand.api.impl.MetricsServiceAdapter(
                plugin.getMetricsService(),
                plugin.getDebugMetricsService());
    }

    @Override
    public com.example.empirewand.api.Version getAPIVersion() {
        return com.example.empirewand.api.Version.of(2, 0, 0);
    }

    @Override
    public boolean isCompatible(com.example.empirewand.api.Version version) {
        return version.getMajor() == 2 && version.getMinor() <= 0;
    }
}