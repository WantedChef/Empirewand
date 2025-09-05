EmpireWand – Richtlijnen
Doel: consistente codekwaliteit, UX en onderhoudbaarheid. Sluit aan op Java 21, Paper 1.20.6 en de architectuur in de repo.

**Code Stijl**
- Taal: Java 21; gebruik records, pattern matching, sealed types waar nuttig.
- Namen: `lowerCamelCase` voor variabelen/methoden, `UpperCamelCase` voor types, `UPPER_SNAKE_CASE` voor constantes.
- Packages: per spell eigen package (`spells.comet`), core gescheiden (`core.*`).
- Immutability first: maak context/config immutable; vermijd gedeelde mutable state.

**Architectuur**
- Ontkoppeling: core kent geen concrete spells/wands; gebruik registry.
- Dependency Injection: geef services via constructors; geen statische singletons.
- `Spell` API: klein en duidelijk (`key()`, `displayName()`, `prereq()`, `execute(ctx)`).
- Context: `SpellContext` met caster, config‑view, services, time.

**Events & Schedulers**
- Click‑flow: registreer alleen op wand‑items; debouncen (≥4 ticks).
- Projectile‑flow: tag projectiel via `PDC`; resolve in `ProjectileHitEvent`.
- Schedulers: korte, begrensde taken; cancellen bij invalid target of unload.

**PDC & Data**
- Keys: `NamespacedKey("empirewand", "wand.key")` etc.; documenteer in `technical.md`.
- Opslag: spells als `String`‑lijst; actieve index als `int`.
- Versie: voeg `wand.version` toe voor toekomstig migreren.

**UI & Feedback**
- Tekst: Kyori Adventure; MiniMessage voor opmaak; centraliseer messages in config.
- Action bar: altijd tonen bij select; korte status bij mislukte cast.
- Geluid: subtiel bij select, krachtig bij impact; consistente volumes.
- Particles: thematisch per spell; gebruik presets in `FxService`.

**Fouten & Validatie**
- Vroeg verifiëren: permissie, cooldown, LOS, target valid.
- Fail fast: stop met duidelijke melding + fizzle FX; geen stacktraces voor spelers.
- Log: waarschuw bij ontbrekende config keys; nooit crashen op NPE.

**Performance**
- Vermijd zware per‑tick loops; prefer events/schedulers met korte duur.
- Particle batching; cap aantal actieve projectielen/effecten per speler.
- Gebruik Paper API features (async waar toegestaan; niet voor Bukkit‑only APIs).

**Permissions**
- Command permissies: fijnmazig (`empirewand.command.*`).
- Spell permissies: `use` en `bind` gescheiden per key.
- Bypass permissies voor staff (cooldown/debug) expliciet documenteren.

**Configuratie**
- Alles in YAML; geen hard‑coded balanswaarden.
- `config.yml`: messages, feature flags (block‑damage, friendly‑fire), defaults.
- `spells.yml`: display‑name, cooldown, values.*, flags.* per spell.
- Herlaad veilig: `/ew reload` valideert en logt diffs/warnings.

**Security & Compat**
- Respecteer PVP/world‑guard; fail closed bij onzekere state.
- Input sanitizen (commando’s), geen kleurcodes via `&`; gebruik MiniMessage.
- Geen reflectie/mixin hacks; Paper kloof minimaliseren.

**Git & Reviews**
- Commits: klein, betekenisvol, in imperative mood ("Add CooldownService").
- Branches: feature branches; PRs met beschrijving, tests en screenshots/gifs.
- CI (optioneel): build + basic lint; unit tests voor helpers/services.

