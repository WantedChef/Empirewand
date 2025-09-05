EmpireWand – Spell Design Gids
Doel: snelle, consistente referentie zodat je elke spell straks direct kan bouwen. Elke spell heeft dezelfde secties: Doel, Input/Output, Mechanics, Events, Visuals/SFX, Balance, Randvoorwaarden, Pseudocode.

Terminologie
- Hartje: 2.0 health (1 hart).
- Tick: 1/20e seconde.
- Caster: de speler die cast.
- Target: entiteit die geraakt wordt.

Categorie: Direct Damage

Comet
- Doel: snelle AoE projectile voor algemene combat.
- Input/Output: input = cast zonder target; output = explosie bij impact.
- Mechanics: `player.launchProjectile(Fireball.class)` met power ~2.5F; `setIsIncendiary(false)`; snelheid: default. Volgproject via scheduler voor trail.
- Events: `ProjectileHitEvent` voor impact en schade-afhandeling; metadata tag `spell=comet`.
- Visuals/SFX: `SOUL_FIRE_FLAME` trail, `EXPLOSION_NORMAL` bij hit, `ENTITY_BLAZE_SHOOT` geluid.
- Balance: ~3.5 harten schade in centrum; lichte falloff mogelijk.
- Randvoorwaarden: respecteer PVP en region flags; geen block damage indien gewenst.
- Pseudocode:
  - onCast: fireball = launch Fireball; fireball.setIncendiary(false); setYield(2.5); tag(fireball,"comet")
  - onTick: spawn trail bij fireball.loc (2 ticks interval)
  - onHit(if tag==comet): doExplosionDamage(no block break)

Explosive
- Doel: trage, zware AoE burst voor verdedigingsbreuk.
- Input/Output: input = cast; output = grote explosie on hit.
- Mechanics: `player.launchProjectile(WitherSkull.class)`; langzame snelheid; op impact `world.createExplosion(loc, 4.0F, false, false)` of custom radius.
- Events: `ProjectileHitEvent`, metadata `spell=explosive`.
- Visuals/SFX: `SMOKE_LARGE`, `EXPLOSION_HUGE`; `ENTITY_GENERIC_EXPLODE`.
- Balance: ~6 harten in centrum; makkelijk te ontwijken; friendly-fire mogelijk.
- Randvoorwaarden: toggle voor block damage (config); immuniteit voor caster 4 ticks.
- Pseudocode:
  - onCast: skull = launch WitherSkull; tag(skull)
  - onHit: createExplosion(no fire, no block break); applyAoEDamage()

Magic Missile
- Doel: gegarandeerde hit met lage burst (3 salvo’s).
- Input/Output: input = auto-target of crosshair target; output = 3 hits met korte delay.
- Mechanics: geen echt projectiel; bepaal `target = player.getTargetEntity(range)` of auto-lock laatste damage source; scheduler die 3 keer `target.damage(x, caster)` triggert met 6–8 ticks tussenruimte; cancel als target weg is.
- Events: none/projectile; gebruik `EntityDamageByEntityEvent` alleen voor modifiers.
- Visuals/SFX: partikelstraal `ENCHANTMENT_TABLE` langs een line-interpolate van caster→target; `ENTITY_ILLUSIONER_CAST_SPELL`.
- Balance: 3×1.5 harten, totaal ~4.5; werkt tegen snelle targets.
- Randvoorwaarden: line-of-sight check per salvo; niet door muren tenzij feature.
- Pseudocode:
  - onCast: lock target; for i in 1..3 schedule after i*7t: if valid(target) then damage(target, 3.0); drawBeam()

Glacial Spike
- Doel: damage plus sterke slow (control tool).
- Input/Output: input = cast; output = impact damage + slow.
- Mechanics: simpele variant via `Arrow` of `Trident`; alternatief: ArmorStand met ICE helm, voortbewegen via vector; bij hit: `damage(8.0)` en `SLOW(80t, amplifier=2)`.
- Events: `ProjectileHitEvent` of collision check per tick.
- Visuals/SFX: `SNOWFLAKE`/`ITEM_SNOWBALL` trail; `BLOCK_ICE_BREAK` bij impact; koude sound.
- Balance: ~4 harten + SLOW II (4s). Sterk in duels.
- Randvoorwaarden: één Spike tegelijk per caster (cooldown) om spam te voorkomen.
- Pseudocode:
  - onCast: proj = launch Arrow; tag; onHit: applyDamageAndSlow()

Grasping Vines
- Doel: root/immobilize voor korte duur.
- Input/Output: AoE cone of op target-block.
- Mechanics: apply `SLOWNESS` met hoog level (bv. 250) voor 2–3s; optioneel `ROOTED` logica door movement te cancelen in `PlayerMoveEvent` zolang flagged.
- Events: `PlayerMoveEvent` hard-stop; `EntityDamageEvent` om root te breken als gewenst.
- Visuals/SFX: `VILLAGER_HAPPY`/`SLIME` of `BLOCK_MOSS_BREAK`; lianen-plaatsing als decor (tijdelijk).
- Balance: geen directe damage; sterke setup tool; cooldown hoger.
- Randvoorwaarden: niet toepassen op bosses; dispel door `MILK_BUCKET`?
- Pseudocode: flag target; onMove if flagged then setToFrom(); after duration unflag.

Polymorph
- Doel: tijdelijk uitschakelen door transform naar schaap.
- Input/Output: entity→sheep, behoud positie en naam.
- Mechanics: despawn originele mob (of hide/invuln), spawn `Sheep` met metadata link naar originele UUID; na duur revert: remove Sheep, respawn/re-enable originele met health snapshot.
- Events: `EntityDeathEvent` van sheep map je terug naar originele als “kill”.
- Visuals/SFX: `SPELL_MOB`/`POOF`; `ENTITY_SHEEP_AMBIENT`.
- Balance: geen damage; harde CC; korte duur (3–5s) en dispellable.
- Randvoorwaarden: niet op spelers tenzij gamemode-acceptabel; drop/aggro state bewaren.
- Pseudocode: snapshot(orig); removeOrHide(orig); sheep=spawnSheep(tag=link); schedule revert.

Ethereal Form
- Doel: tijdelijk door mobs/players heen lopen en valdamage negeren.
- Input/Output: buff op caster.
- Mechanics: `player.setCollidable(false)`; `player.addPotionEffect(SLOW_FALLING)` of custom valdamage-cancel; toggel `noClip` niet beschikbaar in Bukkit, dus gebruik collision ignore lijsten en `EntityDamageEvent` cancel voor fall.
- Events: `EntityDamageEvent` (FALL) cancel; `PlayerToggleSneakEvent` kan als exit.
- Visuals/SFX: `SPELL_INSTANT` aura, `PHANTOM` geluid.
- Balance: mobiliteit en ontsnapping; geen attack tijdens effect (disable damage output).
- Randvoorwaarden: disable block-interact; eindigt bij schade ontvangen?
- Pseudocode: applyState(nonCollidable,true); onEnd: revert state.

Categorie: Utility & Healing

LifeSteal
- Doel: sustain door damage te converteren naar heal.
- Input/Output: projectile hit → heal caster.
- Mechanics: custom Snowball met tag; op `ProjectileHitEvent` bereken damage en `caster.setHealth(min(max, health + damage/2))`.
- Events: `ProjectileHitEvent`, `EntityDamageByEntityEvent` voor exacte damage.
- Visuals/SFX: rode partikeltrail, `ENTITY_PLAYER_BURP` zacht of `VEX_CHARGE`.
- Balance: ~3 harten damage, ~1.5 hart heal; reward agressie.
- Randvoorwaarden: cap op overheal; geen effect op undead (optioneel).
- Pseudocode: onHit: if isTaggedSnowball then dealDamage(); healCaster(dmg*0.5).

Heal
- Doel: directe genezing, no-nonsense.
- Input/Output: instant health.
- Mechanics: `player.setHealth(min(max, health + 8.0))` (4 harten); immuniteit-frames 5 ticks om chain damage te dempen.
- Events: none, behalve geluid/particles.
- Visuals/SFX: `HEART` + `ENTITY_PLAYER_LEVELUP`.
- Balance: geen offensief voordeel; matige cooldown.
- Randvoorwaarden: respecteer combat-tag systemen.
- Pseudocode: onCast: heal(8.0); playFX()

Implementatienoten
- Metadata tagging: gebruik `PersistentDataContainer` of `Projectile#setCustomName` + invisible voor koppeling.
- Cooldowns: centraal beheren per spell key in een `CooldownService` met ticks.
- Config: schade, duur, radius en flags (block damage, friendly fire) in `config.yml`.
- Veiligheid: check `WorldGuard`/region hooks indien gebruikt; null-check targets; NPE’s voorkomen.
- Testing checklist: cast zonder target, cast met target achter muur, PVP aan/uit, in water, tegen undead.


Lore-Integratie: Empire, The Kingdom en Elementos
- Context: de EmpireWand is geïnspireerd op The Kingdom roleplay/PvP-servers (o.a. DusDavidGames, Empire/Entropia/Kingdom 2). Onderstaande uitbreidingen verbinden mechanics met thematiek/namen zoals Jenava, Cemal en RAGTHANATOS zodat je spells ook “in-lore” kloppen.
- Elementos: eenvoudig element-systeem (Vuur, Water, Aarde, Lucht, Licht, Schaduw) dat schade-types, weerstanden en VFX bepaalt. Koppeling via `wand.element` en `spell.elementMask`.
  - Vuur: extra DoT, ontsteekt entities zonder block fire; partikel `FLAME`/`LAVA`.
  - Water: slow + uitblussen vuur; partikel `WATER_SPLASH`.
  - Aarde: knockback-reductie + korte root; partikel `BLOCK_DUST(DIRT/STONE)`.
  - Lucht: hogere projectile-speed en val-immu; partikel `CLOUD`.
  - Licht: bonus vs undead, kleine heal pulse; partikel `SPELL_INSTANT`/`GLOW`.
  - Schaduw: lifesteal-opschaling in duisternis; partikel `SMOKE_NORMAL`.
- Fracties & namen: Entropia (chaos/schaduw), Empire (orde/licht), Kingdom (balans). NPC/figuren: Jenava (healing/protectie), Cemal (defensief/aarde), RAGTHANATOS (doom/schaduw), “DusDavid” (rally/buff).

Elementos – Systeemhooks
- Damage tags: voeg `DamageCause.CUSTOM("element:<name>")` of metadata toe zodat andere plugins kunnen reageren.
- Weerstanden: `ConfigService` map met per element `multiplier` (bv. undead: licht 1.25x, schaduw 0.8x vs undead).
- UI: toon element op item-lore en actionbar bij cast.

Categorie: Elemental Core Spells

Elemental Surge (Empire)
- Doel: basale ranged nuke die per element anders schaalt.
- Input/Output: rechtlijnig projectiel → enkeldoel damage + lichte bijwerking per element.
- Mechanics: `Snowball`/`SmallFireball` afhankelijk van element; bij hit: baseDamage 5.0, mod via `elementMultiplier` en side-effect (DoT/Slow/Glow/Knockback-reduce/etc.).
- Events: `ProjectileHitEvent` met metadata `spell=elemental_surge` en `element`.
- Visuals/SFX: partikel afhankelijk van element; geluid `ENTITY_ILLUSIONER_CAST_SPELL` of element-variant (`BLAZE_SHOOT` voor vuur).
- Balance: middenklasse nuke; cooldown ~6–8s; side-effects klein maar voelbaar.
- Randvoorwaarden: friendly-fire regels volgen; geen block fire.
- Pseudocode: cast→spawn projectile(element); onHit→calcDamage(base*mult); applySmallSideEffect(element).

Entropic Rift (Entropia)
- Doel: area denial door een schaduw-rift die slow + DoT geeft.
- Input/Output: AoE op grond, duurt 5s.
- Mechanics: target block raycast; spawn onzichtbare armorstand anchor; elke 10t: entiteiten in radius 3 krijgen `SLOW I (20t)` en `damage 1.0` schaduw.
- Events: scheduler tick; cancel bij `WorldChange`/`Unload`.
- Visuals/SFX: `SMOKE_LARGE`, `SOUL`-achtige particle cirkel; zacht `WITHER_AMBIENT`.
- Balance: lage TotD maar sterke zone-control; cooldown 18–22s.
- Randvoorwaarden: niet stacken; één rift per caster.
- Pseudocode: placeAnchor(); repeat 10x every 10t: affectEntitiesInRadius(); cleanup.

Jenava’s Grace (Empire/Kingdom)
- Doel: burst heal + korte damage-reduction aura voor allies.
- Input/Output: aoe heal 4 harten + DR 20% voor 6s binnen 6 blok.
- Mechanics: party/kingdom detectie via teams/scoreboard of permission-groep; status `metadata:grace` met eindtijd; DR toegepast in `EntityDamageEvent` voor flagged targets.
- Events: `EntityDamageEvent` voor DR, scheduler voor expiratie.
- Visuals/SFX: `HEART`, `GLOW` kort voor allies, `ENTITY_PLAYER_LEVELUP`.
- Balance: sterke support; cooldown 25–30s; geen stack met Heal spell-effect (neem hoogste).
- Randvoorwaarden: geen effect op vijanden; PVP respect.
- Pseudocode: healAllies(radius,8.0); flagDR(allies,0.8,6s); fx().

Cemal’s Bulwark (Aarde/Defensief)
- Doel: tijdelijke stenen koepel + knockback-reduce binnen koepel.
- Input/Output: spawnt tijdelijke barrier (client-side via falling blocks of echte blocks als server toelaat) 5s.
- Mechanics: bij voorkeur visuele koepel met `BlockDisplay`/`FallingBlock` en collision-flag; binnen radius: `RESISTANCE I` en `KNOCKBACK_RESISTANCE` attribuut 0.5.
- Events: cleanup scheduler; `PlayerMoveEvent` om inside/outside te tracken.
- Visuals/SFX: `BLOCK_CRACK(STONE)`, `BASALT_PLACE`; subtiele `SHIELD_BLOCK`.
- Balance: hoge utility; cooldown 35s; geen volledige afscherming tegen projectielen.
- Randvoorwaarden: geen permanente block-place; worldguard-safe.
- Pseudocode: spawnDomeDisplays(); applyBuffsInside(); after 5s removeDisplays(); unbuff().

Judgement of RAGTHANATOS (Schaduw/Execute)
- Doel: execute-achtige single-target nuke die sterker wordt als target low is.
- Input/Output: straal op target binnen 18 blok; damage schalen op ontbrekende health.
- Mechanics: bereken `missing = maxHealth - health`; `damage = 2.0 + missing * 0.35` met cap; extra 25% in duisternis (lichtniveau < 5).
- Events: line-of-sight check; `EntityDamageByEntityEvent`.
- Visuals/SFX: donkere beam `SMOKE_NORMAL` + `SOUL_FIRE_FLAME`; geluid `WITHER_SHOOT`.
- Balance: dreigend, maar counterable via licht, dodge, immuniteiten; cooldown 28–32s.
- Randvoorwaarden: niet op bosses tenzij expliciet.
- Pseudocode: t=findTarget(); if los(t) dealScaledShadowDamage(t).

DusDavid’s Rally (Empire/Buff)
- Doel: korte team-rally: speed + attack speed buff.
- Input/Output: allies binnen 8 blok krijgen `SPEED I (8s)` en `HASTE I (tools)` of attribuut-aanpassing voor aanvalssnelheid (paper API).
- Mechanics: gebruik teams of same-faction check; toon actionbar “Rally!” met resterende duur.
- Events: scheduler tick voor HUD; remove on end.
- Visuals/SFX: `NOTE` stijgende toon, `VILLAGER_HAPPY`.
- Balance: goede engage tool; cooldown 22–26s.
- Randvoorwaarden: geen effect op vijanden; conflict met andere speed-buffs: kies hoogste.
- Pseudocode: buffAllies(radius); startHUD(); scheduleClear().

Empire Seal (Licht/Control)
- Doel: heilige zegel dat 1 target stillegt als het cast (silence/interupt).
- Input/Output: projectile of beam; bij hit: `SILENCE`-achtige status 3s (blokkeer spell casts via CooldownService check + flag).
- Mechanics: voeg `isSilenced(uuid)` gating in `EmpireWandCommand`/cast entry.
- Events: custom `SpellCastEvent` afbreken indien `isSilenced`.
- Visuals/SFX: `SPELL_INSTANT`, gouden runes met `ENCHANTMENT_TABLE`.
- Balance: sterk vs casters; cooldown 18s; werkt niet op non-casters.
- Randvoorwaarden: duidelijk feedback aan target (title/subtitle).
- Pseudocode: if hitLiving then setSilenced(target,3s).

Kingdom Recall (Utility/Teleport)
- Doel: terugroep naar dichtstbijzijnde kingdom-outpost/beacon.
- Input/Output: channel 3s → teleport, cancel bij damage of bewegen > 0.3 blok.
- Mechanics: vind `nearestOutpostLoc(faction)`; gebruik `PlayerTeleportEvent` met `TeleportCause.PLUGIN`; set temp invuln 1s na aankomst.
- Events: `PlayerMoveEvent` (cancel), `EntityDamageEvent` (interrupt).
- Visuals/SFX: `PORTAL`, `ENDERMAN_TELEPORT`.
- Balance: sterke macro; cooldown 60–90s.
- Randvoorwaarden: verboden in combat-tag; niet in vijandige region.
- Pseudocode: beginChannel(); if not interrupted after 60t then teleport().

Entropia Collapse (AoE/Finisher)
- Doel: trage implosie die vijanden naar midden trekt en eindburst geeft.
- Input/Output: ground-targeted; 2.5s pull, dan burst 5 harten schaduw.
- Mechanics: elke 5t: apply vector naar center met sterkte ~0.4; bij einde `damage` in radius 4; caster immuun voor pull.
- Events: tick scheduler; knockback resist respecteren.
- Visuals/SFX: `REVERSE_PORTAL`, `SOUL` swirl, `WITHER_SPAWN` zacht.
- Balance: sterk als setup met Vines; cooldown 35–40s.
- Randvoorwaarden: geen fall damage toevoegen; geen block-grief.
- Pseudocode: createCenter(); tickPull(); finalBurst(); cleanup().

Implementatiekoppelingen in codebase
- `SpellRegistry` koppelt `id`→klasse: voeg nieuwe ids (`elemental_surge`, `jenavas_grace`, etc.).
- `ConfigService`: per spell: cooldown, baseDamage, radius, elementMultipliers.
- `EmpireWandCommand`: voeg `silence`/channel-interrupt checks (voor Seal/Recall).
- `FxService`: voeg helpers voor element-particles en beams.
- `spells.yml`: definities per spell + beschrijving (voor lore/tooltip).

Voorbeeld `spells.yml` entries (schets)
- elemental_surge:
  - element: FIRE|WATER|EARTH|AIR|LIGHT|SHADOW
  - baseDamage: 5.0
  - cooldown: 7
  - sideEffects:
    - fire: dot: 2 over 3s
    - water: slow: 20t amp 1
    - earth: kbReduce: 0.3 dur: 3s
- jenavas_grace:
  - heal: 8.0
  - damageReduction: 0.2
  - duration: 6s
  - radius: 6
  - cooldown: 28

Testscenario’s (Kingdom/Empire thematiek)
- Entropia-combo: Grasping Vines → Entropic Rift → Entropia Collapse.
- Empire-support: Jenava’s Grace → DusDavid’s Rally → Magic Missile follow-up.
- Shadow-execute: LifeSteal om te sustainen → Judgement of RAGTHANATOS op low target.
