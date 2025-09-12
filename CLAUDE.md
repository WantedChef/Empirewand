# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

### Core Build Commands
- `./gradlew clean build` - Clean build with shaded JAR in `build/libs/empirewand-<version>-all.jar`
- `./gradlew.bat clean build` - Windows PowerShell build command
- `./gradlew test` - Run JUnit tests with JaCoCo coverage (requires Java 21 with ByteBuddy experimental flag)
- `./gradlew jacocoTestReport` - Generate test coverage report (80% minimum required)
- `./gradlew check` - Full validation including tests, coverage, checkstyle, and SpotBugs

### Quality Assurance
- `./gradlew checkstyleMain` - Run Checkstyle linting (no star imports, unused imports)
- `./gradlew spotbugsMain` - Run SpotBugs static analysis
- `./gradlew jacocoTestCoverageVerification` - Verify 80% test coverage threshold

### Testing
- `./gradlew test --tests "*.SpellTest"` - Run specific test pattern
- `./gradlew test --tests "nl.wantedchef.empirewand.spell.fire.FireballTest"` - Run single test class

## Architecture Overview

### Plugin Structure
EmpireWand is a Paper 1.20.6 plugin built on Java 21 with a comprehensive spell system architecture:

- **Main Plugin**: `EmpireWandPlugin.java` - Manages service lifecycle and dependency injection
- **API Layer**: Public API in `api/` package with service adapters for external plugin integration
- **Framework Layer**: Core services in `framework/service/` - spell registry, cooldowns, effects, permissions
- **Spell System**: Abstract `Spell<T>` base class with builder pattern, async execution support
- **Command System**: Hierarchical commands with `/ew` and `/mz` entry points

### Command Structure
- **Primary Commands**: `/ew` (main) and `/mz` (MephidantesZeist alias)
- **Subcommands**: get, bind, unbind, bindall, bindtype, bindcat, set-spell, list, reload, migrate, spells, toggle, stats, switcheffect, cd
- **Permission Patterns**: `empirewand.command.<subcommand>` and `mephidanteszeist.command.<subcommand>`

### Service Architecture
Services follow dependency injection pattern through `EmpireWandAPI.getProvider()`:

1. **ConfigService** - YAML configuration management with migration support
2. **SpellRegistry** - Dynamic spell registration and querying with type safety  
3. **CooldownService** - Per-player, per-spell cooldown tracking with wand-specific disabling
4. **WandService** - Wand creation, binding, and PDC-based persistence
5. **FxService** - Optimized particle/sound effects with batching
6. **MetricsService** - bStats integration and performance monitoring

### Spell System Design
- **Base Classes**: `Spell<T>` (abstract) with `LegacySpell` compatibility layer
- **Builder Pattern**: Fluent API for spell configuration with nullable API support
- **Async Support**: `requiresAsyncExecution()` for heavy computations off main thread
- **Type Safety**: Generic `<T>` for spell effects with `executeSpell()` → `handleEffect()`
- **Prerequisites**: `PrereqInterface` with composition support (level, items, etc.)
- **Projectiles**: `ProjectileSpell` extends `Spell` with collision detection and homing

### Package Structure
```
nl.wantedchef.empirewand/
├── api/                    # Public API for external plugins
├── command/               # Command hierarchy (/ew, /mz)
├── core/                  # Utilities, config, storage
├── framework/             # Core services and command framework
├── listener/              # Event handlers (wand, combat, player)
├── spell/                 # Spell implementations by category
└── common/visual/         # Shared visual effects (afterimages, particles)
```

## Development Guidelines

### Code Style
- Java 21 language features preferred
- 4-space indentation (no tabs)
- No wildcard imports (enforced by Checkstyle)
- Use `@NotNull/@Nullable` annotations consistently
- SpotBugs suppressions require `@SuppressFBWarnings` with justification

### Spell Development
1. **Extend `Spell<T>`** where `T` is your effect type (use `Void` if none)
2. **Use Builder Pattern** - implement nested `Builder` extending `Spell.Builder<T>`
3. **Configure via YAML** - access config through `spellConfig` field after `loadConfig()`
4. **Handle Prerequisites** - implement `prereq()` method, prefer composition over inheritance
5. **Async Guidelines** - override `requiresAsyncExecution()` for I/O or heavy computation

### Testing Requirements
- Unit tests in `src/test/java` mirroring source structure
- Mockito for Bukkit dependencies (use `MockBukkit` patterns)
- Minimum 80% code coverage enforced by JaCoCo
- Test naming: `[method]Should[expected]When[condition]`

### Configuration Management
- All gameplay values in `config.yml` or `spells.yml` - no hardcoded magic numbers
- Access via `ConfigService.getMainConfig()` or `.getSpellsConfig()`
- Support config migrations through `ConfigMigrationService`
- Validate required keys and provide sensible defaults

### Performance Considerations
- Use `FxService` helpers for particle effects to avoid scheduler proliferation
- Prefer `ConcurrentHashMap` for multi-threaded data structures  
- Async spell execution for file I/O, network calls, or heavy computation
- Profile with `PerformanceMonitor` - target <1ms execution for hot paths
- Register tasks through `EmpireWandPlugin.getTaskManager()` to ensure shutdown cleanup

### Threading & Task Safety
- Never call Bukkit world/entity mutations inside `executeSpell()` if async; defer to `handleEffect`
- Long/async logic uses `requiresAsyncExecution()` → async run then schedules `handleEffect` on main thread
- Use `TaskManager` registration to avoid orphan tasks during plugin shutdown

### API Usage
EmpireWand provides comprehensive plugin API:
```java
EmpireWandAPI.EmpireWandProvider provider = EmpireWandAPI.getProvider();
SpellRegistry spells = provider.getSpellRegistry();
CooldownService cooldowns = provider.getCooldownService();
// etc.
```

## Common Patterns

### Spell Implementation Template
```java
public class ExampleSpell extends Spell<ExampleEffect> {
    public ExampleSpell() {
        super(new Builder()
            .name("Example")
            .description("Example spell")
            .cooldown(Duration.ofSeconds(5))
            .type(SpellType.UTILITY)
            .build());
    }
    
    @Override
    public String key() { return "example"; }
    
    @Override
    public PrereqInterface prereq() { 
        return PrereqInterface.NonePrereq();
    }
    
    @Override
    protected ExampleEffect executeSpell(SpellContext context) {
        // Spell logic here
        return new ExampleEffect(data);
    }
    
    @Override
    protected void handleEffect(SpellContext context, ExampleEffect effect) {
        // Handle on main thread
    }
}
```

### Permission Patterns
- Use permissions: `nl.wantedchef.empirewand.spell.use.<key>` (casting)
- Bind permissions: `nl.wantedchef.empirewand.spell.bind.<key>` (binding to wands)
- Check via `PermissionService` or standard Bukkit API

### Error Handling
- Wrap Bukkit calls in try-catch blocks
- Log at appropriate levels (SEVERE for plugin-breaking, WARNING for spell failures)
- Use `CommandErrorHandler` for consistent command feedback
- Never send stack traces to players - log to console only
- Guard against null & empty strings in permission/string helpers (defensive programming style)

### Important Gotchas
- Builders use nullable API; don't reintroduce `EmpireWandAPI.get()` inside constructors
- Cooldown enforcement placeholder in `Spell.cast()` - centralize through service integration, not per-spell logic
- Projectile hit handling relies on PDC markers (`Keys.PROJECTILE_SPELL`) + global listener
- Coverage threshold temporarily set low (0.02 instruction ratio) - aim for 80% in production
- bStats currently NOT relocated due to ASM limitation with Java 21 class files

## Deployment

**Server JAR**: Use `empirewand-<version>-all.jar` (shaded) not the regular JAR
**Requirements**: Paper 1.20.6, Java 21
**Permissions**: See `plugin.yml` for complete permission tree with wildcards