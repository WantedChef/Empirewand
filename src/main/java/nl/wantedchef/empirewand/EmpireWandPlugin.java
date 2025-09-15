package nl.wantedchef.empirewand;

import java.util.UUID;
import java.util.Map;

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
import nl.wantedchef.empirewand.common.visual.Afterimages;
import nl.wantedchef.empirewand.core.event.EventBusSystem;
import nl.wantedchef.empirewand.core.integration.OptimizedServiceRegistry;
import nl.wantedchef.empirewand.core.storage.Keys;
import nl.wantedchef.empirewand.core.task.TaskManager;
import nl.wantedchef.empirewand.core.text.TextService;
import nl.wantedchef.empirewand.core.util.PerformanceMonitor;
import nl.wantedchef.empirewand.core.logging.StructuredLogger;
import nl.wantedchef.empirewand.framework.service.ConfigService;
import nl.wantedchef.empirewand.framework.service.UnifiedCooldownManager;
import nl.wantedchef.empirewand.framework.service.FxService;
import nl.wantedchef.empirewand.framework.service.metrics.DebugMetricsService;
import nl.wantedchef.empirewand.framework.service.metrics.MetricsService;
import nl.wantedchef.empirewand.listener.combat.BloodBarrierDamageListener;
import nl.wantedchef.empirewand.listener.combat.DeathSyncPolymorphListener;
import nl.wantedchef.empirewand.listener.combat.ElementosgodDamageListener;
import nl.wantedchef.empirewand.listener.combat.ExplosionControlListener;
import nl.wantedchef.empirewand.listener.combat.FallDamageEtherealListener;
import nl.wantedchef.empirewand.listener.combat.PolymorphCleanupListener;
import nl.wantedchef.empirewand.listener.player.PlayerJoinQuitListener;
import nl.wantedchef.empirewand.listener.player.SpellCleanupListener;
import nl.wantedchef.empirewand.listener.projectile.ProjectileHitListener;

import nl.wantedchef.empirewand.listener.spell.AuraSpellListener;
import nl.wantedchef.empirewand.listener.wand.WandCastListener;
import nl.wantedchef.empirewand.listener.wand.WandDropGuardListener;
import nl.wantedchef.empirewand.listener.wand.WandSelectListener;
import nl.wantedchef.empirewand.listener.wand.WandStatusListener;
import nl.wantedchef.empirewand.listener.wand.WandSwapHandListener;

/**
 * EmpireWand - Advanced Minecraft Paper Plugin for Magical Wand Mechanics
 * 
 * <p>EmpireWand is a comprehensive Minecraft plugin that provides an extensive magical wand system
 * with over 50 configurable spells across multiple elemental categories. The plugin features
 * enterprise-grade architecture with modern Java 21 features, comprehensive testing, and
 * professional development practices.</p>
 * 
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li><strong>Spell System:</strong> 50+ spells across 17 categories including Fire, Ice, Lightning, Dark, Life, Movement, and more</li>
 *   <li><strong>Advanced Architecture:</strong> Service-oriented design with dependency injection and lifecycle management</li>
 *   <li><strong>Performance Optimized:</strong> Async spell processing, caching, and performance monitoring</li>
 *   <li><strong>Thread Safety:</strong> Concurrent access protection and thread-safe operations</li>
 *   <li><strong>Comprehensive Testing:</strong> 80%+ code coverage with professional test suites</li>
 *   <li><strong>Rich GUI:</strong> Professional inventory interfaces with TriumphGUI integration</li>
 *   <li><strong>Localization:</strong> Multi-language support with Dutch translations</li>
 *   <li><strong>API Integration:</strong> Clean API for plugin integration and extensibility</li>
 * </ul>
 * 
 * <p><strong>Plugin Lifecycle:</strong></p>
 * <p>The plugin follows a sophisticated initialization sequence:</p>
 * <ol>
 *   <li><strong>Service Registration:</strong> 15+ core services are registered and initialized</li>
 *   <li><strong>Configuration Loading:</strong> YAML-based configuration with validation and migration</li>
 *   <li><strong>Spell Registration:</strong> Dynamic spell discovery and registration</li>
 *   <li><strong>Event System:</strong> Advanced event bus with async processing</li>
 *   <li><strong>Command Registration:</strong> Dual command aliases (/ew and /mz)</li>
 *   <li><strong>Listener Registration:</strong> Comprehensive event handling</li>
 * </ol>
 * 
 * <p><strong>Service Architecture:</strong></p>
 * <p>The plugin uses a service registry pattern with the following core services:</p>
 * <ul>
 *   <li><strong>ConfigService:</strong> Configuration management with validation and caching</li>
 *   <li><strong>UnifiedCooldownManager:</strong> Thread-safe cooldown tracking and management</li>
 *   <li><strong>FxService:</strong> Particle effects and sound coordination</li>
 *   <li><strong>SpellRegistry:</strong> Spell registration, discovery, and metadata management</li>
 *   <li><strong>WandService:</strong> Wand creation, management, and persistence</li>
 *   <li><strong>PermissionService:</strong> Permission validation and management</li>
 *   <li><strong>MetricsService:</strong> Performance monitoring and analytics</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong></p>
 * <p>All services are designed to be thread-safe with proper synchronization mechanisms.
 * Spell execution can be either synchronous (main thread) or asynchronous (background threads)
 * based on the spell's requirements.</p>
 * 
 * <p><strong>Performance Features:</strong></p>
 * <ul>
 *   <li>Async spell processing for heavy operations</li>
 *   <li>Configurable particle limits to prevent lag</li>
 *   <li>Efficient cooldown caching with Caffeine</li>
 *   <li>Memory management with weak references</li>
 *   <li>Performance monitoring and metrics collection</li>
 * </ul>
 * 
 * <p><strong>API Usage:</strong></p>
 * <pre>{@code
 * // Get the API provider
 * EmpireWandAPI.EmpireWandProvider provider = EmpireWandAPI.getProvider();
 * 
 * // Access services
 * SpellRegistry spells = provider.getSpellRegistry();
 * CooldownService cooldowns = provider.getCooldownService();
 * ConfigService config = provider.getConfigService();
 * }</pre>
 * 
 * <p><strong>Configuration:</strong></p>
 * <p>The plugin uses YAML-based configuration with the following files:</p>
 * <ul>
 *   <li><strong>config.yml:</strong> Main plugin configuration (600+ lines)</li>
 *   <li><strong>spells.yml:</strong> Individual spell definitions (2000+ lines)</li>
 *   <li><strong>messages.properties:</strong> Localized messages</li>
 *   <li><strong>plugin.yml:</strong> Plugin metadata and permissions</li>
 * </ul>
 * 
 * <p><strong>Commands:</strong></p>
 * <p>The plugin provides dual command aliases with comprehensive functionality:</p>
 * <ul>
 *   <li><code>/ew get [player]</code> - Get a wand</li>
 *   <li><code>/ew bind &lt;spell&gt;</code> - Bind a spell to wand</li>
 *   <li><code>/ew list</code> - List available spells</li>
 *   <li><code>/ew spells</code> - View spell information</li>
 *   <li><code>/ew stats [player]</code> - View wand statistics</li>
 *   <li><code>/ew reload</code> - Reload configuration</li>
 * </ul>
 * 
 * <p><strong>Version Information:</strong></p>
 * <ul>
 *   <li><strong>Plugin Version:</strong> 1.1.1</li>
 *   <li><strong>Java Version:</strong> 21 (with Java 17+ Gradle daemon support)</li>
 *   <li><strong>Minecraft Version:</strong> Paper 1.20.6-R0.1-SNAPSHOT</li>
 *   <li><strong>API Version:</strong> 1.20</li>
 * </ul>
 * 
 * <p><strong>Author:</strong> ChefWanted</p>
 * <p><strong>License:</strong> MIT License</p>
 * 
 * @since 1.0.0
 * @version 1.1.1
 * @author ChefWanted
 * @see EmpireWandAPI
 * @see SpellRegistry
 * @see ConfigService
 */
@SuppressFBWarnings(value = { "EI_EXPOSE_REP",
        "EI_EXPOSE_REP2" }, justification = "Service getters intentionally expose internal singletons (Bukkit plugin architecture).")
public final class EmpireWandPlugin extends JavaPlugin {

    private ConfigService configService;
    private TextService textService;
    private PerformanceMonitor performanceMonitor;
    private DebugMetricsService debugMetricsService;
    private StructuredLogger structuredLogger;
    private SpellRegistry spellRegistry;
    private UnifiedCooldownManager cooldownManager;
    private WandService wandService;
    private FxService fxService;
    private PermissionService permissionService;
    private MetricsService metricsService;
    private TaskManager taskManager;
    private WandStatusListener wandStatusListener;
    private nl.wantedchef.empirewand.api.spell.toggle.SpellManager spellManager;
    private OptimizedServiceRegistry serviceRegistry;
    private EventBusSystem eventBus;

    @Override
    public void onEnable() {
        try {
            // Initialize task manager first
            this.taskManager = new nl.wantedchef.empirewand.core.task.TaskManager(this);

            // Initialize event bus and service registry
            this.eventBus = new EventBusSystem(this);
            this.serviceRegistry = new OptimizedServiceRegistry(this, this.eventBus);

            // Initialize core services
            this.configService = new nl.wantedchef.empirewand.framework.service.ConfigService(this);
            this.textService = new nl.wantedchef.empirewand.core.text.TextService();
            this.performanceMonitor = new nl.wantedchef.empirewand.core.util.PerformanceMonitor(getLogger());
            this.debugMetricsService = new nl.wantedchef.empirewand.framework.service.metrics.DebugMetricsService(1000); // 1000
                                                                                                                     // samples
            this.structuredLogger = new StructuredLogger(getLogger());
            this.cooldownManager = new UnifiedCooldownManager(this);
            this.fxService = new nl.wantedchef.empirewand.framework.service.FxService(this.textService,
                    this.performanceMonitor, this.structuredLogger);
            this.permissionService = new nl.wantedchef.empirewand.framework.service.PermissionServiceImpl();

            // Initialize toggle SpellManager
            this.spellManager = new nl.wantedchef.empirewand.framework.service.toggle.SpellManagerImpl(this);

            // Register API provider early so services depending on API can use it
            EmpireWandAPI.setProvider(new EmpireWandProviderImpl(this));

            // Spell registry no longer requires an EmpireWandAPI instance; builders are
            // constructed without it
            this.spellRegistry = new nl.wantedchef.empirewand.framework.service.SpellRegistryImpl(this.configService);

            // Initialize wand service now that spell registry exists
            this.wandService = new nl.wantedchef.empirewand.framework.service.WandServiceImpl(this, this.spellRegistry);

            // Metrics after spell registry exists
            this.metricsService = new nl.wantedchef.empirewand.framework.service.metrics.MetricsService(this,
                    this.configService, this.spellRegistry,
                    this.debugMetricsService);

            // Register all services with the registry
            registerServices();

            // Start all registered services
            this.serviceRegistry.startServices().join(); // Wait for services to start

            // Register listeners & commands
            registerListeners();
            registerCommands();

            getLogger().info(String.format("EmpireWand enabled on %s", getServer().getVersion()));
            
            // Log plugin startup with structured logging
            this.structuredLogger.logSystemEvent("plugin_startup", "EmpireWand plugin started successfully", 
                Map.of("version", getDescription().getVersion(), 
                       "server_version", getServer().getVersion(),
                       "java_version", System.getProperty("java.version")));

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

        // 3. SpellRegistry – clean up spells
        if (this.spellRegistry != null) {
            try {
                if (this.spellRegistry instanceof nl.wantedchef.empirewand.framework.service.SpellRegistryImpl sr) {
                    sr.shutdown();
                }
                getLogger().info("SpellRegistry shut down");
            } catch (Exception e) {
                getLogger().warning(String.format("Error handling spell registry during shutdown: %s", e.getMessage()));
            }
        }

        // 4. Cleanup CooldownManager
        if (this.cooldownManager != null) {
            try {
                this.cooldownManager.shutdown();
                getLogger().info("UnifiedCooldownManager shut down");
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

        // 6. Shutdown WandStatusListener
        if (this.wandStatusListener != null) {
            try {
                this.wandStatusListener.shutdown();
                getLogger().info("WandStatusListener shut down");
            } catch (Exception e) {
                getLogger().warning(String.format("Error shutting down WandStatusListener: %s", e.getMessage()));
            }
        }

        // 7. Stop all services
        if (this.serviceRegistry != null) {
            try {
                this.serviceRegistry.stopServices().join(); // Wait for services to stop
                getLogger().info("All services have been stopped.");
            } catch (Exception e) {
                getLogger().warning(String.format("Error stopping services: %s", e.getMessage()));
            }
        }

        // 7. Cleanup WandService
        if (this.wandService != null) {
            try {
                if (this.wandService instanceof nl.wantedchef.empirewand.framework.service.WandServiceImpl ws) {
                    ws.shutdown();
                }
                getLogger().info("WandService shut down");
            } catch (Exception e) {
                getLogger().warning(String.format("Error shutting down WandService: %s", e.getMessage()));
            }
        }

        // 8. Cleanup all active spells with ethereal forms, polymorphs, etc.
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
        
        // Core player and spell cleanup listeners
        pm.registerEvents(new PlayerJoinQuitListener(this), this);
        pm.registerEvents(new SpellCleanupListener(this), this);
        
        // Wand interaction listeners
        pm.registerEvents(new WandCastListener(this), this);
        pm.registerEvents(new nl.wantedchef.empirewand.listener.wand.WandSwingListener(this), this);
        pm.registerEvents(new WandSelectListener(this), this);
        pm.registerEvents(new WandSwapHandListener(this), this);
        pm.registerEvents(new WandDropGuardListener(this), this);
        
        // Store reference to WandStatusListener for proper shutdown
        this.wandStatusListener = new WandStatusListener(this);
        pm.registerEvents(this.wandStatusListener, this);
        
        // Projectile and combat listeners
        pm.registerEvents(new ProjectileHitListener(this), this);
        pm.registerEvents(new FallDamageEtherealListener(this), this);
        pm.registerEvents(new ExplosionControlListener(this), this);
        pm.registerEvents(new DeathSyncPolymorphListener(this), this);
        pm.registerEvents(new PolymorphCleanupListener(this), this);
        pm.registerEvents(new BloodBarrierDamageListener(this), this);
        pm.registerEvents(new ElementosgodDamageListener(this), this);
        pm.registerEvents(new nl.wantedchef.empirewand.listener.combat.MinionFriendlyFireListener(), this);
        
        // Enhanced spell effect listeners
        pm.registerEvents(new nl.wantedchef.empirewand.listener.spell.StatusEffectListener(this), this);
        pm.registerEvents(new nl.wantedchef.empirewand.listener.spell.LightningEffectListener(this), this);
        pm.registerEvents(new nl.wantedchef.empirewand.listener.spell.MovementSpellListener(this), this);
        pm.registerEvents(new nl.wantedchef.empirewand.listener.spell.EnvironmentalSpellListener(this), this);
        pm.registerEvents(new nl.wantedchef.empirewand.listener.spell.WaveSpellListener(this), this);
        // AuraSpellListener is now a service, its events are registered in its @PostConstruct method.

        // GUI click listener for wand rules system
        pm.registerEvents(new nl.wantedchef.empirewand.gui.GuiClickListener(getLogger()), this);

        // Performance monitoring (always register last for accurate tracking)
        pm.registerEvents(new nl.wantedchef.empirewand.listener.spell.PerformanceMonitoringListener(this), this);
        
        getLogger().info("Registered " + (14 + 6 + 1) + " spell listeners for comprehensive spell coverage");
        
        // Analyze and report listener coverage
        nl.wantedchef.empirewand.listener.ListenerCoverageAnalyzer coverageAnalyzer = 
                new nl.wantedchef.empirewand.listener.ListenerCoverageAnalyzer(this);
        coverageAnalyzer.printCoverageReport();
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

    private void registerServices() {
        // Register all services that should be managed by the service registry.
        this.serviceRegistry.registerServiceInstance(EmpireWandPlugin.class, this);
        this.serviceRegistry.registerServiceInstance(ConfigService.class, this.configService);
        this.serviceRegistry.registerServiceInstance(TextService.class, this.textService);
        this.serviceRegistry.registerServiceInstance(PerformanceMonitor.class, this.performanceMonitor);
        this.serviceRegistry.registerServiceInstance(DebugMetricsService.class, this.debugMetricsService);
        this.serviceRegistry.registerServiceInstance(StructuredLogger.class, this.structuredLogger);
        this.serviceRegistry.registerServiceInstance(UnifiedCooldownManager.class, this.cooldownManager);
        this.serviceRegistry.registerServiceInstance(FxService.class, this.fxService);
        this.serviceRegistry.registerServiceInstance(PermissionService.class, this.permissionService);
        this.serviceRegistry.registerServiceInstance(nl.wantedchef.empirewand.api.spell.toggle.SpellManager.class, this.spellManager);
        this.serviceRegistry.registerServiceInstance(SpellRegistry.class, this.spellRegistry);
        this.serviceRegistry.registerServiceInstance(WandService.class, this.wandService);
        this.serviceRegistry.registerServiceInstance(MetricsService.class, this.metricsService);
        this.serviceRegistry.registerServiceInstance(TaskManager.class, this.taskManager);
        this.serviceRegistry.registerServiceInstance(WandStatusListener.class, this.wandStatusListener);
        this.serviceRegistry.registerServiceInstance(java.util.logging.Logger.class, getLogger());

        // Register AuraSpellListener as a service class, so the registry can instantiate it.
        this.serviceRegistry.registerService(AuraSpellListener.class, (context) -> new AuraSpellListener());
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

    public ConfigService getConfigService() {
        return configService;
    }

    public UnifiedCooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public FxService getFxService() {
        return fxService;
    }

    public MetricsService getMetricsService() {
        return metricsService;
    }

    public nl.wantedchef.empirewand.api.spell.toggle.SpellManager getSpellManager() {
        return spellManager;
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

    public StructuredLogger getStructuredLogger() {
        return structuredLogger;
    }

    /**
     * Get the task manager for centralized task tracking
     */
    public TaskManager getTaskManager() {
        return taskManager;
    }

    public OptimizedServiceRegistry getServiceRegistry() {
        return this.serviceRegistry;
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
            if (this.spellRegistry != null) {
                var polymorph = this.spellRegistry.getSpell("polymorph");
                if (polymorph.isPresent()
                        && polymorph.get() instanceof nl.wantedchef.empirewand.spell.control.Polymorph p) {
                var entities = p.getPolymorphedEntities();
                for (var entry : entities.entrySet()) {
                    UUID sheepId = entry.getKey();
                    UUID originalId = entry.getValue();

                    // Remove sheep safely
                    try {
                        Entity sheep = getServer().getEntity(sheepId);
                        if (sheep != null && sheep.isValid() && sheep.getWorld().isChunkLoaded(sheep.getChunk())) {
                            sheep.remove();
                        }
                    } catch (Exception ex) {
                        getLogger().fine("Failed to remove polymorph sheep " + sheepId + ": " + ex.getMessage());
                    }

                    // Restore original safely
                    try {
                        Entity original = getServer().getEntity(originalId);
                        if (original instanceof LivingEntity living &&
                            living.isValid() &&
                            !living.isDead() &&
                            living.getWorld().isChunkLoaded(living.getChunk())) {
                            living.setAI(true);
                            living.setInvulnerable(false);
                            living.setInvisible(false);
                            living.setCollidable(true);
                        }
                    } catch (Exception ex) {
                        getLogger().fine("Failed to restore original entity " + originalId + ": " + ex.getMessage());
                    }
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
        return plugin.getSpellRegistry();
    }

    @Override
    public WandService getWandService() {
        return plugin.getWandService();
    }

    @Override
    public PermissionService getPermissionService() {
        return plugin.getPermissionService();
    }

    @Override
    public nl.wantedchef.empirewand.api.service.ConfigService getConfigService() {
        return new nl.wantedchef.empirewand.api.impl.ConfigServiceAdapter(plugin.getConfigService());
    }

    @Override
    public nl.wantedchef.empirewand.api.service.CooldownService getCooldownService() {
        return new nl.wantedchef.empirewand.api.impl.CooldownServiceAdapter(plugin.getCooldownManager());
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
    public nl.wantedchef.empirewand.api.spell.toggle.SpellManager getSpellManager() {
        return plugin.getSpellManager();
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
