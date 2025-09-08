package com.example.empirewand;

import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

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
import com.example.empirewand.core.storage.Keys;
import com.example.empirewand.core.task.TaskManager;
import com.example.empirewand.core.text.TextService;
import com.example.empirewand.core.util.PerformanceMonitor;
import com.example.empirewand.listeners.combat.DeathSyncPolymorphListener;
import com.example.empirewand.listeners.combat.PolymorphCleanupListener;
import com.example.empirewand.listeners.combat.ExplosionControlListener;
import com.example.empirewand.listeners.combat.FallDamageEtherealListener;
import com.example.empirewand.listeners.player.PlayerJoinQuitListener;
import com.example.empirewand.listeners.projectile.ProjectileHitListener;
import com.example.empirewand.listeners.wand.WandCastListener;
import com.example.empirewand.listeners.wand.WandDropGuardListener;
import com.example.empirewand.listeners.wand.WandSwapHandListener;
import com.example.empirewand.visual.Afterimages;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
    private TaskManager taskManager;

    @Override
    public void onEnable() {
        try {
            // Initialize task manager first
            this.taskManager = new TaskManager(this);

            // Initialize core services
            this.configService = new ConfigService(this);
            this.textService = new TextService();
            this.performanceMonitor = new PerformanceMonitor(getLogger());
            this.debugMetricsService = new DebugMetricsService(1000); // 1000 samples
            this.cooldownService = new CooldownService();
            this.fxService = new FxService(this.textService, this.performanceMonitor);
            this.permissionService = new com.example.empirewand.core.services.PermissionServiceImpl();

            // Register API provider early so services depending on API can use it
            EmpireWandAPI.setProvider(new EmpireWandProviderImpl(this));

            // Spell registry no longer requires an EmpireWandAPI instance; builders are
            // constructed without it
            this.spellRegistry = new SpellRegistryImpl(this.configService);

            // Metrics after spell registry exists
            this.metricsService = new MetricsService(this, this.configService, this.spellRegistry,
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
            Afterimages.shutdown();
            getLogger().info("Afterimages system shut down");
        } catch (Exception e) {
            getLogger().warning(String.format("Error shutting down afterimages: %s", e.getMessage()));
        }

        // 3. SpellRegistry – no explicit shutdown needed; ensure no hard references
        // linger
        if (this.spellRegistry != null) {
            try {
                getLogger().info("SpellRegistry ready for shutdown");
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

        // 6. Cleanup all active spells with ethereal forms, polymorphs, etc.
        try {
            cleanupActiveSpells();
            var poly = this.spellRegistry.getSpell("polymorph");
            if (poly.isPresent()
                    && poly.get() instanceof com.example.empirewand.spell.implementation.control.Polymorph p) {
                p.cleanup();
            }
            getLogger().info("Active spells cleaned up");
        } catch (Exception e) {
            getLogger().warning(String.format("Error cleaning up active spells: %s", e.getMessage()));
        }

        // 7. Unregister all event listeners
        try {
            HandlerList.unregisterAll(this);
            getLogger().info("Event listeners unregistered");
        } catch (Exception e) {
            getLogger().warning(String.format("Error unregistering event listeners: %s", e.getMessage()));
        }

        // 8. Shutdown metrics (existing code)
        if (this.metricsService != null) {
            try {
                this.metricsService.shutdown();
                getLogger().info("Metrics service shut down");
            } catch (Exception e) {
                getLogger().warning(String.format("Error shutting down metrics: %s", e.getMessage()));
            }
        }

        // 9. Reset API provider to no-op (existing code)
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
        pm.registerEvents(new PolymorphCleanupListener(this), this);
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
                    && polymorph.get() instanceof com.example.empirewand.spell.implementation.control.Polymorph poly) {
                var entities = poly.getPolymorphedEntities();
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
