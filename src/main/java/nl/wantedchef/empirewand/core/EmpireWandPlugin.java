package nl.wantedchef.empirewand.core;

import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.PermissionService;
import nl.wantedchef.empirewand.api.service.WandService;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.command.EmpireWandCommand;
import nl.wantedchef.empirewand.command.MephidantesZeistCommand;
// import nl.wantedchef.empirewand.common.visual.Afterimages; // TODO: Create Afterimages class
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.core.task.TaskManager;
import nl.wantedchef.empirewand.core.text.TextService;
import nl.wantedchef.empirewand.core.util.PerformanceMonitor;
import nl.wantedchef.empirewand.core.config.ConfigService;
import nl.wantedchef.empirewand.core.services.CooldownService;
import nl.wantedchef.empirewand.core.services.FxService;
import nl.wantedchef.empirewand.core.services.DebugMetricsService;
import nl.wantedchef.empirewand.core.services.MetricsService;
import nl.wantedchef.empirewand.listener.combat.DeathSyncPolymorphListener;
import nl.wantedchef.empirewand.listener.combat.ExplosionControlListener;
import nl.wantedchef.empirewand.listener.combat.FallDamageEtherealListener;
import nl.wantedchef.empirewand.listener.combat.PolymorphCleanupListener;
import nl.wantedchef.empirewand.listener.player.PlayerJoinQuitListener;
import nl.wantedchef.empirewand.listener.projectile.ProjectileHitListener;
import nl.wantedchef.empirewand.listener.wand.WandCastListener;
import nl.wantedchef.empirewand.listener.wand.WandDropGuardListener;
import nl.wantedchef.empirewand.listener.wand.WandSelectListener;
import nl.wantedchef.empirewand.listener.wand.WandStatusListener;
import nl.wantedchef.empirewand.listener.wand.WandSwapHandListener;

@SuppressFBWarnings(value = { "EI_EXPOSE_REP",
        "EI_EXPOSE_REP2" }, justification = "Service getters intentionally expose internal singletons (Bukkit plugin architecture).")
public final class EmpireWandPlugin extends JavaPlugin {

    private ConfigService configService;
    private TextService textService;
    private PerformanceMonitor performanceMonitor;
    private DebugMetricsService debugMetricsService;
    SpellRegistry spellRegistry;
    CooldownService cooldownService;
    WandService wandService;
    FxService fxService;
    PermissionService permissionService;
    MetricsService metricsService;
    private TaskManager taskManager;

    @Override
    public void onEnable() {
        try {
            // Initialize task manager first
            this.taskManager = new nl.wantedchef.empirewand.core.task.TaskManager(this);

            // Initialize core services
            this.configService = new nl.wantedchef.empirewand.core.config.ConfigService(this);
            this.textService = new nl.wantedchef.empirewand.core.text.TextService();
            this.performanceMonitor = new nl.wantedchef.empirewand.core.util.PerformanceMonitor(getLogger());
            this.debugMetricsService = new nl.wantedchef.empirewand.core.services.DebugMetricsService(1000); // 1000
                                                                                                             // samples
            this.cooldownService = new nl.wantedchef.empirewand.core.services.CooldownService(this);
            this.fxService = new nl.wantedchef.empirewand.core.services.FxService(this.textService,
                    this.performanceMonitor);
            this.permissionService = new nl.wantedchef.empirewand.core.services.PermissionServiceImpl();

            // Register API provider early so services depending on API can use it
            EmpireWandAPI.setProvider(new EmpireWandProviderImpl(this));

            // Spell registry no longer requires an EmpireWandAPI instance; builders are
            // constructed without it
            this.spellRegistry = new nl.wantedchef.empirewand.framework.registry.SpellRegistryImpl(this.configService);

            // Initialize wand service now that spell registry exists
            this.wandService = new nl.wantedchef.empirewand.core.services.WandServiceImpl(this, this.spellRegistry);
            // Initialize default templates after construction to avoid 'this' escape
            if (this.wandService instanceof nl.wantedchef.empirewand.core.services.WandServiceImpl ws) {
                ws.initializeDefaultTemplates();
            }

            // Metrics after spell registry exists
            this.metricsService = new nl.wantedchef.empirewand.framework.metrics.services.MetricsService(this,
                    this.configService, this.spellRegistry,
                    this.debugMetricsService);

            // Register listeners & commands
            registerListeners();
            registerCommands();

            getLogger().info(String.format("EmpireWand enabled on %s", getServer().getVersion()));

            // Initialize global afterimage system
            initializeAfterimages();

            // Initialize metrics
            if (this.metricsService != null) {
                this.metricsService.initialize();
            }

        } catch (Exception e) {
            getLogger().severe(String.format("Failed to enable EmpireWand: %s", e.getMessage()));
            // Log full stack trace through logger to avoid direct stderr output
            for (StackTraceElement ste : e.getStackTrace()) {
                getLogger().severe(String.format("  at %s", ste.toString()));
            }
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Starting EmpireWand shutdown...");

        // 1. Cancel all scheduled tasks first (use our task manager)
        if (this.taskManager != null) {
            try {
                this.taskManager.cancelAllTasks();
                getLogger().info(String.format("Cancelled all scheduled tasks (%d tasks)",
                        this.taskManager.getActiveTaskCount()));
            } catch (Exception e) {
                getLogger().warning(String.format("Error cancelling scheduled tasks: %s", e.getMessage()));
            }
        }

        // 2. Cleanup Afterimages system
        try {
            // Afterimages.shutdown(); // TODO: Implement Afterimages
            getLogger().info("Afterimages system shut down (placeholder)");
        } catch (Exception e) {
            getLogger().warning(String.format("Error shutting down afterimages: %s", e.getMessage()));
        }

        // 3. SpellRegistry – clean up spells
        if (this.spellRegistry != null) {
            try {
                if (this.spellRegistry instanceof nl.wantedchef.empirewand.core.services.SpellRegistryImpl sr) {
                    sr.shutdown();
                }
                getLogger().info("SpellRegistry shut down");
            } catch (Exception e) {
                getLogger().warning(String.format("Error handling spell registry during shutdown: %s", e.getMessage()));
            }
        }

        // 4. Cleanup CooldownService
        if (this.cooldownService != null) {
            try {
                this.cooldownService.shutdown();
                getLogger().info("CooldownService shut down");
            } catch (Exception e) {
                getLogger().warning(String.format("Error shutting down cooldown service: %s", e.getMessage()));
            }
        }

        // 5. Cleanup FxService
        if (this.fxService != null) {
            try {
                this.fxService.shutdown();
                getLogger().info("FxService shut down");
            } catch (Exception e) {
                getLogger().warning(String.format("Error shutting down FX service: %s", e.getMessage()));
            }
        }

        // 6. Cleanup WandService
        if (this.wandService != null) {
            try {
                if (this.wandService instanceof nl.wantedchef.empirewand.core.services.WandServiceImpl ws) {
                    ws.shutdown();
                }
                getLogger().info("WandService shut down");
            } catch (Exception e) {
                getLogger().warning(String.format("Error shutting down WandService: %s", e.getMessage()));
            }
        }

        // 7. Cleanup all active spells with ethereal forms, polymorphs, etc.
        try {
            cleanupActiveSpells();
            // Only attempt polymorph cleanup if spellRegistry is available
            if (this.spellRegistry != null) {
                var poly = this.spellRegistry.getSpell("polymorph");
                if (poly.isPresent()
                        && poly.get() instanceof nl.wantedchef.empirewand.spell.control.Polymorph p) {
                    p.cleanup();
                }
            }
            getLogger().info("Active spells cleaned up");
        } catch (Exception e) {
            getLogger().warning(String.format("Error cleaning up active spells: %s", e.getMessage()));
        }

        // 8. Unregister all event listeners
        try {
            HandlerList.unregisterAll(this);
            getLogger().info("Event listeners unregistered");
        } catch (Exception e) {
            getLogger().warning(String.format("Error unregistering event listeners: %s", e.getMessage()));
        }

        // 9. Shutdown metrics (existing code)
        if (this.metricsService != null) {
            try {
                this.metricsService.shutdown();
                getLogger().info("Metrics service shut down");
            } catch (Exception e) {
                getLogger().warning(String.format("Error shutting down metrics: %s", e.getMessage()));
            }
        }

        // 10. Reset API provider to no-op (existing code)
        EmpireWandAPI.clearProvider();

        getLogger().info("EmpireWand has been disabled");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoinQuitListener(this), this);
        pm.registerEvents(new nl.wantedchef.empirewand.listener.player.FlightListener(this), this);
        pm.registerEvents(new WandCastListener(this), this);
        pm.registerEvents(new WandSelectListener(this), this);
        pm.registerEvents(new WandStatusListener(this), this);
        pm.registerEvents(new WandSwapHandListener(this), this);
        pm.registerEvents(new WandDropGuardListener(this), this);
        pm.registerEvents(new ProjectileHitListener(this), this);
        pm.registerEvents(new FallDamageEtherealListener(this), this);
        pm.registerEvents(new ExplosionControlListener(this), this);
        pm.registerEvents(new DeathSyncPolymorphListener(this), this);
        pm.registerEvents(new PolymorphCleanupListener(this), this);
    }

    private void registerCommands() {
        var ewCommand = getCommand("ew");
        if (ewCommand != null) {
            // var commandExecutor = new
            // nl.wantedchef.empirewand.command.EmpireWandCommand(this); // TODO: Create
            // EmpireWandCommand
            // ewCommand.setExecutor(commandExecutor);
            // ewCommand.setTabCompleter(commandExecutor);
            getLogger().warning("Command 'ew' registration placeholder - command class missing");
        } else {
            getLogger().warning("Could not find command 'ew' - command registration failed");
        }

        var mzCommand = getCommand("mz");
        if (mzCommand != null) {
            // var commandExecutor = new
            // nl.wantedchef.empirewand.command.MephidantesZeistCommand(this); // TODO:
            // Create MephidantesZeistCommand
            // mzCommand.setExecutor(commandExecutor);
            // mzCommand.setTabCompleter(commandExecutor);
            getLogger().warning("Command 'mz' registration placeholder - command class missing");
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
            // Afterimages.initialize(this, aiMax, aiLifetime, aiPeriod); // TODO: Implement
            // Afterimages
            getLogger().info("Afterimages initialized (placeholder)");
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

    public ConfigService getConfigService() {
        return configService;
    }

    public CooldownService getCooldownService() {
        return cooldownService;
    }

    public FxService getFxService() {
        return fxService;
    }

    public MetricsService getMetricsService() {
        return metricsService;
    }

    public DebugMetricsService getDebugMetricsService() {
        return debugMetricsService;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    public TextService getTextService() {
        return textService;
    }

    /**
     * Get the task manager for centralized task tracking
     */
    public TaskManager getTaskManager() {
        return taskManager;
    }

    /**
     * Clean up all active spell effects when disabling
     */
    private void cleanupActiveSpells() {
        // Clean up all players with active ethereal forms
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getPersistentDataContainer().has(Keys.ETHEREAL_ACTIVE)) {
                player.setCollidable(true);
                player.setInvulnerable(false);
                player.getPersistentDataContainer().remove(Keys.ETHEREAL_ACTIVE);
            }
        }

        // Clean up polymorph entities - get polymorph spell and clean its map
        try {
            var polymorph = this.spellRegistry.getSpell("polymorph");
            if (polymorph.isPresent()
                    && polymorph.get() instanceof nl.wantedchef.empirewand.spell.control.Polymorph p) {
                var entities = p.getPolymorphedEntities();
                for (var entry : entities.entrySet()) {
                    UUID sheepId = entry.getKey();
                    UUID originalId = entry.getValue();

                    // Remove sheep
                    Entity sheep = getServer().getEntity(sheepId);
                    if (sheep != null && sheep.isValid()) {
                        sheep.remove();
                    }

                    // Restore original if possible
                    Entity original = getServer().getEntity(originalId);
                    if (original instanceof LivingEntity living && living.isValid() && !living.isDead()) {
                        living.setAI(true);
                        living.setInvulnerable(false);
                        living.setInvisible(false);
                        living.setCollidable(true);
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning(String.format("Error cleaning up polymorph entities: %s", e.getMessage()));
        }
    }
}

// ===== API PROVIDER IMPLEMENTATION =====

record EmpireWandProviderImpl(EmpireWandPlugin plugin) implements EmpireWandAPI.EmpireWandProvider {

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
    public nl.wantedchef.empirewand.api.service.ConfigService getConfigService() {
        return new nl.wantedchef.empirewand.api.impl.ConfigServiceAdapter(plugin.getConfigService());
    }

    @Override
    public nl.wantedchef.empirewand.api.service.CooldownService getCooldownService() {
        return new nl.wantedchef.empirewand.api.impl.CooldownServiceAdapter(plugin.getCooldownService());
    }

    @Override
    public nl.wantedchef.empirewand.api.service.EffectService getEffectService() {
        return new nl.wantedchef.empirewand.api.impl.EffectServiceAdapter(plugin.getFxService());
    }

    @Override
    public nl.wantedchef.empirewand.api.service.MetricsService getMetricsService() {
        return new nl.wantedchef.empirewand.api.impl.MetricsServiceAdapter(
                plugin.getMetricsService(),
                plugin.getDebugMetricsService());
    }

    @Override
    public nl.wantedchef.empirewand.api.Version getAPIVersion() {
        return nl.wantedchef.empirewand.api.Version.of(2, 0, 0);
    }

    @Override
    public boolean isCompatible(nl.wantedchef.empirewand.api.Version version) {
        return version.getMajor() == 2 && version.getMinor() <= 0;
    }

}
