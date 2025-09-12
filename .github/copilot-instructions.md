# Copilot Instructions: EmpireWand

Concise, project-specific knowledge so an AI agent can be productive immediately. Follow these patterns; do not introduce new architectural styles.

## ⚙️ Overview
Minecraft Paper plugin (Java 21) implementing a configurable spell & wand system. Entry point: `EmpireWandPlugin` (`src/main/java/.../EmpireWandPlugin.java`). Core domains: spells, wands, commands, visual FX, cooldowns, metrics.

## 🧱 Architecture
- Lifecycle: `onEnable()` wires services (config, text, performance monitor, metrics, spell registry, wand service, permission, FX) then registers listeners & commands.
- Services live under `framework/service/*` (e.g. `SpellRegistryImpl`, `CooldownService`, `FxService`, `WandServiceImpl`). API adapters exposed through `EmpireWandAPI.setProvider()`.
- Spell system: abstract `Spell<T>` + concrete packages by category (`spell/fire`, `spell/dark`, etc). Projectile variants extend `ProjectileSpell` for homing/trails & hit dispatch via global listener.
- Registry: `SpellRegistryImpl` eagerly registers all spells via builder suppliers; uses multiple caches (display name, categories, metadata) + `PerformanceMonitor` timings.
- Config access: `ConfigService` yields `ReadableConfig` pulled from `config.yml`/`spells.yml`; each spell optionally loads its own section via `loadConfig` during registry registration.
- Task safety: Long/async logic uses `requiresAsyncExecution()` → async run then schedules `handleEffect` on main thread and registers with `TaskManager` to avoid orphan tasks.

## 🔑 Key Services (access through `EmpireWandAPI.getProvider()`)
- `SpellRegistry` (query, metadata, projectile hit routing, toggle support)
- `WandService` (binding, selection state, PDC-backed storage)
- `CooldownService` (per-player spell throttling; some TODO placeholders remain inside `Spell.cast()` for future integration)
- `FxService` (particles, titles, batching)
- `MetricsService` + `DebugMetricsService` (bStats + internal sampling)
- `PermissionService` (string construction patterns; defensive null checks)

## 🪄 Spell Pattern
Minimal requirements:
1. Extend `Spell<T>` (or `ProjectileSpell<P>`). Implement: `key()`, `prereq()`, `executeSpell()`, `handleEffect()`.
2. Provide inner static `Builder` extending `Spell.Builder<T>`; builders accept nullable API (pass `null` consistently like registry does) and set: `name`, `description`, `cooldown`, `type`.
3. Use config-driven values: retrieve from `spellConfig` (set in `loadConfig`). Never hardcode gameplay numbers.
4. Async: override `requiresAsyncExecution()` only for heavy computation; avoid Bukkit API calls off main thread.
5. Projectile: set projectile class + optional homing/trail/hit sound in `ProjectileSpell.Builder`.

Example reference files: `spell/ProjectileSpell.java`, `spell/Spell.java`, `spell/fire/Fireball.java`.

## 🧪 Commands & Permissions
- Commands declared in `plugin.yml`: `/ew`, `/mz` multiplex subcommands (bind, bindall, bindcat, bindtype, set-spell, list, reload, migrate, spells, cd).
- Permission naming: `empirewand.spell.use.<key>`, `empirewand.spell.bind.<key>`, wildcards: `nl.wantedchef.empirewand.spell.use.*`.
- Use `PermissionServiceImpl` methods when constructing checks; do not inline mismatched prefixes.

## 📂 Configuration Conventions
- `config.yml`: global messages, categories (logical binding groups), defaults (`cooldowns.default` ms), metrics flags.
- `spells.yml`: per-spell sections under `spells:` with: `display-name`, `description`, `type`, `cooldown` (milliseconds), nested `values` & optional `flags` / cosmetic fx blocks.
- Categories in `config.yml` drive `/ew bindcat`—keys must match `spells.yml` entries.

## 🧵 Threading & Tasks
- Never call Bukkit world/entity mutations inside `executeSpell()` if async; defer to `handleEffect`.
- When scheduling manual tasks, register them through `EmpireWandPlugin.getTaskManager()` to ensure shutdown cleanup.

## 🧯 Performance & Caching
- Registry caches: invalidate via `registerSpell`/`unregisterSpell` mutations only. Avoid adding redundant caching inside spells (already short-lived operations).
- Particle heavy effects: prefer `FxService` batching helpers instead of ad-hoc per-tick runnables.

## 🛠 Build & Tests
- Build: `./gradlew.bat clean build` (Windows) produces shaded jar at `build/libs/empirewand-<version>-all.jar` (shadow disables plain jar).
- Static analysis: Checkstyle (0 warnings policy), SpotBugs (HIGH), Jacoco (temporary low threshold 0.02 instruction ratio).
- Tests: JUnit 5 + Mockito; enable ByteBuddy experimental flag already set in `build.gradle.kts`.

## ⚠️ Gotchas
- Builders use nullable API; don’t reintroduce `EmpireWandAPI.get()` inside constructors—access services through `SpellContext` if needed.
- Cooldown enforcement placeholder in `Spell.cast()`—do not implement ad-hoc cooldown logic per spell (centralize later in service integration point).
- Projectile hit handling relies on PDC markers (`Keys.PROJECTILE_SPELL`) + global listener; don’t duplicate per-spell listeners.
- Always guard against null & empty strings in permission/string helpers (see defensive style in `PermissionServiceImpl`).
- Shutdown: avoid lingering async runnables—use TaskManager or they won’t be tracked/cancelled.

## ✅ When Adding Code
- Follow 4-space indent; no wildcard imports (Checkstyle enforced).
- Put gameplay numbers & toggles in YAML; expose via `values` or `flags`.
- Reuse existing enums / types (`SpellType`) rather than creating parallel categorization.

Request clarification if introducing: new resource cost systems, dynamic spell registration APIs, or cross-plugin event contracts (not presently supported).
