EmpireWand – Stappenplan
Doel: concreet uitvoerplan met controlelijsten per stap, gebaseerd op de huidige architectuur en documenten.

**Voorbereiding**
- Repo bouwt met JDK 21 en Paper 1.20.6.
- `plugin.yml` aanwezig met main, commands, permissies placeholders.
- Testserver klaar met hot‑reload of snelle restarts.

**Stap 1 — Basisskelet**
- Maak hoofdklasse `com.example.empirewand.EmpireWandPlugin` (start/stop logs).
- Registreer services (lege skeletons): `SpellRegistry`, `CooldownService`, `WandData`, `FxService`, `ConfigService`, `PermissionService`.
- Voeg listeners (skeleton): `WandInteractionListener`, `ProjectileListener`.
- Controle: plugin start, geen errors.

**Stap 2 — Wand & PDC**
- Definieer `NamespacedKey`s: `wand.key`, `wand.spells`, `wand.activeIndex`.
- `WandData`: read/write helpers (String‑lijst + index) op ItemStack PDC.
- `/ew get`: geeft herkenbare wand met display‑name en PDC‑flag.
- Controle: herstart → wand behoudt data; meerdere wands mogelijk.

**Stap 3 — Binden & Selecteren**
- `/ew bind <spell>`: valideer key, permissie, voeg toe aan lijst.
- `/ew unbind <spell>` en `/ew bindall`.
- Selectie: R‑klik → next; Sneak+R‑klik → previous; debouncer 4 ticks.
- Action bar via Adventure met huidige spell‑naam.

**Stap 4 — Cast Pad**
- L‑klik: haal actieve key, checks: permissie/use, cooldown.
- `Spell` interface + `SpellContext` (caster, config, services).
- Call `registry.get(key).execute(ctx)`; cooldown plaatsen.
- Failure feedback: fizzle FX + bericht.

**Stap 5 — Configs**
- `config.yml`: messages, feature‑flags, defaults.
- `spells.yml`: per‑spell `display-name`, `cooldown`, `values.*`, `flags.*`.
- `ConfigService`: laden, getters, `reload` implementeren; `/ew reload`.

**Stap 6 — Spells v0**
- Implementeer `Leap` (mobility) en `Comet` (projectiel AoE) minimal.
- `ProjectileListener`: tag/resolve projectielen via PDC.
- `FxService`: helpers voor trail/impact/action bar/sounds.

**Stap 7 — Game Feel**
- Trails: consistente presets; impact FX; UI teksten centraliseren.
- Geluiden: select/cast/impact; volumes en pitches in balans.

**Stap 8 — Spell Set v1**
- `Explosive`, `MagicMissile`, `GlacialSpike`, `GraspingVines`, `Heal`, `LifeSteal`.
- Daarna: `Polymorph`, `EtherealForm` (speciale state & revert logisch testen).

**Stap 9 — Permissions & Edge Cases**
- Perms: `empirewand.command.*`, `empirewand.spell.use.<key>`, `empirewand.spell.bind.<key>`.
- Edge cases: PVP toggles, friendly‑fire, region flags, water/lucht/walls.

**Stap 10 — QA & Release**
- QA checklist doorlopen (in `important.md`).
- Voorbeeld `config.yml`/`spells.yml` met comments meesturen.
- `README` updaten met gebruik/commands; changelog opstellen.

**Snelle Checklists**
- Core loop: get/bind/select/cast → werkt zonder ghost states.
- Persist: PDC data blijft na restart en bij item transfer.
- Cooldowns: blokkeren cast; bypass werkt; visuele countdown optioneel.
- Spells: geen NPE bij target null; LOS correct; cancels netjes.
- Performance: geen zware per‑tick loops; particles batchen/limiteren.

