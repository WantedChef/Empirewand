EmpireWand - Roadmap
Doel: gefaseerde oplevering van een stabiele, uitbreidbare wand-plugin met uitstekende game feel. Deze roadmap bundelt prioriteiten, mijlpalen, risico's, acceptatiecriteria en deliverables. Richtlijnversies: Java 21, Paper 1.20.6.

Inhoud
1. Doelen & Principes
2. Fases en Mijlpalen (Dag 0—10)
3. Gedetailleerde Taakverdeling per Fase
4. Configuratie-schema's (YAML)
5. Permissions-matrix
6. Data Model (PDC)
7. Performance & Telemetrie
8. QA & Release-checklist
9. Post‑v1 Backlog

1) Doelen & Principes
- Stabiliteit eerst: elke fase levert een werkende build op.
- Modulair ontwerp: core kent geen concrete spells/wands, alles via registries/services.
- UX belangrijk: duidelijke feedback (action bar, geluid, particles) en snelle respons.
- Zero‑loss data: alle wanddata via PDC op het item; test over server restarts.
- Config‑gedreven balans: geen hard‑coded waarden, alles in YAML aanpasbaar.

2) Fases en Mijlpalen

Fase 0 – Bootstrap (Dag 0–1)
- Doel: basisproject dat start en skeleton services exposeert.
- Deliverables: gradle set‑up, `plugin.yml`, packages, serviceskeletons, listener-registraties.
- Succes: server start schoon; log toont geladen services/listeners.

Fase 1 – Core Loop (Dag 1–3)
- Doel: item verkrijgen → spells binden → selecteren → casten (minimaal 2 spells: `Leap`, `Comet`).
- Deliverables: `/ew get`, `/ew bind`, `/ew unbind`, select via R‑klik, cast via L‑klik.
- Succes: spells werken vloeiend; PDC data persistent; basis debounce/cooldown.

Fase 2 – Config & Registry (Dag 3–4)
- Doel: centrale configuratie en registraties voor spells en cooldowns.
- Deliverables: `config.yml`, `spells.yml`, `SpellRegistry`, `CooldownService`, `/ew reload`.
- Succes: balans zonder hercompilatie aanpasbaar; cooldowns consequent toegepast.

Fase 3 – Game Feel Pass (Dag 4–5)
- Doel: leesbare en responsieve feedback bij select/cast/fail.
- Deliverables: `FxService` met presets, action bar teksten, consistente sounds/particles.
- Succes: select/cast voelt snappy; duidelijke failure messaging (geen spam).

Fase 4 - Spell Set v1 (Dag 5-7)
- Doel: eerste volwaardige set combat/utility spells.
- Deliverables: `Explosive`, `MagicMissile`, `GlacialSpike`, `GraspingVines`, `Heal`, `LifeSteal`.
- Succes: 8+ spells stabiel; projectielen afgehandeld via `EntityListener`.

Fase 5 – Control & Special (Dag 7–8)
- Doel: crowd control en state‑changes correct en revertibel.
- Deliverables: `Polymorph`, `EtherealForm`, PVP/friendly‑fire toggles.
- Succes: states eindigen netjes; geen dupes/leaks of stuck spelers/mobs.

Fase 6 – Permissions & Hardening (Dag 8–9)
- Doel: misbruik voorkomen, randgevallen afdekken, performance borgen.
- Deliverables: permissie‑matrix, debouncers, PVP/region respect, logging/warnings.
- Succes: geen abuse‑paden; lage tick‑kosten; heldere fouten.

Fase 7 - Docs & Release (Dag 9-10)
- Doel: shipbare build met volledige documentatie en voorbeeldconfigs.
- Deliverables:
  - [x] `README`
  - [x] `guidelines.md` (en `AGENTS.md` toegevoegd)
  - [x] `technical.md`
  - [x] `stappenplan.md` (ook `1_stappenplan.md`)
  - [x] `roadmap.md`
  - [ ] changelog
- Succes: plug-and-play release inclusief configs en permissies.

3) Gedetailleerde Taakverdeling per Fase

Fase 0 – Bootstrap
- Project: controleer `build.gradle.kts` (Java 21), `settings.gradle.kts`.
- `plugin.yml`: main = `com.example.empirewand.EmpireWandPlugin` (aanwezig).
- Services: maak/valideer skeletons: `core/ConfigService.java`, `core/SpellRegistry.java`, `core/CooldownService.java`, `core/WandData.java`, `core/FxService.java`, `core/PermissionService.java`.
- Listeners: registreer in `EmpireWandPlugin` de `WandInteractionListener`; projectile hits worden afgehandeld in `EntityListener` (geen aparte `ProjectileListener` nodig).
- Logging: startup logregel met serverversie.
- Acceptatiecriteria (AC): plugin enablet zonder warnings/errors; klas‑exposure via getters in `EmpireWandPlugin`.

Fase 1 - Core Loop
- [x] Wand herkenning: PDC-sleutels (namespace `empirewand`) en item-criteria.
- [x] Commandos: `/ew get`, `/ew bind <spell>`, `/ew unbind <spell>`; [`/ew list` optioneel nog open, permissiechecks deels TODO].
- [x] Selecteren/casten: R-klik next, Sneak+R-klik previous, L-klik cast.
- [x] Debounce/cooldown: per-caster debouncer + cooldown via `CooldownService`.
- [x] Data: PDC-velden voor spell-lijst en actieve index.
 - AC: 2 spells (Leap/Comet) werken; herstart verliest geen data; geen dubbel cast binnen debounce.

Fase 2 – Config & Registry
- YAML: `config.yml` (messages, feature flags), `spells.yml` (per‑spell balans).
- Loader: `ConfigService` leest, valideert, en verschaft view‑objecten.
- Registry: `SpellRegistry` registreert/biedt lookup op key; detecteer ontbrekende config keys met warnings.
- Reload: `/ew reload` valideert, herlaadt configs, en meldt diffs/warnings in console.
- AC: run‑time balanswijziging zichtbaar; cooldowns consistent tussen restarts.
- Tests: ongeldige waarden fallbacken; reload behoudt PDC data.

Fase 3 – Game Feel Pass
- `FxService`: presets voor trails/impact FX; helper voor action bar + sounds.
- Failure UX: meld permissie/LOS/cooldown helder; geen chatspam.
- AC: alle select/cast flows hebben visuele/aurale feedback; SFX volumes consistent.
- Tests: in drukke omgeving (veel entities) blijft feedback leesbaar en performant.

Fase 4 – Spell Set v1
- Implementaties: maak packages per spell; projectielen taggen via PDC of metadata.
- Listener: `ProjectileListener` koppelt hit‑events terug naar juiste spellcontext.
- Balans: damage, radius, duur uit `spells.yml`; respecteer PVP/region flags.
- AC: 6 extra spells stabiel; AoE respecteert friend/foe; geen block damage tenzij geconfigureerd.
- Tests: beams vs projectielen, water/zwemmen, undead interacties (Lifesteal).

Fase 5 – Control & Special
- States: tijdelijke modificaties (collidable, silence) met nette revert.
- Beperkingen: disable attack tijdens `EtherealForm`; dispel‑regels voor `Polymorph`.
- AC: geen stuck states; effects eindigen altijd (ook bij logout/teleport/death).
- Tests: massaal gebruik in PVP; edge cases: dimension change, combat‑tag systemen.

Fase 6 – Permissions & Hardening
- Matrix: permissies per command/spell/use/bind; staff bypass (cooldown/debug) expliciet.
- Randgevallen: null targets, unloaded chunks, spectators, invuln frames.
- Performance: cap actieve projectielen/effecten per speler; batch particles.
- AC: geen abuse‑paden; TPS‑impact minimaal (< 5 ms piek per tick door plugin).
- Tests: soak test 1 uur zonder NPEs; log schoon.

Fase 7 – Docs & Release
- Docs: werk `guidelines.md`, `technical.md`, `stappenplan.md`, `spells.md` bij; changelog.
- Voorbeelden: voorbeeld `config.yml`/`spells.yml` met commentaar; permissie‑overzicht.
- Release: jar‑naamgeving, minimale Paper‑versie, installatie‑stappen.
- AC: nieuwe admin kan plugin configureren zonder code te lezen.

4) Configuratie‑schema's (YAML)

Voorbeeld `config.yml`
```yaml
messages:
  prefix: "<gradient:#8A2BE2:#00BFFF>[EmpireWand]</gradient> "
  select: "Actief: <aqua><spell></aqua>"
  no_permission: "<red>Je hebt hier geen permissie voor."
  on_cooldown: "<yellow>Even wachten... (<time>t)"
features:
  friendly_fire: false
  block_damage: false
  pvp_only: true
cooldowns:
  default: 20   # ticks
```

Voorbeeld `spells.yml`
```yaml
leap:
  display: "Leap"
  cooldown: 20
  values:
    power: 1.2
    verticalBoost: 0.6
comet:
  display: "Comet"
  cooldown: 60
  values:
    yield: 2.5
    incendiary: false
lifesteal:
  display: "Life Steal"
  cooldown: 80
  values:
    damage: 6.0
    heal_ratio: 0.5
```

Validatie‑regels
- Verplicht: `display`, `cooldown` (>= 0), `values` object per spell.
- Onbekende keys: log warning, negeer zonder crash.
- Reload: valideer en toon diffs in console.

5) Permissions‑matrix

Commands
- `empirewand.command.get`: gebruik `/ew get`
- `empirewand.command.bind`: gebruik `/ew bind <key>`
- `empirewand.command.unbind`: gebruik `/ew unbind <key>`
- `empirewand.command.bindall`: alle spells binden (admin)
- `empirewand.command.list`: lijst spells op huidige wand
- `empirewand.command.reload`: herlaad configs

Spells
- `empirewand.spell.<key>.use`: cast‑recht voor een spell
- `empirewand.spell.<key>.bind`: bind‑recht voor een spell

Bypass (staff)
- `empirewand.bypass.cooldown`
- `empirewand.bypass.pvp`

6) Data Model (PDC)

Namespace: `empirewand`
- `wand.version` (int): data versie voor migraties.
- `wand.spells` (String[]): lijst van spell‑keys.
- `wand.active_index` (int): actieve spell index.
- `proj.spell` (String): key op projectielen voor hit‑routing.

Migraties
- Bij mismatch `wand.version`: kopieer/transformeer velden en schrijf nieuwe versie.

7) Performance & Telemetrie
- Budget: plugin mag geen consistente spikes > 5 ms veroorzaken.
- Particles: batchen, max per tick; korte schedulers.
- Projectielen: cap per speler; cleanup bij logout/death.
- Telemetrie (optioneel): bStats + eenvoudige counters (casts/spell, cooldown hits, failures).

8) QA & Release‑checklist

Functioneel
- Get/Bind/Unbind/List/Reload werken met permissies en feedback.
- Selecteren en casten met debounce en cooldown.
- Spells respecteren PVP en region‑flags; geen blockdamage tenzij geconfigureerd.

Robuustheid
- Geen NPEs in 1 uur soak test; reload zonder memory leaks.
- Edge cases: dimensionchange, logout tijdens effect, death/respawn, unloaded chunks.

Documentatie
- `README` met installatie en config, permissies, lijst spells.
- Voorbeeldconfigs met comments, changelog up‑to‑date.

Release
- Build naam/versie klopt; minimale Paper‑versie vermeld; jar getest op schone server.

9) Post‑v1 Backlog (prioriteit ≈)
- Integraties: WorldGuard, PlaceholderAPI, bStats (hoog).
- Nieuwe spells‑pakket: defensief/control (middel).
- Mana/ressource‑systeem (laag, apart project).
- CI‑pipeline en basis unittests voor services (middel).
- Debug tools: `/ew debug` overlay voor FX/LOS (laag).

Bijlagen – Overzicht Deliverables
- Core: `SpellRegistry`, `CooldownService`, `WandData`, `FxService`, `ConfigService`, `PermissionService`.
- Commands: `/ew get|bind|unbind|bindall|list|reload`.
- Listeners: `WandInteractionListener`, `EntityListener`.
- Configs: `plugin.yml`, `config.yml`, `spells.yml` (met comments).
- Spells v1: Leap, Comet, Explosive, MagicMissile, GlacialSpike, GraspingVines, Heal, LifeSteal, Polymorph, EtherealForm.

Risico's & Mitigaties (samenvatting)
- Performance: te veel per‑tick taken → batch FX, cap projectielen/effecten.
- Data‑corruptie: PDC sleutelconflicten → vaste `NamespacedKey` conventies + `wand.version`.
- Abuse: AoE/friendly‑fire griefing → flags + permissies + cooldowns.
- Compat: serverversies → target Paper 1.20.6; feature‑detect voor hooks.

Metrieken (Succescriteria)
- Tick time: geen spikes > 5 ms door plugin.
- Crash‑vrij: 0 NPE's in logs na 1 uur intensief testen.
- UX: acties binnen 100 ms feedback in action bar/sounds.
- Config: balanswijziging via YAML werkt zonder restart.

<!-- Out-of-Scope section intentionally removed per request -->
