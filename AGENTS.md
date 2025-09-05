# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/`: Java sources (Paper plugin). Key packages:
  - `com.example.empirewand`: plugin entry (`EmpireWandPlugin`), command, listeners.
  - `core`: services (config, permissions, cooldowns, registry, FX, keys).
  - `spell`: base types and `spell/implementation` for concrete spells (e.g., `MagicMissile`).
- `src/main/resources/`: plugin assets (`plugin.yml`, `config.yml`, `spells.yml`).
- Build metadata: `build.gradle.kts`, `settings.gradle.kts` (project `empirewand`).
- Docs: `README.md`, `EmpireWand_Structure.md`, `important/` notes.

## Build, Test, and Development Commands
- `./gradlew build`: Compiles with Java 21, processes resources, creates JAR in `build/libs/empirewand-1.0.0.jar`.
- `./gradlew test`: Runs JUnit 5 tests (none by default).
- `./gradlew clean`: Cleans build outputs.
- Run in server: copy the JAR to your Paper server `plugins/` and start the server.

## Coding Style & Naming Conventions
- Language: Java 21, UTF‑8. Prefer 4‑space indentation.
- Packages: `com.example.empirewand[..]`.
- Classes/Interfaces: UpperCamelCase (`WandData`, `SpellRegistry`).
- Methods/fields: lowerCamelCase; constants UPPER_SNAKE_CASE.
- Spells: one class per file in `spell/implementation` (`GlacialSpike.java`).
- Plugin YAML: keep keys lower-case; version/name expanded from Gradle.

## Testing Guidelines
- Framework: JUnit 5 (configured). Place tests in `src/test/java` mirroring package structure.
- Naming: `ClassNameTest` (unit), `…IT` (integration if applicable).
- Aim for tests on services (`core/*`) and spell behaviors with deterministic logic.
- Run with `./gradlew test`; add assertions for cooldowns, permissions, and registry wiring.

## Commit & Pull Request Guidelines
- Use Conventional Commits: `feat: add GlacialSpike spell`, `fix: correct cooldown check`.
- Keep commits small and scoped; reference issues (`#123`) when relevant.
- PRs: include summary, rationale, screenshots/logs if behavior changes, test coverage notes, and migration notes for configs if needed.

## Security & Configuration Tips
- Do not hardcode server-specific values; use `config.yml` and `spells.yml`.
- Access config via `ConfigService`; permissions via `PermissionService`; keys via `Keys`.
- Validate user inputs in commands/listeners; respect cooldowns and permission checks.

## Architecture Overview
- Entry: `EmpireWandPlugin` registers commands, listeners, and services.
- Services: encapsulate reusable logic (config, permissions, FX, cooldowns, registry).
- Spells: implement `Spell` with behavior via `SpellContext`.
