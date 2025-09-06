# EmpireWand Plugin

EmpireWand is a Minecraft Paper plugin for 1.20.6 providing magical wand mechanics with configurable spells.

## Setup

1. Clone the repository.
2. Open in VS Code with Java 21 extension.
3. Run `./gradlew build` to compile.

## Build

- `./gradlew clean build` - Builds the JAR in `build/libs/empirewand-1.0.0.jar`.
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

## Development

Follow AGENTS.md guidelines: Java 21, 4-space indent, no wildcard imports. Tests in `src/test/java`.

## License

MIT License.
