# EmpireWand Plugin

EmpireWand is a Minecraft Paper plugin for 1.20.6 providing magical wand mechanics with configurable spells.

## Setup

1. Clone the repository.
2. Open in VS Code with Java 21 extension.
3. Run `./gradlew build` to compile.

## Build

- `./gradlew clean build` - Builds the JAR in `build/libs/empirewand-1.1.0.jar`.
- `./gradlew test` - Runs JUnit tests.
- `./gradlew spotbugsMain` - Static analysis.

## Run

- Copy JAR to Paper server `plugins/` folder.
- Start server: `java -jar paper-1.20.6.jar`.
- Use `/ew get` to get a wand.
- Bind spells: `/ew bind <spell-key>`.
- Right-click to cycle, left-click to cast.

## Spells

See `spells.yml` for configuration. Examples: `leap`, `comet`, `void-swap`, `life-reap`.

Best practices:
- Use kebab-case keys (e.g., `glacial-spike`) and mirror with class names in `spell/implementation` (e.g., `GlacialSpike`).
- Put all gameplay values in `spells.yml` or `config.yml` (no hardcoded values). Access via `ConfigService`.
- For projectile spells, prefer the hybrid routing:
  - Implement `ProjectileSpell` for new/complex spells and rely on `ProjectileListener`.
  - Keep `EntityListener` for cross-spell events (e.g., ethereal fall cancel) and legacy simple paths.
- For visual trails, use `FxService` helpers (`followParticles`, `followTrail`) to avoid duplicate schedulers.
- Respect permissions: `empirewand.spell.use.<key>` (use) and `empirewand.spell.bind.<key>` (bind).
- Guard early in listeners and keep event paths light (no blocking I/O, keep allocations low).

Production notes:
- Metrics: until a definitive bStats plugin ID is configured, set `metrics.enabled: false` on production servers. Keep it enabled during development for local insights.

## API Usage

EmpireWand provides a comprehensive API for integrating with other plugins. All services are accessed through the `EmpireWandAPI` provider.

### Getting Started

```java
// Get the API provider
EmpireWandAPI.EmpireWandProvider provider = EmpireWandAPI.getProvider();

// Access services
EffectService effects = provider.getEffectService();
CooldownService cooldowns = provider.getCooldownService();
ConfigService config = provider.getConfigService();
MetricsService metrics = provider.getMetricsService();
SpellRegistry spells = provider.getSpellRegistry();
```

### Effect Service

Create visual and audio effects:

```java
// Display messages
effects.actionBar(player, "Spell ready!");
effects.title(player, Component.text("Fireball!"), Component.text("Cast successful"), 10, 40, 10);

// Play sounds
effects.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

// Create particles
effects.spawnParticles(location, Particle.FLAME, 20, 0.5, 0.5, 0.5, 0.1);

// Batch particles for performance
effects.batchParticles(location, Particle.SMOKE, 10, 0.1, 0.1, 0.1, 0.05);
effects.flushParticleBatch();
```

### Cooldown Service

Manage spell cooldowns:

```java
// Check cooldowns
boolean onCooldown = cooldowns.isOnCooldown(playerId, "fireball", currentTicks);

// Set cooldowns
cooldowns.set(playerId, "fireball", currentTicks + 100); // 5 seconds

// Manage per-wand cooldown disabling
cooldowns.setCooldownDisabled(playerId, wand, true);
boolean disabled = cooldowns.isCooldownDisabled(playerId, wand);
```

### Configuration Service

Access plugin configuration:

```java
// Get configuration sections
ReadableConfig mainConfig = config.getMainConfig();
ReadableConfig spellsConfig = config.getSpellsConfig();

// Read values
boolean debug = mainConfig.getBoolean("debug", false);
int cooldown = mainConfig.getInt("cooldowns.default", 100);
String message = mainConfig.getString("messages.welcome", "Welcome!");
```

### Metrics Service

Track plugin usage and performance:

```java
// Record events
metrics.recordSpellCast("fireball");
metrics.recordSpellCast("lightning", 150); // with duration
metrics.recordFailedCast();
metrics.recordWandCreated();

// Get statistics
long totalCasts = metrics.getTotalSpellCasts();
double successRate = metrics.getSpellCastSuccessRate();
String debugInfo = metrics.getDebugInfo();
```

### Spell Registry

Access and manage spells:

```java
// Get spells
Optional<Spell> spell = spells.getSpell("fireball");
Set<String> spellKeys = spells.getSpellKeys();

// Advanced querying
SpellQuery query = spells.createQuery()
    .category("fire")
    .maxCooldown(200)
    .enabled(true)
    .build();
List<Spell> fireSpells = spells.findSpells(query);
```

## Development

Follow AGENTS.md guidelines: Java 21, 4-space indent, no wildcard imports. Tests in `src/test/java`.

## License

MIT License.
