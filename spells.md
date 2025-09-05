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
