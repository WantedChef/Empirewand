EmpireWand – Belangrijke Richtlijnen & Checklist
Doel: een compacte, praktische handleiding om van architectuur naar een strakke, voelbare game‑ervaring te gaan. Focus op implementatievolgorde, configureerbaarheid, feedback, permissies, services en QA.

Terminologie
- Caster: speler die cast.
- PDC: PersistentDataContainer op het wand‑item.
- Tick: 1/20e seconde.
- Hart: 2.0 health (1 hartje).

Kernlus (Core Gameplay Loop)
- Verkrijgen: `/ew empirewand` geeft correct item met herkenbare PDC + lore.
- Binden: `/ew bind <spell>` schrijft spell key naar PDC.
- Selecteren: R‑klik volgende, Sneak+R‑klik vorige; action bar feedback via Adventure.
- Uitvoeren: L‑klik leest actieve spell uit PDC, zoekt implementatie in registry en voert uit.

Kern DoD (Definition of Done)
- Item herkenning: wand heeft unieke `NamespacedKey` in PDC en duidelijke display‑name.
- Bind: validatie van spell key, permissiecheck, success/failure feedback (sound + action bar).
- Select: debouncing (min 3–5 ticks), wrap‑around, audit van state (PDC consistent).
- Cast: cooldown gerespecteerd, permissie `use`, heldere foutfeedback bij fail.

Configuratie (config.yml en spells.yml)
- config.yml
  - `messages.*`: alle spelerteksten centraliseren.
  - `cooldowns.*`: default cooldowns per categorie.
  - `features.block-damage`: boolean voor explosies.
  - `features.friendly-fire`: true/false per wereld of globaal.
- spells.yml (per spell‑key)
  - `display-name`: kleur/opmaak (MiniMessage toegestaan).
  - `cooldown`: ticks of seconden.
  - `values`: schade, radius, duur, snelheid, etc.
  - `flags`: bijv. `sets-fire`, `requires-los`, `hit-players`, `hit-mobs`.

Voorbeeld spells.yml (Comet en Explosive)
```yaml
comet:
  display-name: "<gradient:#FF8C00:#FF4500><b>Comet</b></gradient>"
  cooldown: 40    # 2s
  values:
    damage: 7.0   # 3.5 harten
    explosion-radius: 2.5
  flags:
    sets-fire: false
    block-damage: false

explosive:
  display-name: "<red><b>Explosive</b></red>"
  cooldown: 80    # 4s
  values:
    damage: 12.0  # ~6 harten centraal
    power: 4.0
  flags:
    block-damage: false
```

Permissies
- Commands
  - `empirewand.command.get`
  - `empirewand.command.bind`
  - `empirewand.command.unbind`
  - `empirewand.command.bindall`
- Spells
  - `empirewand.spell.use.<key>` (bv. `empirewand.spell.use.comet`)
  - `empirewand.spell.bind.<key>`
- Overig
  - `empirewand.bypass.cooldown`
  - `empirewand.debug`

Game Feel (Feedback)
- Particles: elke actie heeft leesbare, thematische particles (trail, impact, aura).
- Sounds: subtiel bij select, krachtig bij impact; volumes in balans.
- UI: action bar voor select/cooldown; korte bossbar voor kanaal‑/cast‑tijd indien relevant.
- Failure states: duidelijke melding + fizzle‑effect bij mislukte cast (LOS, cooldown, permissie).

Services & Structuur
- `SpellRegistry`: registreert spells per key; levert instance/config.
- `CooldownService`: per speler + spell‑key met ticks; API: `isReady`, `put`.
- `WandData`: helpers voor PDC (read/write actieve spell, list, index).
- `FxService`: gecentraliseerde particle/sound helpers met presets.
- `ConfigService`: laadt `config.yml` en `spells.yml` + hot‑reload hook.
- `PermissionService`: leesbare checks en samengestelde permissies.

Listeners & Commando’s
- `WandInteractionListener`
  - R‑klik → next; Sneak+R‑klik → previous; debouncer; feedback via Adventure.
  - L‑klik → cast: cooldown/permissie/LOS checks; call `Spell#execute(ctx)`.
- `ProjectileListener`
  - `ProjectileHitEvent`: resolveer spell op basis van `PersistentDataContainer` tag.
- `/ew` root command
  - `get`, `bind <spell>`, `unbind`, `bindall`, `list`, `reload` (config/herlaad).

Pseudocode (kernpad)
```java
// on right click
if (!isWand(item)) return;
if (sneaking) selectPrevious(player, item); else selectNext(player, item);
FxService.actionBar(player, selectedName(item));

// on left click (cast)
SpellKey key = WandData.getActive(item);
if (!PermissionService.canUse(player, key)) return failFX("Geen permissie");
if (!CooldownService.isReady(player, key)) return failFX("Cooldown");
Spell spell = registry.get(key);
if (!spell.prereq().passes(player)) return failFX(spell.prereq().message());
spell.execute(new SpellContext(player, config.forKey(key)));
CooldownService.put(player, key, config.cooldown(key));
```

Veiligheid & Compatibiliteit
- Respecteer PVP/world‑guard flags; optionele integratie via hooks.
- Null/validatie: targets kunnen verdwijnen; check still‑valid bij delayed effects.
- Friendly‑fire: togglebaar; caster korte spawn‑immunity (4–5 ticks) bij AoE.
- Performance: particles batchen; geen zware per‑tick loops zonder limiet.

QA/Tests (snelle checklist)
- Wand verkrijgen/binden/selecteren/casten werkt vloeiend, zonder ghost‑states.
- Cooldowns blokkeren correct; bypass‑perm werkt.
- Spells functioneren in water/in lucht/near walls; LOS correct.
- PVP aan/uit; mobs én players; undead edge‑cases (lifesteal?).
- Config reload: waarden worden live toegepast zonder NPE’s/leaks.

Release Checklist
- `plugin.yml`: commands + permissies gedocumenteerd.
- Default `config.yml` en `spells.yml` meegeleverd met comments.
- Logische defaults: geen block‑damage, duidelijke cooldowns.
- Foutmeldingen en action bar teksten consistent en vertaalbaar (messages‑sectie).

Volgende Stappen (aanbevolen volgorde)
1) Bouw `SpellRegistry`, `CooldownService`, `WandData`, `FxService` skeletons.
2) Implementeer `/ew get`, `/ew bind`, interaction listener, en 2 simpele spells (Leap, Comet).
3) Voeg `spells.yml` laadlogica toe + per‑spell configuratie.
4) Game Feel pass: particles/sounds voor select/cast/impact.
5) QA‑ronde met bovenstaande checklist, daarna uitbreiden met meer spells.

Notities
- Gebruik `PersistentDataContainer` i.p.v. ruwe metadata waar mogelijk.
- Overweeg MiniMessage (Adventure) voor opgemaakte namen in displays/action bar.
- Houd de API van `Spell` klein: `prereq()`, `execute(ctx)`, optioneel `onTick(ctx)`.

