# Volledige Roadmap en 1-stappenplan

Doel: Gefaseerde oplevering van een stabiele, modulaire en uitbreidbare EmpireWand-plugin met uitstekende game feel. Richtlijnen: Java 21, Paper 1.20.6, Kyori Adventure, PDC-data op het item, config-gedreven balans. Elke fase levert een werkende build op.

Fase 0 - Bootstrap (Dag 0-1)
- Core skeleton: Gradle (Java 21), `plugin.yml`, hoofdklasse, logging bij startup.
- Services: skeletons voor `ConfigService`, `FxService`, `SpellRegistry`, `CooldownService`, `WandUtil`.
- Listeners/commands: registreer placeholders (`WandInteractionListener`, `ProjectileListener`, `/ew`).
- AC: Server start schoon, services geregistreerd, geen warnings.
- Deliverables: startbaar project, basis mappenstructuur, minimale command stub.

Fase 1 - Core Loop (Dag 1-3)
- Wand & PDC: EmpireWand-flag (`empirewand:wand=true`), lijst `empirewand:spells`, actief `empirewand:active`.
- Commands: `/ew empirewand` (get), `/ew bind`, `/ew unbind`, `/ew bindall`, contextuele tab-completion, permissies.
- Spells v0: `Leap` en `Comet` via `Spell`-interface + `SpellRegistry`; `CastContext` met services.
- Interacties: L-klik = cast, R-klik = volgende, Sneak+R = vorige; action bar feedback.
- Cooldowns: default + per-spell via `CooldownService`.
- AC: 2 spells werken; PDC-persist over restart; trade-proof; nette UX.
- Deliverables: werkende jar met 2 spells en kernloop.

Fase 2 - Config & Registry (Dag 3-4)
- Configs: `config.yml` (messages, features, cooldowns), `spells.yml` (balans per spell).
- Reload: `/ew reload` met validatie en console-warnings bij ontbrekende keys.
- Registry: declaratieve registratie van spells; duidelijke fouten bij onbekende ids.
- AC: run-time balanswijziging zichtbaar; cooldowns consistent; reload stabiel.
- Deliverables: resources + `ConfigService` uitbreidingen en registry-logging.

Fase 3 - Game Feel Pass (Dag 4-5)
- FX-presets: trails, impacts, success/fail feedback via `FxService`.
- Messaging: MiniMessage-only, consistente kleuren en korte teksten.
- Debounce: bescherming tegen dubbel-klik spam; nette rate limiting.
- AC: vlotte, responsieve feel; geen spam; duidelijke failures.
- Deliverables: preset-methodes in `FxService`, message-keys in `config.yml`.

Fase 4 - Spell Set v1 (Dag 5-7)
- Aanvullende spells: volgens `important/spells.md`: o.a. `Explosive`, `MagicMissile`, `GlacialSpike`, `GraspingVines`, `Heal`, `LifeSteal`.
- Projectielen: centrale `ProjectileListener` en tagging via PDC of metadata.
- Balans: per spell configurabel (damage, radius, duur, flags).
- AC: 8+ stabiele spells; geen memory-leaks; listeners netjes afgemeld.
- Deliverables: packages `spells.*`, config-secties, impact- en state-handling.

Fase 5 - Control & Special (Dag 7-8)
- Control-spells: `Polymorph`, `EtherealForm` met nette state-enter/exit.
- PVP/regions: toggles en respecteren van serverregels (soft hooks indien aanwezig).
- AC: geen stuck states; states eindigen voorspelbaar; geen abuse-paden.
- Deliverables: state-machine hulpfuncties, cleanup-routines, aanvullende config.

Fase 6 - Permissions & Hardening (Dag 8-9)
- Permissie-matrix: fijnmazige rechten per command/spell.
- Hardening: edge cases, null-safety, gamemode-restricties, world-checks.
- Performance: lage tick-kosten, minimale allocaties, cache waar zinvol.
- Telemetrie: opt-in debug-logging, counters per spell (lokaal).
- AC: geen console-errors na uur testen; TPS stabiel.
- Deliverables: permissies in `plugin.yml`, guard-clauses, lichte metrics.

Fase 7 - Docs & Release (Dag 9-10)
- Documentatie:
  - [x] ~~Guidelines toegevoegd (`AGENTS.md`).~~
  - [ ] README (installatie, gebruik)
  - [ ] Technische uitleg
  - [ ] Changelog
- Voorbeelden: voorbeeld `config.yml`/`spells.yml`, permissies-tabel.
- Release: semver-tagging, distributie-binary en verificatiestappen.
- AC: plug-and-play; setup < 5 min; heldere changelog.
- Deliverables: complete docs + release-artifact.

Acceptatiecriteria (globaal)
- Stabiliteit: elke fase shipt zonder errors/warnings op Paper 1.20.6.
- UX: action bar en SFX/particles voor select/cast/fail; geen spam.
- Data: alle wanddata via PDC, restart-safe en trade-safe.
- Modulariteit: core kent geen concrete spells; alles via `SpellRegistry` en context.
- Config: alle balans in YAML; reload werkt en valideert waarden.

Technische keuzes (samengevat)
- Java 21, Paper 1.20.6, Kyori Adventure/MiniMessage, PDC voor itemdata.
- Commands: Cloud Command Framework optioneel; anders nette abstrahering voor latere migratie.
- PDC-keys: `empirewand:wand`, `empirewand:spells` (lijst/JSON), `empirewand:active` (string id).
- Cooldowns: `cooldowns.default` + per-spell overrides; per speler/spell in memory.
- Veiligheid: permissies, gamemode/world checks, null-safety en guard-clauses.

Commando's en permissies
- [x] ~~`/ew empirewand`: geeft een lege EmpireWand - `empirewand.get`.~~
- [x] ~~`/ew bind <spellId>`: bind spell aan wand - `empirewand.bind`.~~
- [x] ~~`/ew unbind <spellId>`: verwijder spell van wand - `empirewand.unbind`.~~
- [x] ~~`/ew bindall`: bind alle spells - `empirewand.bindall`.~~
- [x] ~~`/ew reload`: herlaad configs - `empirewand.reload`.~~
- [ ] Tab-completion: bind toont beschikbare; unbind toont gebonden spells.

Data-model en utilities
- `Spell` interface: `id()`, `displayName()`, `cooldownKey()`, `cast(CastContext)`.
- `CastContext`: `Player caster`, locatie/richting, `ItemStack wand`, `FxService`, `ConfigService`.
- `SpellRegistry`: `register`, `byId`, `all` — registraties tijdens `onEnable`.
- `WandUtil`: PDC helpers (detectie, lees/schrijf, volgende/vorige), validatie.
- `CooldownService`: `tryAcquire(player, spell)`, resterende tijd formattering.

Configuratiebestanden (resources)
- `config.yml`
  - `messages.no-wand`, `messages.no-spells`, `messages.selected`, `messages.cooldown`.
  - `features.restrict-gamemodes`, `cooldowns.default` (bijv. 500ms).
- `spells.yml`
  - Per spell-id sectie: `cooldown`, `power/damage`, `radius/duration`, `particles`, `sound`, flags.

Events en logica
- `PlayerInteractEvent`: R-klik volgende, Sneak+R vorige, L-klik cast; PDC-checks; feedback via `FxService`.
- `ProjectileHitEvent`: impact-afhandeling voor projectielen (Comet/Missile); opruimen en FX.
- Edge-cases: teleport/world change — geen speciale handling nodig, PDC draagt data.

Test-scenario’s (manueel)
- Nieuwe wand krijgen; naam/lore/PDC check; geen cast zonder spells.
- Bind/unbind/bindall; tab-complete; permissies; feedback.
- Wisselen/casten; cooldowns en fail-meldingen.
- Server-restart en item trade; PDC-persist.
- Extra spells uit v1-set; impact en state-einde verifiëren.

Deliverables per fase (samenvatting)
- [x] ~~F0: skeleton + startup-clean~~
- [x] ~~F1: core loop + 2 spells~~
- [x] ~~F2: configs + reload + registry~~
- [ ] F3: game feel + FX presets
- [x] ~~F4: spell set v1 + projectile listener~~
- [x] ~~F5: control/special + state-machine~~
- [ ] F6: permissies/hardening/perf + metrics light
- [ ] F7: docs + release-artifact

Risico’s en mitigatie
- Data-corruptie in PDC: valideer/versieer PDC payload; safe-fallback bij parse errors.
- Abuse via spells: permissies en server-flag checks; cooldowns streng afdwingen.
- Performance: profielen met timings; minimaliseer allocaties in tick-gevoelige paden.
- Compat: test met populaire region/PvP plugins; bouw soft-deps in waar zinvol.

Backlog na v1
- Meerdere wandtypes (MephiWand/BloodWand) die dezelfde `Spell`-infra delen.
- GUI voor spell-selectie/binding (inventory menu).
- Resource-pack integratie (custom model data/icoontjes).
- Uitbreidbaar “spell pack” systeem (soft-deps/autodiscovery).

Fase 1 (uitgewerkt detail)
- Core/services: constructor-injectie in commands/listeners/spells; `ConfigService` en `FxService` centraal.
- Item & PDC: `empirewand:spells` (lijst/JSON), `empirewand:active` (string id).
- Commands `/ew`: `empirewand`, `bind`, `unbind`, `bindall` met permissies en tab-complete.
- Spells: `Comet` (projectiel + impact FX) en `Leap` (mobility) in eigen packages.
- Events: L-klik cast; R-klik volgende; Sneak+R vorige; feedback via action bar.
- Config: `config.yml` (messages/flags/cooldowns), `spells.yml` (per-spell params).
- FX/UX: Kyori Adventure/MiniMessage; `FxService` voor sounds/particles/actionbar.
- Build & test: Gradle jar; lokale Paper test; scenario’s incl. PDC-persist, trade, cooldown.

Checklist totaal
- [x] ~~Bootstrap schoon (F0)~~
- [x] ~~Core loop + 2 spells (F1)~~
- [x] ~~Config/registry/reload (F2)~~
- [ ] FX/game feel (F3)
- [x] ~~Spell set v1 (F4)~~
- [x] ~~Control & special (F5)~~
- [ ] Permissions/hardening/perf (F6)
- [ ] Docs & release (F7)

Permissions-matrix (detail)
- `empirewand.get`: toegang tot `/ew empirewand`.
- `empirewand.bind`: toegang tot `/ew bind <id>`.
- `empirewand.unbind`: toegang tot `/ew unbind <id>`.
- `empirewand.bindall`: toegang tot `/ew bindall`.
- `empirewand.reload`: toegang tot `/ew reload`.
- Optioneel per-spell rechten: `empirewand.spell.<id>` om cast toe te staan/te blokkeren op runtime.

Command-spec (detail)
- `/ew empirewand` — geeft EmpireWand item; zonder subargumenten. Foutmelding als inventory vol is.
- `/ew bind <spellId>` — voegt spell toe; melding als al aanwezig; tab-complete uit `SpellRegistry`.
- `/ew unbind <spellId>` — verwijdert spell; active wordt aangepast indien nodig.
- `/ew bindall` — voegt alle spells toe; sla duplicaten over.
- `/ew reload` — herlaadt `config.yml` en `spells.yml`; logt diffs/warnings in console.

Config-schema's (YAML voorbeelden)
 - [x] ~~Resources aanwezig in `src/main/resources/config.yml` en `src/main/resources/spells.yml`.~~
```yml
# config.yml
messages:
  no-wand: "<red>Houd een EmpireWand vast."
  no-spells: "<yellow>Geen spells gebonden. Gebruik /ew bind."
  selected: "<gray>Actief: <green><spell></green>"
  cooldown: "<yellow>Wacht <time>ms voor <spell>."
  bound: "<green><spell></green> toegevoegd."
  unbound: "<red><spell></red> verwijderd."
features:
  restrict-gamemodes: false
cooldowns:
  default: 500
```

```yml
# spells.yml
comet:
  cooldown: 1200
  speed: 1.1
  gravity: 0.01
  radius: 2.5
  damage: 6.0
  particles: "FLAME"
  sound: "ENTITY_GHAST_SHOOT"
leap:
  cooldown: 900
  forward: 1.0
  upward: 0.6
  particles: "CLOUD"
  sound: "ENTITY_ENDER_DRAGON_FLAP"
```

PDC-datamodel (versieerbaar)
- [x] ~~Keys en utils aanwezig (`core/Keys.java`, `core/WandData.java`).~~
- `empirewand:wand` (byte) — flag = 1.
- `empirewand:spells` (string) — JSON array van spell-id’s, bv. `"[\"comet\",\"leap\"]"` of alternatieve List<String> via `PersistentDataType.STRING` delimited.
- `empirewand:active` (string) — huidige spell-id.
- Versieveld (optioneel): `empirewand:version` (int) — gebruik voor migraties.
- Validatie: bij lezen JSON parse; ongeldige id’s filteren tegen `SpellRegistry`; active fallback naar index 0.

Package-structuur (aanbevolen)
- [x] ~~Structuur gerealiseerd onder `src/main/java/com/example/empirewand/**`.~~
- `com.example.empirewand` — hoofdplugin en bootstrap
- `com.example.empirewand.core` — services (`ConfigService`, `FxService`, `CooldownService`, `SpellRegistry`, `WandUtil`)
- `com.example.empirewand.command` — `/ew` commands en tab-completions
- `com.example.empirewand.listener` — `WandInteractionListener`, `ProjectileListener`
- `com.example.empirewand.spells` — interfaces en basisklassen
- `com.example.empirewand.spells.comet` — implementatie
- `com.example.empirewand.spells.leap` — implementatie

Performance & telemetrie
- Budget: interact/cast handlers < 0.5 ms p/event; geen sync I/O.
- Allocaties: hergebruik `Vector`/`Location` waar mogelijk; caches voor `NamespacedKey`.
- Telemetrie (opt-in): tel casts per spell, cooldown hits/misses; debug-log toggles in `config.yml`.
- Timings: activeer Paper timings bij test; controleer listeners op hotspots.

QA & testplan per fase
- F0: plugin enable/disable herhaald 10x; geen memory groei; no errors/warnings.
- F1: 50x snelle clicks; cooldown/double-cast voorkomen; PDC-persist over 3 restarts; wand trade tussen 2 spelers.
- F2: wijzig `spells.yml` live; `/ew reload`; gedrag wijzigt zonder restart; invalid keys geven console-warnings.
- F3: feedback consistent; geen dubbele action bar spam; debouncer werkt.
- F4: projectile impact in water/lava/void; geen NPE’s; clean-up bij chunk unload.
- F5: states verlaten altijd (timer/trigger); geen stuck mismatch; pvp toggles geëerbiedigd.
- F6: permissies per command/spell respecteren; gamemode restricties getest; TPS stabiel > 19.8.
- F7: verse server met alleen plugin + configs start foutloos; README stappen kloppen.

Release-stappen
- `./gradlew clean build` produceert jar zonder warnings.
- Smoke-test op Paper 1.20.6; check logs, Timings kort sample.
- Documentatie controleren: README, permissies, config-voorbeelden, changelog.
- Versienummer bump (semver) en tag; zip met jar + voorbeeldconfigs.

Coding guidelines (kernpunten)
- Geen statische singletons; voorkeur voor constructor-injectie.
- Geen legacy kleurcodes; uitsluitend Kyori/MiniMessage.
- Guard-clauses aan het begin van event handlers; vroegtijdig return bij niet-relevante events.
- Nette null-safety; gebruik `Optional` waar passend in registry-lookup.

Tijdlijn (indicatie)
- Dag 0–1: F0
- Dag 1–3: F1
- Dag 3–4: F2
- Dag 4–5: F3
- Dag 5–7: F4
- Dag 7–8: F5
- Dag 8–9: F6
- Dag 9–10: F7
