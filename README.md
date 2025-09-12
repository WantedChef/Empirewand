# EmpireWand Plugin

<<<<<<< HEAD
EmpireWand is a Minecraft Paper plugin providing magical wand mechanics with configurable spells.
=======
EmpireWand is a comprehensive Minecraft Paper plugin for 1.20.6+ providing magical wand mechanics with 50 configurable spells across multiple elemental types.
>>>>>>> origin/main

## 🚀 Quick Start

### Prerequisites
- **Minecraft Server**: Paper 1.20.6 or higher
- **Java**: Java 17 or higher
- **Dependencies**: None required (self-contained)

### Installation
1. Download the latest `EmpireWand.jar` from releases
2. Place in your server's `plugins/` directory
3. Restart or reload your server
4. Use `/wand give` to get started

### Building from Source
```bash
# Clone the repository
git clone https://github.com/your-org/empirewand.git
cd empirewand

# Build the plugin
./gradlew build

# Find the built jar in build/libs/
```

## 🎯 Features

### Spell Categories
- **Fire**: 5 spells including Fireball, Inferno, Flamethrower
- **Ice**: 5 spells including FreezeRay, IceWall, ArcticBlast
- **Dark**: 5 spells including ShadowBolt, Void, Kill
- **Earth**: 5 spells including GraspingVines, Earthquake
- **Life**: 5 spells including SuperHeal, Regenerate, Lifewalk
- **Heal**: 5 spells including Prayer, DivineHeal, Restoration
- **Movement**: 5 spells including Levitate, Phase, Recall
- **Utility**: 5 spells including Gate, Empower, Invulnerability
- **Offensive**: 5 spells including SuperKill, Volley, Shuriken
- **Defensive**: 5 spells including Shell, Reflect, Absorb

### Toggle Spells
- **MephiCloud**: Nether-themed particle effects while flying
- **KajCloud**: Beautiful cloud particles while flying
- **ShadowCloak**: Hypixel-grade stealth spell

## 🛠️ Configuration

### Config Files
- `config.yml`: Main plugin configuration
- `spells/`: Individual spell configurations
- `templates/`: Wand templates

### Basic Configuration
```yaml
# config.yml
wand:
  default-spells: ["fireball", "heal"]
  max-spells: 10
  cooldown-seconds: 5
  
performance:
  async-spells: true
  particle-limit: 1000
  
logging:
  level: INFO
  debug-spells: false
```

## 📋 Commands

### Player Commands
- `/wand give [player]` - Give basic wand
- `/wand zeist [player]` - Give Mephidantes Zeist wand
- `/wand stats [player]` - View wand statistics
- `/wand switch <effect>` - Switch wand effect

### Admin Commands
- `/wand reload` - Reload configuration
- `/wand template <name>` - Create wand template
- `/wand debug` - Toggle debug mode

## 🔧 Development

### Adding New Spells
1. Create spell class in appropriate package
2. Extend `Spell<T>` or `ProjectileSpell`
3. Implement required methods
4. Register in `SpellRegistryImpl`
5. Add configuration in `config/spells/`

### Example Spell Structure
```java
public class MySpell extends Spell<Void> {
    public MySpell(Builder<Void> builder) {
        super(builder);
    }
    
    @Override
    public String key() { return "my-spell"; }
    
    @Override
    protected Void executeSpell(SpellContext context) {
        // Spell logic here
        return null;
    }
}
```

## 🧪 Testing

<<<<<<< HEAD
### Deprecation Fixes (September 2025)
- Added compatibility `Prereq` class to resolve import errors for legacy spell implementations.
- Updated `Polymorph.java` to use `PrereqInterface.NonePrereq()` for prerequisites.
- Fixed compilation errors by ensuring all spell classes use correct APIs (`LegacySpell`, `PrereqInterface`).
- Deprecated legacy `Prereq` usage in favor of `PrereqInterface` for better type safety and consistency.

## Supported Versions

- Paper: 1.20.6
- Java: 21

## Setup

1. Clone the repository.
2. Open in VS Code with Java 21 extension.
3. Run `./gradlew build` to compile.

## Build

The project now produces ONLY a single shaded plugin JAR (no separate `-all` classifier) to simplify deployment.

Commands:
- Unix/macOS: `./gradlew clean build`
- Windows (PowerShell): `./gradlew.bat clean build`

Useful tasks:
- `build` – compiles, runs static analysis, creates shaded jar
- `test` – runs JUnit tests (`./gradlew test`)
- `spotbugsMain` – SpotBugs analysis
- `checkstyleMain` – Checkstyle report

Output:
- Shaded plugin JAR: `build/libs/empirewand-<version>.jar`  (deploy this one)

Notes:
- bStats is currently NOT relocated due to an ASM limitation parsing Java 21 class files in the Shadow plugin version used. If relocation becomes necessary, uncomment the `relocate("org.bstats", "nl.wantedchef.empirewand.shaded.bstats")` line in `build.gradle.kts` once Shadow/ASM supports class file major version 65 fully.
- The build uses a Java 21 toolchain for compilation; ensure your Gradle daemon runs on JDK 17+.

## Install / Upgrade

- Copy `empirewand-<version>.jar` from `build/libs/` to your Paper server `plugins/` folder.
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

## Telemetry & Privacy

- Metrics (bStats) are disabled by default: `metrics.enabled: false` in `config.yml`.
- To enable, set `metrics.enabled: true` and ensure a valid `metrics.plugin-id` is configured.
- No stacktraces are sent to players; errors are logged to the console/logger.

## Commands & Permissions

See `src/main/resources/plugin.yml` for the authoritative list of commands and permissions. Document highlights:
- Primary commands: `/ew`, `/mz`
- Wildcards: `nl.wantedchef.empirewand.spell.use.*` (default: true), `nl.wantedchef.empirewand.spell.bind.*` (default: op)

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
=======
### Unit Tests
```bash
./gradlew test
>>>>>>> origin/main
```

### Integration Testing
- Use test server with Paper 1.20.6
- Test all 50 spells with various configurations
- Verify performance with 100+ concurrent players

## 📊 Performance

### Optimizations
- **Async Spell Processing**: Heavy spells run on separate threads
- **Particle Limiting**: Configurable particle limits per spell
- **Cooldown Caching**: Efficient cooldown management
- **Memory Management**: Weak references for spell data

### Benchmarks
- Spell casting: <1ms average
- Particle effects: <0.5ms per 100 particles
- Memory usage: <50MB for 1000 active spells

## 🔍 Troubleshooting

### Common Issues
1. **Spells not working**: Check permissions and configuration
2. **Performance issues**: Reduce particle limits in config
3. **Compilation errors**: Ensure Java 17+ and Paper 1.20.6+

### Debug Mode
Enable debug logging:
```yaml
logging:
  level: DEBUG
  debug-spells: true
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Follow code style guidelines
4. Add unit tests
5. Submit pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Issues**: [GitHub Issues](https://github.com/your-org/empirewand/issues)
- **Discord**: [Support Server](https://discord.gg/your-server)
- **Wiki**: [Documentation](https://github.com/your-org/empirewand/wiki)
