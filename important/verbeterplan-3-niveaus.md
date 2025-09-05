# EmpireWand – Verbeterplan in 3 Niveaus

Dit document beschrijft een concreet, uitvoerbaar plan om EmpireWand in drie duidelijke niveaus te verbeteren: van fundament (kwaliteit en stabiliteit), naar beleving (UX en gameplay), tot platform (API, schaalbaarheid en release‑discipline). Elk niveau bevat het waarom, wat, hoe, acceptatiecriteria, risico’s en een globale doorlooptijd. Het plan volgt jullie repo‑richtlijnen (Java 21, geen wildcard imports, geen sync I/O in event paths, etc.).

## Samenvatting

- Doel: hogere kwaliteit, betere spelerservaring, duurzame basis voor uitbreidbaarheid en releases.
- Scope: codekwaliteit, performance, configuratie/migratie, testdekking, UX/feedback, spell‑framework, API, CI/CD, documentatie.
- Aanpak: drie opeenvolgende niveaus; elk niveau levert zelfstandig waarde en kan afzonderlijk worden gereleased.
- Succes: sub‑ms event cost gemiddeld, >80% testdekking op core, zero‑warning builds, voorspelbare releases met changelogs en artefacten.

---

## Overzicht van de niveaus

1) Fundament & Kwaliteit – stabiliseren, meten en harden van core services en config.
2) UX & Gameplay – vloeiende wand‑interactie, duidelijk feedbackkanaal, consistente spells en targeting.
3) Platform & Schaalbaarheid – API, metrics/telemetrie, migraties, release‑pipeline en compatibiliteit.

---

## Niveau 1 – Fundament & Kwaliteit

### Waarom

- Betrouwbaarheid en voorspelbaarheid zijn randvoorwaardelijk voor verdere groei.
- Core services (CooldownService, ConfigService, SpellRegistry) vormen het koppelvlak voor alle features.
- Zonder meetbaarheid (tests/metrics) zijn regressies en performanceproblemen lastig te voorkomen.

### Wat (deliverables)

- Testdekking core modules: CooldownService, ConfigService, SpellRegistry, PermissionService, deterministische spell‑logica.
- Config‑hardening: schema/validatie, defaults, config‑versies en migraties.
- Zero‑warning build, statische analyse (Checkstyle, SpotBugs) en JaCoCo coverage‑rapport.
- Guard‑patterns in listeners; centralisatie van tekst/formatting (Adventure/MiniMessage) en PDC‑sleutels.
- Performance‑baseline en micro‑optimalisaties voor hotspots (allocaties omlaag, batching particles in FxService).

### Hoe (stappen)

.

2. Config‑hardening
   - Introduceer `config-version` in `config.yml` en `spells.yml`.
   - Voeg `ConfigValidator` toe: valideert structuur, types, waardenbereiken en kebab‑case spell‑keys.
   - Implementeer `ConfigMigrationService`: simpele migratiestappen op basis van `config-version` (non‑destructief, met back‑up).

3. Kwaliteitshek
   - Activeer Checkstyle (no wildcard imports, 4‑spatie indent + bestaande conventies).
   - Activeer SpotBugs en configureer Gradle om bij hoog/medium severity te falen.
   - Voeg JaCoCo toe met target coverage (bijv. 80% voor `core/*`).

4. Guard‑patterns en centralisaties
   - Listeners: minimaliseer vroege exits en duplicate checks; voorkom sync I/O.
   - Tekst: centraliseer kleur/formatting (Adventure MiniMessage). Voorbeeld: vervang ad‑hoc `stripMiniTags` usages door util in bijv. `TextService`.
   - `Keys`: uniformeer `NamespacedKey` creatie via plugin‑scoped factory; hergebruik `PersistentDataType` wrappers.

5. Performance‑baseline
   - Log basic timings (debug‑modus) per event/spell (gemiddelde en P95 over 1m venster).
   - Minimaliseer objectallocaties in hotpaths; batch particles via `FxService` (één scheduler taak per tick i.p.v. veel kleine).

### Acceptatiecriteria (DoD)

- `./gradlew build` slaagt zonder warnings; Checkstyle/SpotBugs clean.
- `./gradlew test` met >80% coverage voor `core/*` en geen flakey tests.
- Configs hebben `config-version` en migreren correct wanneer waarden ontbreken of key‑namen wijzigen.
- Hotpaths in listeners tonen geen significante allocaties; debug‑timings < 1 ms gemiddeld per event.

### Risico’s & mitigaties

- Testflakiness door Bukkit gedrag → MockBukkit + isolatie van logica buiten Bukkit API waar mogelijk.
- Te strenge statische analyse → start met waarschuwingen, dan opschalen naar fouten per regelset.
- Config migraties → altijd back‑ups maken en dry‑run logging.

### Doorlooptijd (indicatief)

- 1–2 weken, afhankelijk van bestaande complexiteit en testbaarheid.

---

## Niveau 2 – UX & Gameplay

### Waarom

- Spelerservaring bepaalt adoptie. Heldere feedback, intuïtieve bediening en consistente cooldowns zijn cruciaal.
- Configuratiegedreven spells en targeting verlagen onderhoud en verbeteren balans.

### Wat (deliverables)

- Wand UX: spelwisseling (scroll/sneak+click), actiebar/cooldown feedback, foutmeldingen met reden (geen permissie, cooldown actief, resources ontbreken).
- Command UX: uitbreiden van `/empirewand` subcommando’s, duidelijke help en tab‑complete, permissiechecks met heldere messages.
- Spell‑framework: uniforme lifecycle (`canCast`, `cost`, `cooldown`, `cast`, `onFinish`), prerequisites, targeting util (raycast), effect batching.
- Config‑uitbreiding: per spell key standaardvelden (cooldown, kosten, particle‑profiel, geluid), validatie en defaults.
- Lokalisatie: minimaal EN/NL resource bundels voor messages.

### Hoe (stappen)

1. UX‑feedbacklaag
   - `FxService` uitbreiden met actiebar‑helper, title/subtitle templates en geluidsprofielen.
   - Gestandaardiseerde fouten: `NoPermission`, `OnCooldown`, `InvalidTarget`, `OutOfRange`, etc. met één plek voor messaging.

2. Input & besturing
   - Listener: sneaken + scroll/right‑click om spells te cyclen; respecteer cooldown visueel.
   - Voeg debounce/anti‑spam toe (bijv. minimale interval wisselen/casten).

3. Commands & tabcomplete
   - `/empirewand` subcommando’s: `give`, `set-spell`, `list`, `reload`, `cooldown <player> clear`.
   - TabComplete: spell‑keys (kebab‑case), spelersnamen, permissiegevoelig.

4. Spell‑framework harmoniseren
   - Interface uitbreiden: `boolean canCast(Context)`, `void applyCost(Context)`, `Duration getCooldown(Context)`, `CastResult cast(Context)`.
   - `Context` bevat caster, target, omgeving, configuratie en Fx‑kanaal.
   - Targeting util: raycast met max afstand, entity selectie, grond/hemel checks; gedeeld door spells.

5. Config‑uitbreiding en validatie
   - `spells.yml` standaardvelden: `cooldown`, `range`, `mana/cost`, `fx: {particles: …, sound: …}`.
   - Validator controleert kebab‑case keys en dat elke key een geregistreerde implementatie heeft.

6. Lokalisatie
   - `messages.properties` (EN) en `messages_nl.properties` (NL).
   - `TextService` levert geformatteerde Adventure‑componenten per sleutel met placeholders.

### Acceptatiecriteria (DoD)

- Spelers zien consistente actiebar‑updates bij cast/cooldown/mislukte acties.
- Spells zijn af te spelen via scroll/sneak‑combo; wissel is vloeiend en spam‑bestendig.
- Commands hebben duidelijke help en foutmeldingen; tabcomplete werkt per permissie.
- Spells staan in `spells.yml` met uniforme velden en worden strikt gevalideerd.
- Berichten zijn gelokaliseerd (EN/NL) met eenvoudige uitbreidbaarheid.

### Risico’s & mitigaties

- Te veel visuele feedback kan druk geven → throttle en per‑speler toggles in config.
- Incompatibiliteit met bestaande items → wand‑identificatie via PDC key die uniek is en stabiel blijft.

### Doorlooptijd (indicatief)

- 2–3 weken, afhankelijk van aantal spells dat geharmoniseerd wordt.

---

## Niveau 3 – Platform & Schaalbaarheid

### Waarom

- Externe integraties en community‑bijdragen vragen om stabiele API’s en release‑proces.
- Telemetrie en migraties reduceren risico bij updates en versnellen debugging.

### Wat (deliverables)

- Publieke API: events (`SpellCastEvent`, `SpellFailEvent`, `WandSelectEvent`), `SpellRegistry` extensiepunten, `PermissionService` hooks.
- Metrics/telemetrie: bStats integratie (opt‑in), eigen debug‑metrics (casts per minuut, fail‑reasons), performance‑sample.
- Config‑migraties v2→v3: tooling met duidelijke logging en back‑ups.
- Release‑pipeline: GitHub Actions release op `v*` tags, artefacten upload, changelog via conventionele commits, Javadoc artefacten.
- Compatibiliteit: getest op Paper 1.20.6, Java 21 toolchain, schaduw‑JAR met juiste relocations.

### Hoe (stappen)

1. API‑laag
   - Definieer `api` package met events en lichte service‑interfaces; documenteer via Javadoc.
   - Markeer API‑stabiliteit (Experimental/Stable) en semver beleid.

2. Metrics & logging
   - Integreer bStats met toggle in `config.yml`.
   - Voeg interne counters/timers toe (alleen bij debug‑modus) met ringbuffer voor P95.

3. Migratie‑tooling
   - `ConfigMigrationService` uitbreiden met discrete stappen en rapportage (wat, waarom, welke keys).
   - CLI‑achtig admin‑commando: `/empirewand migrate --dry-run`.

4. Release & distributie
   - GH Actions: build cache, test, checkstyle/spotbugs, jacoco; upload `empirewand-<ver>.jar` + `-sources.jar` + `-javadoc.jar`.
   - Release‑notities genereren op basis van conventionele commits; changelog bijhouden in `CHANGELOG.md`.

5. Compatibiliteit & schaduw
   - Gradle Shadow plugin: relocations voor embedded libs (bijv. adventure‑platform indien gebundeld).
   - Test start/stop lifecycle op Paper 1.20.6 lokaal.

### Acceptatiecriteria (DoD)

- API events en services zijn gedocumenteerd en backwards‑compatible binnen minor releases.
- bStats data verschijnt (bij opt‑in) en interne debug‑metrics zijn zichtbaar in logs bij debug‑modus.
- Releases via GitHub Actions leveren artefacten en changelogs automatisch op tags.
- Schaduw‑JAR start probleemloos op Paper 1.20.6.

### Risico’s & mitigaties

- API‑lock‑in → markeer experimental en plan de eerste stabile release (1.0.0) pas na feedback.
- Telemetrie‑privacy → alleen geaggregeerde, niet‑persoonlijke metrics; opt‑in.

### Doorlooptijd (indicatief)

- 2 weken voor eerste stabiele release‑keten en baseline metrics.

---

## Architectuur & Code‑structuur (concrete aanpassingen)

- Packages blijven onder `com.example.empirewand`.
- Nieuwe/uitgebreide modules:
  - `com.example.empirewand.api`: events en lichte service‑interfaces.
  - `com.example.empirewand.core.text`: `TextService` (MiniMessage/Adventure), i18n bundels.
  - `com.example.empirewand.core.config`: `ConfigValidator`, `ConfigMigrationService`.
  - `com.example.empirewand.core.targeting`: raycast/selection utils (range, FOV, entity filters).
  - `com.example.empirewand.core.metrics`: debug‑timings en counters.
  - `com.example.empirewand.spell`: vernieuwde `Spell` interface + `Context` model.

Hints op codepunten:

- `src/main/java/com/example/empirewand/listeners/WandInteractionListener.java`: centraliseer tekstformatting (nu o.a. `display = stripMiniTags(display);`) via `TextService` en beperk logica in listeners.
- `src/main/java/com/example/empirewand/core/Keys.java`: bied factory‑methodes voor `NamespacedKey` en strongly‑typed PDC.
- `src/main/java/com/example/empirewand/core/SpellRegistry.java`: valideer kebab‑case keys en éénmalige init met logging van conflicts.
- `src/main/resources/spells.yml`: voeg uniforme velden toe; valideer op load en bij `/empirewand reload`.

---

#stra

## Config, Migraties & Backwards‑compatibility

- `config-version` in alle YAML’s; migratiestappen versie‑per‑versie met dry‑run en back‑up.
- Strikte validatie bij load; duidelijke foutmeldingen met pad naar key.
- Compatibiliteit: behoud bestaande spell keys en gedrag; breaking changes via major release.

---

## Security, Permissions & Stabiliteit

- Permissions via `PermissionService` met duidelijke naamruimte (`empirewand.spell.use.<key>` en beheercommando’s).
- Inputvalidatie op commands (types, ranges) en listeners (null checks, wereld/GM‑states).
- Fail‑safe defaults: bij configfout → schakel spell uit met log en duidelijke admin‑melding.

---

## Performance‑richtlijnen

- Budget: sub‑milliseconde per event gemiddeld; throttle heavy effecten.
- Batching: particles en geluiden via `FxService` scheduler i.p.v. per‑cast losse taken.
- Allocatie‑reductie: hergebruik builders, avoid stream/boxing in hotpaths, voorkom tijdelijke objecten in loops.

---

## Uitrolstrategie

- Feature‑flags per UX/feedback onderdeel (actiebar, geluiden, particles dichtheid).
- Canary‑test op kleine server, daarna brede uitrol.
- Rollback: behoud vorige JAR en config‑back‑ups; `/empirewand migrate --rollback` indien nodig.

---

## Planning (indicatief, sprints)

- Sprint 1 (Niveau 1): tests, config‑validatie, quality gates, baseline timings.
- Sprint 2 (Niveau 2 – deel 1): UX‑feedbacklaag, commands/tabcomplete, spell‑framework harmonisatie.
- Sprint 3 (Niveau 2 – deel 2): targeting utils, config‑uitbreiding per spell, lokalisatie.
- Sprint 4 (Niveau 3): API events, metrics, migratie‑tooling, release‑pipeline, schaduw‑JAR.

---

## Backlog (selectie, gegroepeerd)

- Core/kwaliteit
  - EW‑101: MockBukkit + Mockito testinfra opzetten
  - EW‑102: Tests CooldownService (incl. clock abstraction)
  - EW‑103: ConfigValidator + foutmeldingen met pad
  - EW‑104: ConfigMigrationService + backups + dry‑run
  - EW‑105: Checkstyle/SpotBugs/JaCoCo integreren

- UX/Gameplay
  - EW‑201: Actionbar‑feedback kanaal in FxService
  - EW‑202: Spell cycling (sneak + scroll/right‑click) + debounce
  - EW‑203: Command set `/empirewand ...` + TabCompleter uitbreiden
  - EW‑204: Spell interface + Context model harmoniseren
  - EW‑205: Targeting util (raycast, filters, range)
  - EW‑206: Lokalisatie EN/NL + TextService

- Platform
  - EW‑301: API events (SpellCastEvent, SpellFailEvent, WandSelectEvent)
  - EW‑302: bStats integratie + debug‑metrics
  - EW‑303: GH Actions release op tags + changelog
  - EW‑304: Shadow JAR + relocations + compat‑tests

---

## Definitie van Succes

- Kwaliteit: >80% coverage core, zero‑warning builds, consistente listeners met guards.
- UX: duidelijke actiebar/hints, intuïtieve spell‑wissel, minder supportvragen.
- Platform: voorspelbare releases met changelogs, stabiele API voor uitbreidingen, inzicht via metrics.

---

## Volgende stap (zonder implementatie)

1) Bevestig dit plan en prioriteiten per niveau. 2) Kies sprint 1 scope (EW‑101 t/m EW‑105). 3) Daarna implementatie starten volgens dit document.

