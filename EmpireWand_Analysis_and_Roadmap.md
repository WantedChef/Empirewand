# EmpireWand: Analyse en Roadmap (correct en actueel)

Datum: 6 september 2025  
Auteur: EmpireWand Team  
Versie: 1.1 (document)

Doel: een feitelijke analyse van de huidige codebase en een realistische, incrementele roadmap. Focus op juistheid, prestaties, stabiliteit en ontwikkelervaring. Nog niets implementeren; dit document dient als leidraad voor prioritering en verificatie.

## TL;DR
- Project is netjes gestructureerd (Java 21, Paper 1.20.6), met 60 spell‑implementaties en centrale services (config, fx, registry, permissions, wand‑data). 93 Java‑bestanden in totaal.
- Sterk: config‑gedreven spelwaarden, duidelijke permission nodes, PDC‑gebaseerde wandopslag, bStats + eenvoudige performance‑meting, unit‑tests voor core en voorbeeldspells.
- Belangrijkste verbeterpunten: dubbele afhandeling van projectile hits (twee listeners), herhaling rond trails/timers (±50 referenties naar BukkitRunnable, ±20 timers), cooldown‑opschoning bij quit, command‑flow leesbaarheid.
- Roadmap: 3 sprints. Sprint 1 richt op correctheid en eenduidige event‑routing; Sprint 2 pakt duplicatie en DX aan; Sprint 3 polish + documentatie/tests.

## Projectoverzicht (feitelijk)
- Doelplatform: Paper 1.20.6; Java 21; Adventure voor text.
- Codeaantallen: 93 Java‑bestanden; 60 spells onder `spell/implementation`.
- Kernpakketten:
  - `com.example.empirewand`: entrypoint (`EmpireWandPlugin`), commands, listeners.
  - `core`: `ConfigService`, `CooldownService`, `FxService`, `PermissionService`, `SpellRegistryImpl`, `WandData`, `Keys`, `PerformanceMonitor`, metrics‑diensten.
  - `spell`: API (`Spell`, `ProjectileSpell`, `SpellContext`, `Prereq`, `CastResult`) + implementaties.
- Config & resources: `plugin.yml` (placeholders via Gradle expand), `config.yml`, `spells.yml`, `messages*.properties`.
- Build & tools: Gradle (Checkstyle, SpotBugs, Jacoco), bStats, unit‑tests via JUnit 5 + Mockito.
- Commando: `/ew` met subcommando’s (`get|bind|unbind|bindall|set-spell|list|reload|cooldown|migrate`) + tab‑complete.

## Architectuur en datastromen
- Services via `EmpireWandPlugin`‑getters gedeeld met listeners/commands; schaalbaar genoeg voor deze codebasegrootte.
- Spells worden expliciet geregistreerd in `SpellRegistryImpl` en aangesproken via een key (kebab‑case) uit `spells.yml`.
- Wanddata in PDC: sleutelverificatie (`WAND_KEY`), gebonden spells als CSV, actieve index bewaakt.
- Eventflow:
  - `WandInteractionListener`: selecteren/cyclen en casten; valideert permissies en cooldown op basis van `config.yml`/`spells.yml`.
  - `ProjectileListener`: routeert hits naar `ProjectileSpell.onProjectileHit` wanneer een spell dit interface implementeert.
  - `EntityListener`: behandelt een aantal spell‑specifieke projectile‑hits (bv. `explosive`, `glacial-spike`, `lifesteal`, `comet`, `magic-missile`) en extra events (fall damage bij `ethereal-form`, explode‑gedrag).
- FX: `FxService` centraliseert particles/sounds/actionbar, heeft batching en simpele timings via `PerformanceMonitor`.
- Config: `ConfigService` laadt, valideert (`ConfigValidator`) en kan migreren (`ConfigMigrationService`) met back‑ups.
- Metrics: `MetricsService` (bStats) + interne `DebugMetricsService` (ringbuffer + P95), feature‑toggels via config.

## Kwaliteit en testbaarheid
- Stijl en conventies volgen repo‑richtlijnen (Java 21, UTF‑8, geen wildcard imports, kebab‑case keys, guard‑early in listeners).
- Unit‑tests aanwezig voor kernservices (cooldowns, config, wand, registry, fx) en voorbeeldspells (`MagicMissile`, `Heal`). Geen end‑to‑end tests (verwacht bij Paper integratie).
- Statistische analyse (Checkstyle/SpotBugs) geconfigureerd; waarschuwingen momenteel gedoogd via instellingen.

## Belangrijkste bevindingen
- Sterktes:
  - Config‑gedreven spelwaarden; permissies consistent (plugin.yml definieert zowel `use.*` als `bind.*`).
  - Heldere API‑oppervlakken (`EmpireWandAPI`, `SpellRegistry`, `WandService`, `PermissionService`).
  - PDC‑sleutels en wrappers in `Keys` maken opslag eenduidig en herbruikbaar.
  - FX‑batching en eenvoudige performance‑timing aanwezig; bStats opt‑in met extra debugstatistieken.
  - Tests dekken kernlogica en voorbeeldspells; `SpellContext` maakt testen eenvoudiger.
- Verbeterpunten:
  - Projectile‑afhandeling is dubbel uitgevoerd: zowel `ProjectileListener` (voor `ProjectileSpell`) als `EntityListener` handelen `ProjectileHitEvent` af. Dit kan leiden tot dubbele FX of divergent gedrag. Maak de routering eenduidig (zie Sprint 1).
  - Duplicatie rond projectile‑trails/timers: veel spells hebben vergelijkbare `BukkitRunnable`‑patronen (±50 referenties; ±20 `runTaskTimer`‑aanroepen). Extract/utility kan DRY bevorderen en het aantal schedulers reduceren.
  - Cooldowns worden niet expliciet opgeschoond bij player‑quit. Voeg een quit‑listener toe om per‑speler state (`CooldownService`) te wissen.
  - Command‑flow (`EmpireWandCommand`) is lang door if‑chains. Een lichte subcommand‑structuur verhoogt leesbaarheid/onderhoudbaarheid.
  - `Keys` gebruikt vaste namespace‑constanten ("empirewand"). Dit is stabiel, maar documenteer dat de plugin‑namespace gelijk moet blijven met de projectnaam om PDC‑compatibiliteit te behouden.

## Prestatie‑observaties (zonder aannames)
- Trails/FX zijn veelvoorkomend in spells; batching in `FxService` helpt. Houd flush‑momenten expliciet bij intensieve bursts.
- `PerformanceMonitor` wordt gebruikt in listeners/FX. Stel team‑interne drempelwaarden af (bijv. 10–25 ms voor hotspots) en bekijk debug‑metrics periodiek.
- Vermijd I/O of zware berekeningen in event‑paden; huidige code houdt zich hieraan.

## Veiligheid en stabiliteit
- Inputvalidatie: commands valideren argumenten en spell‑keys via registry; `ConfigValidator` waarborgt structuur/typen/ranges in YAML.
- Rechten: `PermissionService` centraliseert permissienamen. `plugin.yml` bevat expliciete nodes voor alle spells (use/bind).
- Foutafhandeling: services en listeners hanteren guard‑early en defensieve null‑checks; `onEnable` logt en schakelt plugin uit bij ernstige fouten.

## Roadmap (geen implementatie, wel planning)

Sprint 1 — Correctheid & routering (focus: low‑risk, hoge impact)
- Eenduidige projectile‑hit routering:
  - Doel: hybride benadering (besloten). Nieuwe/complexe projectile‑spells implementeren `ProjectileSpell` en lopen via `ProjectileListener`. Bestaande simpele effecten mogen tijdelijk in `EntityListener` blijven met duidelijk deprecatie‑pad.
  - `EntityListener` versmallen naar cross‑spell events (fall‑damage ethereal, explode‑policy) en generieke failsafes; spell‑specifieke switches gefaseerd afbouwen.
- Cooldown‑opschoning: `PlayerQuitEvent`‑listener toevoegen die `CooldownService.clearAll(playerId)` aanroept.
- Verificatieplan: regressietest `magic-missile`, `comet`, `glacial-spike`, `explosive`; controleren op dubbele FX en correcte remove‑logica. Unit‑tests uitbreiden waar zinvol (mocked). 

Sprint 2 — Duplicatie & DX
- DRY voor trails/timers:
  - Extract utility (bijv. `ProjectileTrailUtil` of extra helpers in `FxService`) voor veelvoorkomende trail‑loops en tick‑schemata.
  - Richtlijn: één scheduler per spell‑actie waar mogelijk; prefer korte taken boven langdurige timers.
- Command‑refactor:
  - Introduceer interne subcommand‑structuur (kleine interface + dispatcher) om `EmpireWandCommand` op te delen; behoudt bestaande functionaliteit.
- Spell‑registratie:
  - Optioneel: annotatie‑gedreven of tabelgestuurde registratie. Weging: expliciete registratie is snel en duidelijk; automatisering alleen als onderhoudslast stijgt.

Sprint 3 — Polish, documentatie en tests
- API‑/Javadoc aanvullen voor public surfaces (`api/*`, `core/*`).
- README en `spells.md` uitbreiden met voorbeelden en best‑practices (kebab‑case keys, waardenranges, FX‑tips).
- Testuitbreiding: extra unit‑tests voor ten minste 3 projectile‑spells en 2 utility‑paden (config‑migratiepaden, keys/wanddata randen).

## Acceptatiecriteria per sprint
- Sprint 1:
  - Hybride routering actief: `ProjectileSpell`‑spells via `ProjectileListener`; geen dubbele FX voor `comet`/`magic-missile`/`glacial-spike`/`explosive`.
  - Cooldowns worden gewist bij quit (geobserveerd in unit/mocked tests).
- Sprint 2:
  - Merkbare reductie in timer‑duplicatie; trail‑helpers hergebruikt in alle spells met standaard trail.
  - `EmpireWandCommand` opgesplitst in compacte subcommands; gedrag ongewijzigd (backward compatible).
- Sprint 3:
  - Publieke API’s voorzien van Javadoc; ontwikkelrichtlijnen gedocumenteerd.
  - Nieuwe tests toegevoegd en groen onder `./gradlew test` + Jacoco‑rapport.

## Risico’s en mitigatie
- Event‑routering: kans op regressies bij verplaatsen van logic. Mitigatie: per spell verifiëren met gecontroleerde FX/damage scenario’s en unit‑tests.
- Scheduler‑reductie: te agressieve consolidatie kan gedrag veranderen (timing). Mitigatie: helper abstraheert alleen boilerplate; spell‑waarden blijven leidend via config.
- Registratie‑aanpassing: automatische detectie kan init‑kosten verhogen. Mitigatie: behouden van expliciete registratie tenzij onderhoudslast dit vereist.

## Open vragen voor afstemming
## Besluiten (open vragen gesloten)
- Projectile‑spells: Hybride aanpak. Nieuwe/complexe projectile‑spells implementeren `ProjectileSpell`; eenvoudige bestaande effecten mogen tijdelijk in `EntityListener` blijven met deprecatie en migratie‑pad.
- `Keys`/namespace: Backwards‑compatibility behouden. Vaste namespace `empirewand` aanhouden om PDC‑compat te garanderen; evaluatie op lange termijn kan, maar geen migratie nu.
- bStats: Tijdelijk uit in productie. Advies: zet `metrics.enabled: false` op productie‑servers totdat een definitieve plugin‑ID is geconfigureerd; laat in development aan voor lokale metingen.

## Verificatie‑checklist (na implementatie)
- Build: `./gradlew clean build` produceert JAR; plugin start zonder errors.
- Tests: `./gradlew test` groen; Jacoco‑rapport gegenereerd.
- Handmatige validatie: projectiles tonen geen dubbele FX; cooldown‑clear werkt; commands functioneren en tab‑complete blijft accuraat; metrics op productie staan uit volgens configuratie.

—
Opmerking: dit document beschrijft analyse en planning. Nog niets implementeren; wijzigingen volgen in gerichte PR’s per sprint.
