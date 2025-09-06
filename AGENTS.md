# Repository Guidelines

## Project Structure
- `src/main/java/`: core sources.
  - `com.example.empirewand`: entrypoint (`EmpireWandPlugin`), command, listeners.
  - `core`: `ConfigService`, `CooldownService`, `FxService`, `PermissionService`, `SpellRegistry`, `WandData`, `Keys`.
  - `spell`: API + `spell/implementation` for concrete spells (e.g., `MagicMissile`).
- `src/main/resources/`: `plugin.yml`, `config.yml`, `spells.yml`.
- Build files: `build.gradle.kts`, `settings.gradle.kts`. Project name `empirewand`.

## Build, Test, Run
- `./gradlew build`: Java 21 compile, resources expand, JAR → `build/libs/empirewand-1.0.0.jar`.
- `./gradlew test`: JUnit 5 tests (add under `src/test/java`).
- Local run: copy JAR to your Paper 1.20.6 server `plugins/`, start server.
- CI: GitHub Actions builds on push/PR and uploads artifacts; releases on `v*` tags.

## Coding Style & Conventions
- Java 21, UTF‑8, 4‑space indent; no wildcard imports.
- Classes/Interfaces UpperCamelCase; methods/fields lowerCamelCase; constants UPPER_SNAKE_CASE.
- Packages under `com.example.empirewand…`.
- Spell keys in configs use kebab‑case (`glacial-spike`); Java class `GlacialSpike` in `spell/implementation`.
- Guard early in listeners; avoid sync I/O in event paths; keep allocations low.

## Testing Guidelines
- Framework: JUnit 5. Place tests mirroring packages.
- Focus: `core/*` services (cooldowns, config), registry lookups, deterministic spell logic.
- Naming: `ClassNameTest`. Run with `./gradlew test`.

## Commit & PR Guidelines
- Conventional Commits: `feat: add glacial-spike spell`, `fix: cooldown off-by-one`.
- Branches: `feature/spell-<id>`, `fix/<area>`, `docs/<topic>`.
- PRs must include: purpose, scope, screenshots/logs if behavior changes, config migration notes, and how to verify.
- CI must pass; no warnings introduced.

## Security, Config & Performance
- No hardcoded gameplay values; use `config.yml` / `spells.yml`. Read via `ConfigService`.
- Permissions via `PermissionService` (e.g., `empirewand.spell.use.<key>`).
- Respect cooldowns (`CooldownService`) and player state. Validate command inputs.
- Performance budget: sub‑millisecond per event on average; batch particles; reuse objects where reasonable.
