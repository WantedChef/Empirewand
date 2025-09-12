# EmpireWand Minecraft Plugin - Developer Context

## Project Overview

EmpireWand is a comprehensive Minecraft Paper plugin for 1.20.6+ that provides magical wand mechanics with 50 configurable spells across multiple elemental types. It's designed to be a self-contained plugin with no external dependencies beyond Paper API.

### Core Features

- **50+ Configurable Spells**: Spread across 10 categories (Fire, Ice, Dark, Earth, Life, Heal, Movement, Utility, Offensive, Defensive)
- **Toggle Spells**: Special persistent effects like MephiCloud, KajCloud, and ShadowCloak
- **Wand System**: Players use magical wands to cast spells with configurable properties
- **API**: Comprehensive API for integration with other plugins
- **Performance Optimized**: Async spell processing, particle limiting, and efficient cooldown management

### Technology Stack

- **Language**: Java 21
- **Build Tool**: Gradle with Kotlin DSL (build.gradle.kts)
- **Game Server**: Paper 1.20.6+
- **Dependencies**:
  - Paper API 1.20.6-R0.1-SNAPSHOT (compileOnly)
  - bStats Bukkit 3.0.2 (implementation)
  - JUnit Jupiter 5.10.0 (testImplementation)
  - Mockito Core 5.9.0 (testImplementation)
  - JetBrains Annotations 24.1.0 (compileOnly)
  - SpotBugs Annotations 4.8.6 (compileOnly)

## Building and Running

### Prerequisites

- Java 21 JDK
- Gradle (or use the included Gradle wrapper)

### Building

```bash
# Unix/macOS:
./gradlew clean build

# Windows (PowerShell):
.\gradlew.bat clean build
```

The build process includes:
- Compilation with Java 21 toolchain
- Static analysis (Checkstyle, SpotBugs)
- Unit testing
- Creation of a shaded plugin JAR

The final plugin JAR is located at `build/libs/empirewand-<version>.jar`.

### Testing

```bash
# Run unit tests
./gradlew test

# Run with coverage analysis
./gradlew jacocoTestReport
```

### Running

1. Copy the built JAR from `build/libs/` to your Paper server's `plugins/` directory
2. Start the server: `java -jar paper-1.20.6.jar`
3. Use in-game commands like `/ew get` to get started

## Development Conventions

### Code Structure

- **Main Package**: `nl.wantedchef.empirewand`
- **API Package**: `nl.wantedchef.empirewand.api` (Public API for other plugins)
- **Core Services**: `nl.wantedchef.empirewand.framework.service` (Internal service implementations)
- **Spells**: `nl.wantedchef.empirewand.spell` (Base spell classes and implementations)
- **Commands**: `nl.wantedchef.empirewand.command`
- **Listeners**: `nl.wantedchef.empirewand.listener`

### Spell System

Spells are the core functionality of EmpireWand. Each spell extends the abstract `Spell<T>` class.

#### Creating a New Spell

1. Create spell class in appropriate package under `nl.wantedchef.empirewand.spell`
2. Extend `Spell<T>` or `ProjectileSpell` (which extends `Spell<Void>`)
3. Implement required methods:
   - `key()`: Returns unique spell identifier
   - `prereq()`: Returns prerequisite checker
   - `executeSpell(SpellContext)`: Core spell logic
   - `handleEffect(SpellContext, T)`: Apply effects (called on main thread)
4. Register in `SpellRegistryImpl` (in `EmpireWandPlugin.onEnable()`)
5. Add configuration in `src/main/resources/config/spells.yml`

#### Spell Context

`SpellContext` provides spells with access to:
- Plugin instance
- Casting player
- Configuration service
- Effects service
- Target entity or location
- Spell key

### API Usage

EmpireWand provides a comprehensive API for integrating with other plugins. All services are accessed through the `EmpireWandAPI` provider.

#### Getting Started

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

### Configuration

Configuration files are located in `src/main/resources`:
- `config.yml`: Main plugin configuration
- `spells.yml`: Spell-specific configurations
- `plugin.yml`: Bukkit plugin metadata

All gameplay values should be in configuration files, accessed via `ConfigService`.

### Commands and Permissions

Primary commands:
- `/ew`: Main EmpireWand command
- `/mz`: MephidantesZeist alias command

Key permissions:
- `empirewand.spell.use.*` (default: true)
- `empirewand.spell.bind.*` (default: op)

See `src/main/resources/plugin.yml` for the complete list.

### Best Practices

1. **Use kebab-case keys** (e.g., `glacial-spike`) and mirror with class names in `spell/implementation` (e.g., `GlacialSpike`)
2. **Put all gameplay values in config files** (no hardcoded values). Access via `ConfigService`
3. **For projectile spells**, prefer the hybrid routing:
   - Implement `ProjectileSpell` for new/complex spells and rely on `ProjectileListener`
   - Keep `EntityListener` for cross-spell events and legacy simple paths
4. **For visual trails**, use `FxService` helpers (`followParticles`, `followTrail`) to avoid duplicate schedulers
5. **Respect permissions**: `empirewand.spell.use.<key>` (use) and `empirewand.spell.bind.<key>` (bind)
6. **Guard early in listeners** and keep event paths light (no blocking I/O, keep allocations low)