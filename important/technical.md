EmpireWand – Technisch Ontwerp
Doel: concreet technisch overzicht van architectuur, data, event‑flows en configuratie voor implementatie en onderhoud.

**Stack & Build**
- Runtime: Java 21, Paper API 1.20.6.
- Build: Gradle Kotlin DSL; output `build/libs/empirewand-<ver>.jar`.
- Main class: `com.example.empirewand.EmpireWandPlugin`.
- Externe libs: Paper levert Adventure; MiniMessage en Cloud kunnen worden toegevoegd indien gewenst.

**Architectuur Overzicht**
- Core
  - `SpellRegistry`: registreert en levert `Spell` implementaties + config views.
  - `CooldownService`: cooldowns per speler + spell‑key, met tick‑granulariteit.
  - `WandData`: PDC‑helpers (flag, spell‑lijst, actieve index).
  - `FxService`: geconsolideerde particles, sounds, action bar.
  - `ConfigService`: laden/valideren `config.yml` en `spells.yml`, reload.
  - `PermissionService`: leesbare checks (commands/spells/bypass).
- Listeners
  - `WandInteractionListener`: R‑klik select, Sneak+R‑klik previous, L‑klik cast.
  - `ProjectileListener`: `ProjectileHitEvent` resolveert spell op basis van tag.
- Commands
  - `/ew get|bind|unbind|bindall|list|reload` via Paper of Cloud framework.

**Data Model (PDC)**
- Keys (`NamespacedKey("empirewand", key)`)
  - `wand.key`: marker dat item een wand is (String or byte flag).
  - `wand.spells`: lijst van spell‑keys (CSV of PersistentDataContainer list).
  - `wand.activeIndex`: huidige selectie (int).
  - `wand.version`: migratieversie (int), default 1.
- Projectiel‑tags
  - `proj.spell`: gekoppelde spell‑key (String).
  - `proj.owner`: caster UUID voor friendly‑fire checks.

**Spell API**
- Interface (indicatief)
  - `String key()` – unieke key (bv. `comet`).
  - `Component displayName()` – vanuit config.
  - `Prereq prereq()` – checks/reden bij fail.
  - `void execute(SpellContext ctx)` – voert effect uit.
- `SpellContext`
  - `Player caster`, `Location origin`, `ConfigView config`, `FxService fx`, helpers.
  - Methodes: `raycast(range)`, `nearestTarget()`, `schedule(delay, task)`.

**Event‑Flows**
- Selecteren (R‑klik / Sneak+R‑klik)
  - Validate wand → update index → action bar met display‑name → select sound.
- Casten (L‑klik)
  - Lees key → permissie/use check → cooldown check → prereq check → execute → cooldown zetten → cast sound.
- Projectiel Impact
  - On launch: tag PDC (`proj.spell`, `proj.owner`).
  - On hit: resolve key → effect (damage/AoE/slow) → FX → cleanup.

**Configuratie**
- `config.yml`
  - `messages.*` – action bar/foutmeldingen templates (MiniMessage toegestaan).
  - `features.block-damage`, `features.friendly-fire` – toggles.
  - `cooldowns.default`, overrides per categorie indien gewenst.
- `spells.yml` (per key)
  - `display-name`, `cooldown`, `values.*` (schade, radius, duur), `flags.*` (los, sets‑fire, hit‑players, hit‑mobs).

**plugin.yml (voorbeeld)**
```yaml
name: ${name}
version: ${version}
main: com.example.empirewand.EmpireWandPlugin
api-version: 1.20
commands:
  ew:
    description: EmpireWand commands
    usage: /ew <get|bind|unbind|bindall|list|reload>
permissions:
  empirewand.command.get: { default: op }
  empirewand.command.bind: { default: op }
  empirewand.command.unbind: { default: op }
  empirewand.command.bindall: { default: op }
  empirewand.command.reload: { default: op }
  empirewand.spell.use.*: { default: true }
  empirewand.spell.bind.*: { default: op }
```

**Belangrijke Conventies**
- PDC‑waarden serialiseren via `PersistentDataContainer` (geen NBT hacks).
- Alle spell‑keys in `kebab-case` (bv. `magic-missile` of compact `magicmissile` consistent).
- FX intensiteit afhankelijk van client performance; bied globale scaler in config.

**Veiligheid & Compatibiliteit**
- Region/PVP respecteren: bij onduidelijkheid niet uitvoeren.
- Friendly‑fire: check `proj.owner` vs. victim + config toggle.
- Annuleer effecten bij dimension change/log out.

**Testen**
- Unit: `WandData` serialization/deserialization, cooldown logic edge cases.
- Integratie: interacties, cast op water/lucht/wall, LOS, undead edge cases.
- Soak: 10 min intensief casten met 5 spelers → geen memory leak/tick spikes.

**Uitbreiding (na v1)**
- Mana/resource system, progression, nieuwe wands (MephiWand, BloodWand).
- Integraties: WorldGuard, PlaceholderAPI, bStats metrics.

