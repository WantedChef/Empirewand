# EmpireWand Plugin

EmpireWand is a comprehensive Minecraft Paper plugin for 1.20.6+ providing magical wand mechanics with 50+ configurable spells across multiple elemental types.

## 🚀 Quick Start

### Prerequisites
- **Minecraft Server**: Paper 1.20.6 or higher
- **Java**: Java 21 or higher (Java 17+ for Gradle daemon)
- **Dependencies**: None required (self-contained with shaded dependencies)

### Installation
1. Download the latest `empirewand-<version>-all.jar` from releases or build from source
2. Place in your server's `plugins/` directory
3. Restart or reload your server
4. Use `/ew get` to get a wand and start using spells

### Building from Source
```bash
# Clone the repository
git clone https://github.com/your-org/empirewand.git
cd empirewand

# Build the plugin (Unix/macOS)
./gradlew clean build

# Build the plugin (Windows PowerShell)
./gradlew.bat clean build

# Find the built jar in build/libs/
# Deploy: empirewand-<version>-all.jar (shaded plugin JAR)
```

## 🎯 Features

### Spell Categories
Over 50 spells across multiple categories:
- **Fire**: Fireball, Inferno, Flame Wave, Blaze Launch
- **Ice**: Glacial Spike, Frost Nova, Arctic effects
- **Lightning**: Chain Lightning, Lightning Storm, Thunder Blast, Little Spark
- **Dark**: Shadow spells, Void effects, Soul magic
- **Earth**: Grasping Vines, Earthquake, Sandstorm
- **Life**: Healing spells, Lifesteal, Life Reap
- **Movement**: Leap, Teleport, Blink Strike, Phase abilities
- **Utility**: Polymorph, Ethereal Form, Stasis Field
- **Projectile**: Magic Missile, Comet, Arcane Orb
- **Area Effects**: Explosion Wave, Poison Wave, Radiant Beacon

### Toggle Spells & Movement Enhancements
- **Angel Wings, Dragon Fury, Phoenix Rise**: Enhanced flight abilities
- **Storm Rider, Void Walk, Crystal Glide**: Advanced movement spells
- **Shadow Cloak**: Hypixel-grade stealth spell
- **Kaj Cloud, Mephi Cloud**: Particle effect clouds while flying

## 🛠️ Configuration

### Config Files
- `config.yml`: Main plugin configuration
- `spells.yml`: Individual spell configurations with damage, cooldowns, costs
- Plugin creates default configurations on first run

### Basic Configuration Example
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

metrics:
  enabled: false  # bStats metrics disabled by default
  plugin-id: 12345  # Configure if enabling metrics
```

## 📋 Commands

### Primary Commands
- `/ew get [player]` - Get a basic wand
- `/ew bind <spell-key>` - Bind a spell to your wand
- `/ew unbind <slot>` - Unbind a spell from slot
- `/ew list` - List available spells
- `/ew spells` - View spell information
- `/ew stats [player]` - View wand statistics
- `/ew switcheffect <effect>` - Switch wand visual effects
- `/ew reload` - Reload configuration (requires op)

### MephidantesZeist Alias
- `/mz <command>` - Alternative command alias with same functionality

### Usage
- **Right-click**: Cycle through bound spells
- **Left-click**: Cast current spell
- **Crouch + Right-click**: Additional spell interactions

## 🔧 Development

### Tech Stack
- **Language**: Java 21
- **Build Tool**: Gradle 8.x with Kotlin DSL
- **Framework**: Paper API 1.20.6-R0.1-SNAPSHOT
- **Dependencies**: bStats (metrics), JetBrains Annotations
- **Testing**: JUnit Jupiter 5.10.0, Mockito 5.9.0
- **Static Analysis**: Checkstyle, SpotBugs
- **Build**: Shadow plugin for shaded JAR creation

### Adding New Spells
1. Create spell class in appropriate package (e.g., `spell/fire/MyFireSpell.java`)
2. Extend `Spell<T>` or `ProjectileSpell` for projectile-based spells
3. Implement required methods (`key()`, `executeSpell()`)
4. Register in `SpellRegistryImpl`
5. Add configuration in `spells.yml`
6. Add permissions to `plugin.yml`

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
        // Spell logic here - use ConfigService for values
        return null;
    }
}
```

### Best Practices
- Use kebab-case keys (e.g., `glacial-spike`) matching class names (`GlacialSpike`)
- Put all gameplay values in `spells.yml` or `config.yml` (no hardcoded values)
- Access configuration via `ConfigService`
- For projectile spells, implement `ProjectileSpell` and use `ProjectileListener`
- Use `FxService` helpers for visual effects to avoid duplicate schedulers
- Respect permissions: `empirewand.spell.use.<key>` (use) and `empirewand.spell.bind.<key>` (bind)

## 🧪 Testing

### Running Tests
```bash
# Run all unit tests
./gradlew test

# Run with coverage report
./gradlew test jacocoTestReport

# Static analysis
./gradlew spotbugsMain checkstyleMain
```

### Test Structure
- Unit tests with JUnit Jupiter and Mockito
- Comprehensive test coverage for commands, core functionality, framework services, and spells
- 80% code coverage requirement
- Integration testing supported with test servers

### Build Tasks
- `build` – Compiles, runs static analysis, creates shaded jar
- `test` – Runs JUnit tests with coverage
- `spotbugsMain` – SpotBugs static analysis
- `checkstyleMain` – Code style verification
- `jacocoTestReport` – Generate coverage reports

## 📁 Project Structure

```
src/
├── main/
│   ├── java/nl/wantedchef/empirewand/
│   │   ├── EmpireWandPlugin.java          # Main plugin class
│   │   ├── api/                           # Public API interfaces
│   │   ├── command/                       # Command handlers
│   │   ├── core/                          # Core services
│   │   ├── framework/                     # Framework implementations
│   │   └── spell/                         # Spell implementations
│   │       ├── enhanced/                  # Enhanced spell variants
│   │       ├── toggle/                    # Toggle spells (movement, aura)
│   │       ├── fire/, ice/, lightning/    # Elemental spells
│   │       └── control/, utility/         # Special categories
│   └── resources/
│       ├── plugin.yml                     # Plugin metadata & permissions
│       ├── config.yml                     # Default configuration
│       └── spells.yml                     # Spell definitions & settings
└── test/                                  # Comprehensive test suite
    ├── java/nl/wantedchef/empirewand/
    │   ├── command/                       # Command tests
    │   ├── core/                          # Core service tests
    │   ├── framework/                     # Framework tests
    │   └── spell/                         # Spell tests
    └── resources/                         # Test resources
```

## 🔧 Scripts

### Gradle Scripts
- **Build**: `./gradlew clean build` (Unix/macOS) or `./gradlew.bat clean build` (Windows)
- **Test**: `./gradlew test`
- **Coverage**: `./gradlew jacocoTestReport`
- **Analysis**: `./gradlew spotbugsMain checkstyleMain`

### Development Scripts
- `clean_spells.py`: Python script for cleaning spell configurations

## 🌐 Environment Variables

### Runtime Environment
- **No environment variables required** - All configuration through YAML files
- Plugin creates default configurations automatically in server's `plugins/EmpireWand/` folder

### Build Environment
- **JAVA_HOME**: Should point to JDK 21+ installation
- **Gradle daemon**: Requires JDK 17+ minimum
- Build uses Java 21 toolchain for compilation

## 📊 Performance & Telemetry

### Performance Features
- **Async Spell Processing**: Heavy spells run on separate threads
- **Particle Limiting**: Configurable particle limits per spell (default: 1000)
- **Cooldown Caching**: Efficient cooldown management
- **Memory Management**: Weak references for spell data
- **ByteBuddy Optimization**: Java 21 class instrumentation support

### Metrics & Privacy
- **bStats metrics disabled by default** (`metrics.enabled: false`)
- No stacktraces sent to players; errors logged to console only
- To enable metrics: set `metrics.enabled: true` and configure `metrics.plugin-id`

### Performance Benchmarks
- Spell casting: <1ms average
- Particle effects: <0.5ms per 100 particles  
- Memory usage: <50MB for 1000 active spells

## 🛡️ Permissions

### Command Permissions
- Most commands default to `op` required
- Viewing commands (`list`, `spells`, `stats`) default to `true` (all players)
- Wildcard permissions available: `empirewand.*`

### Spell Permissions
- **Use spells**: `empirewand.spell.use.*` (default: `true`)
- **Bind spells**: `empirewand.spell.bind.*` (default: `op`)
- Individual spell permissions: `empirewand.spell.use.<spell-key>`

See `src/main/resources/plugin.yml` for the complete list of commands and permissions.

## 🔍 Troubleshooting

### Common Issues
1. **Spells not working**: Check permissions and spell configuration in `spells.yml`
2. **Performance issues**: Reduce `particle-limit` in config.yml
3. **Build errors**: Ensure Java 21+ and Gradle compatibility
4. **Missing spells**: Check `spells.yml` for proper spell definitions

### Debug Mode
Enable detailed logging:
```yaml
logging:
  level: DEBUG
  debug-spells: true
```

### Known Issues
- bStats relocation: Currently enabled with Shadow 9.1.0 (full Java 21 support)
- Java 21 requirement: Gradle daemon needs JDK 17+ minimum

## 📖 API Usage

EmpireWand provides a comprehensive API for integrating with other plugins. All services are accessed through the `EmpireWandAPI` provider.

### Getting Started
```java
// Get the API provider
EmpireWandAPI.EmpireWandProvider provider = EmpireWandAPI.getProvider();

// Access services
EffectService effects = provider.getEffectService();
CooldownService cooldowns = provider.getCooldownService();
ConfigService config = provider.getConfigService();
SpellRegistry spells = provider.getSpellRegistry();
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/my-feature`)
3. Follow code style guidelines (Checkstyle configuration in `config/checkstyle/`)
4. Add unit tests with 80% coverage requirement
5. Ensure all static analysis passes (SpotBugs, Checkstyle)
6. Submit pull request

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

**Copyright (c) 2025 WantedChef**

## 🆘 Support

- **Issues**: [GitHub Issues](https://github.com/your-org/empirewand/issues)
- **Documentation**: Check `src/main/resources/plugin.yml` for authoritative command and permission lists
- **Discord**: TODO - Add Discord server link if available
- **Wiki**: TODO - Add wiki link if available

## 📝 TODO Items

- TODO: Add environment variables section if any runtime variables are discovered
- TODO: Document specific server performance requirements for high player counts
- TODO: Add examples of custom spell configurations
- TODO: Document plugin integration examples for API usage
- TODO: Add Discord server link when available
- TODO: Add wiki documentation link when available

---

**Version**: 1.1.1 | **API Version**: 1.20 | **Author**: ChefWanted
