# 🌟 EmpireWand - Spell Design Guide

> **Doel:** Snelle, consistente referentie voor het ontwikkelen van spells. Elke spell volgt dezelfde gestructureerde aanpak voor optimale implementatie.

## 📚 Inhoudsopgave
- [📖 Terminologie](#-terminologie)
- [⚔️ Direct Damage Spells](#️-direct-damage-spells)
- [🛡️ Utility & Healing Spells](#️-utility--healing-spells)
- [🚀 Next Coming Spells](#-next-coming-spells)

## 📖 Terminologie
- **❤️ Hartje:** 2.0 health (1 hart)
- **⏱️ Tick:** 1/20e seconde
- **🎯 Caster:** De speler die de spell cast
- **🎯 Target:** Entiteit die geraakt wordt

## ⚔️ Direct Damage Spells

### 🔥 Comet
**Doel:** Snelle AoE projectile voor algemene combat situaties.

**📥 Input/Output:**
- **Input:** Cast zonder target
- **Output:** Explosie bij impact

**⚙️ Mechanics:**
```java
player.launchProjectile(Fireball.class)
fireball.setYield(2.5F)
fireball.setIsIncendiary(false)
```

**🎯 Events:**
- `ProjectileHitEvent` voor impact en schade-afhandeling
- Metadata tag `spell=comet`

**✨ Visuals/SFX:**
- `SOUL_FIRE_FLAME` trail particles
- `EXPLOSION_NORMAL` bij impact
- `ENTITY_BLAZE_SHOOT` cast geluid

**⚖️ Balance:**
- ~3.5 harten schade in centrum
- Licht falloff mogelijk
- Medium range, hoge snelheid

**📋 Randvoorwaarden:**
- Respecteert PVP en region flags
- Geen block damage indien gewenst
- Caster immuniteit

**💻 Pseudocode:**
```java
onCast:
    fireball = launch Fireball
    fireball.setIsIncendiary(false)
    setYield(2.5)
    tag(fireball, "comet")

onTick (2 ticks interval):
    spawn trail bij fireball.loc

onHit (if tag==comet):
    doExplosionDamage(no block break)
```

---

### 💥 Explosive
**Doel:** Trage, zware AoE burst voor verdedigingsbreuk en structureel damage.

**📥 Input/Output:**
- **Input:** Cast richting
- **Output:** Grote explosie on hit

**⚙️ Mechanics:**
```java
player.launchProjectile(WitherSkull.class)
world.createExplosion(loc, 4.0F, false, false)
```

**🎯 Events:**
- `ProjectileHitEvent` voor impact
- Metadata tag `spell=explosive`

**✨ Visuals/SFX:**
- `SMOKE_LARGE` particles
- `EXPLOSION_HUGE` bij impact
- `ENTITY_GENERIC_EXPLODE` geluid

**⚖️ Balance:**
- ~6 harten schade in centrum
- Makkelijk te ontwijken door lage snelheid
- Friendly-fire mogelijk

**📋 Randvoorwaarden:**
- Toggle voor block damage (config)
- Immuniteit voor caster (4 ticks)
- Werkt op mobs en players

**💻 Pseudocode:**
```java
onCast:
    skull = launch WitherSkull
    tag(skull)

onHit:
    createExplosion(no fire, no block break)
    applyAoEDamage()
```

---

### 🔮 Magic Missile
**Doel:** Gegarandeerde hit met lage burst damage - perfect tegen snelle targets.

**📥 Input/Output:**
- **Input:** Auto-target of crosshair target
- **Output:** 3 hits met korte delay

**⚙️ Mechanics:**
```java
target = player.getTargetEntity(range)
scheduler: 3x target.damage(3.0, caster) met 7 ticks delay
```

**🎯 Events:**
- Geen speciale projectile events
- `EntityDamageByEntityEvent` voor modifiers

**✨ Visuals/SFX:**
- `ENCHANTMENT_TABLE` particle straal
- `ENTITY_ILLUSIONER_CAST_SPELL` cast sound
- Beam effect tussen caster en target

**⚖️ Balance:**
- 3 × 1.5 harten = totaal ~4.5 damage
- Werkt tegen snelle/evasieve targets
- Lage individuele damage

**📋 Randvoorwaarden:**
- Line-of-sight check per salvo
- Niet door muren (tenzij feature)
- Target validation per hit

**💻 Pseudocode:**
```java
onCast:
    lock target
    for i in 1..3:
        schedule after i*7ticks:
            if valid(target):
                damage(target, 3.0)
                drawBeam()
```

---

### ❄️ Glacial Spike
**Doel:** Damage plus sterke slow effect - ideale control tool.

**📥 Input/Output:**
- **Input:** Cast richting
- **Output:** Impact damage + slow debuff

**⚙️ Mechanics:**
```java
proj = launch Arrow
onHit: damage(8.0) + SLOW(80ticks, amplifier=2)
```

**🎯 Events:**
- `ProjectileHitEvent` voor impact
- Collision check per tick

**✨ Visuals/SFX:**
- `SNOWFLAKE` trail particles
- `BLOCK_ICE_BREAK` bij impact
- Koude ambient sounds

**⚖️ Balance:**
- ~4 harten damage + SLOW II (4s)
- Sterk in duels en kiting
- Medium range

**📋 Randvoorwaarden:**
- Max één Spike per caster tegelijk
- Line-of-sight vereist
- Target immuniteit na hit

**💻 Pseudocode:**
```java
onCast:
    proj = launch Arrow
    tag(proj)

onHit:
    applyDamageAndSlow()
```

---

### 🌿 Grasping Vines
**Doel:** Root/immobilize voor korte duur - setup tool voor combos.

**📥 Input/Output:**
- **Input:** Cast op target area
- **Output:** AoE slow/root effect

**⚙️ Mechanics:**
```java
apply SLOWNESS(level=250) voor 60 ticks
optional: ROOTED logica via PlayerMoveEvent
```

**🎯 Events:**
- `PlayerMoveEvent` voor hard-stop
- `EntityDamageEvent` om root te breken

**✨ Visuals/SFX:**
- `BLOCK_MOSS_BREAK` particles
- Lianen als tijdelijke decor
- Nature ambient sounds

**⚖️ Balance:**
- Geen directe damage
- Sterke setup tool voor combos
- Hoge cooldown

**📋 Randvoorwaarden:**
- Niet toepassen op bosses
- Dispellable door milk bucket
- Werkt op players en mobs

**💻 Pseudocode:**
```java
onCast:
    flag target
    applySlowness(250, 60ticks)

onMove (if flagged):
    setToFrom() // Cancel movement

after duration:
    unflag target
```

---

### 🐑 Polymorph
**Doel:** Tijdelijk uitschakelen door transform naar schaap.

**📥 Input/Output:**
- **Input:** Target entity
- **Output:** Entity → Sheep transform

**⚙️ Mechanics:**
```java
hide original entity
spawn Sheep met metadata link
schedule revert na duration
```

**🎯 Events:**
- `EntityDeathEvent` van sheep maps terug
- Duration-based revert

**✨ Visuals/SFX:**
- `SPELL_MOB` particles
- `ENTITY_SHEEP_AMBIENT` sounds
- `POOF` transform effect

**⚖️ Balance:**
- Geen damage, pure CC
- Korte duur (3-5s)
- Dispellable

**📋 Randvoorwaarden:**
- Niet op players (tenzij gamemode)
- Bewaart aggro state
- Health snapshot voor revert

**💻 Pseudocode:**
```java
onCast:
    snapshot(original)
    removeOrHide(original)
    sheep = spawnSheep(tag=link)
    schedule revert(duration)
```

---

### 👻 Ethereal Form
**Doel:** Tijdelijk door mobs/players heen lopen en valdamage negeren.

**📥 Input/Output:**
- **Input:** Cast op zelf
- **Output:** Collision disable + fall damage immunity

**⚙️ Mechanics:**
```java
player.setCollidable(false)
player.addPotionEffect(SLOW_FALLING)
cancel EntityDamageEvent(FALL)
```

**🎯 Events:**
- `EntityDamageEvent` (FALL) cancel
- `PlayerToggleSneakEvent` voor manual exit

**✨ Visuals/SFX:**
- `SPELL_INSTANT` aura particles
- `PHANTOM` ambient sound
- Ghostly transparency effect

**⚖️ Balance:**
- Mobiliteit en ontsnapping
- Geen attack tijdens effect
- Medium duration

**📋 Randvoorwaarden:**
- Disable block interaction
- Eindigt bij ontvangen damage
- No-clip simulatie

**💻 Pseudocode:**
```java
onCast:
    applyState(nonCollidable=true)
    addSlowFalling(duration)

onEnd:
    revert state
```

---

## 🛡️ Utility & Healing Spells

### 🩸 LifeSteal
**Doel:** Sustain door damage te converteren naar healing.

**📥 Input/Output:**
- **Input:** Cast richting
- **Output:** Projectile hit → heal caster

**⚙️ Mechanics:**
```java
snowball = launch Snowball
onHit: damage + healCaster(damage * 0.5)
```

**🎯 Events:**
- `ProjectileHitEvent` voor impact
- `EntityDamageByEntityEvent` voor exact damage

**✨ Visuals/SFX:**
- Rode particle trail
- `VEX_CHARGE` impact sound
- Healing particles bij caster

**⚖️ Balance:**
- ~3 harten damage, ~1.5 hart heal
- Rewards agressie
- Medium range

**📋 Randvoorwaarden:**
- Cap op overheal
- Optioneel: geen effect op undead
- Self-damage protection

**💻 Pseudocode:**
```java
onHit:
    if isTaggedSnowball:
        dealDamage()
        healCaster(dmg * 0.5)
```

---

### ❤️ Heal
**Doel:** Directe genezing - no-nonsense health restore.

**📥 Input/Output:**
- **Input:** Cast op zelf
- **Output:** Instant health boost

**⚙️ Mechanics:**
```java
player.setHealth(min(max, health + 8.0))
```

**🎯 Events:**
- Geen speciale events
- Particle/sound effects

**✨ Visuals/SFX:**
- `HEART` particles
- `ENTITY_PLAYER_LEVELUP` sound
- Healing aura effect

**⚖️ Balance:**
- 4 harten healing
- Geen offensief voordeel
- Matige cooldown

**📋 Randvoorwaarden:**
- Respect combat-tag systemen
- Immuniteit frames (5 ticks)
- Max health cap

**💻 Pseudocode:**
```java
onCast:
    heal(8.0)
    playFX()
```

---

### 🦘 Leap
**Doel:** Snelle mobiliteit boost voor positioning en ontsnapping.

**📥 Input/Output:**
- **Input:** Cast richting
- **Output:** Velocity boost in kijkrichting

**⚙️ Mechanics:**
```java
direction = player.getDirection().normalize()
player.setVelocity(direction * multiplier + verticalBoost)
```

**🎯 Events:**
- Geen speciale events
- Instant effect

**✨ Visuals/SFX:**
- `CLOUD` burst particles
- `ENTITY_RABBIT_JUMP` sound
- Motion trail effect

**⚖️ Balance:**
- ~1.5x snelheid boost
- Korte cooldown voor frequente gebruik
- Vertical boost optioneel

**📋 Randvoorwaarden:**
- Geen collision checks
- Werkt in lucht en op grond
- Direction-based velocity

**💻 Pseudocode:**
```java
onCast:
    dir = player.getDirection().normalize()
    dir.y += verticalBoost
    player.setVelocity(dir * multiplier)
    playFX()
```

---

## 🚀 Next Coming Spells

> **Deze spells zijn gepland voor toekomstige implementatie**

### 🌑 Dark Circle
**Doel:** Grote AoE crowd control voor groepen vijanden - belegeringsspell.

**📥 Input/Output:**
- **Input:** Cast op locatie
- **Output:** Cirkel die vijanden naar midden trekt en omhoog gooit

**⚙️ Mechanics:**
```java
entities = world.getNearbyEntities(caster, radius)
pullEntitiesToCenter()
scheduleLaunch(upPower)
```

**🎯 Events:**
- `EntityDamageEvent` voor valdamage
- Scheduler voor pull→launch sequence

**✨ Visuals/SFX:**
- `SMOKE_LARGE` cirkel particles
- `ENTITY_ENDER_DRAGON_FLAP` pull sound
- `ENTITY_GENERIC_EXPLODE` launch effect

**⚖️ Balance:**
- Radius ~8 blokken
- Launch height ~15 blokken
- Hoge cooldown voor balans

**📋 Randvoorwaarden:**
- Respect PVP flags
- Caster immuniteit
- Alleen op grond

**💻 Pseudocode:**
```java
onCast:
    createCircle(radius)
    pullEntitiesToCenter()
    scheduleLaunch(upPower)
```

---

### 🌊 Dark Pulse
**Doel:** Ranged golf aanval met wither effect - sustained damage.

**📥 Input/Output:**
- **Input:** Cast richting
- **Output:** Golf die vooruit gaat en targets withert

**⚙️ Mechanics:**
```java
launchPulse(direction)
onHit: target.addPotionEffect(WITHER, duration, amplifier)
```

**🎯 Events:**
- `ProjectileHitEvent` of area scan
- Wither effect application

**✨ Visuals/SFX:**
- `SMOKE_NORMAL` trail
- `ENTITY_WITHER_SHOOT` cast sound
- `ENTITY_WITHER_HURT` impact

**⚖️ Balance:**
- Range ~20 blokken
- Wither damage ~1 hart per 2s
- Medium cooldown

**📋 Randvoorwaarden:**
- Line-of-sight vereist
- Friendly-fire toggle
- Duration-based effect

**💻 Pseudocode:**
```java
onCast:
    launchPulse(direction)

onHit:
    applyWitherEffect()
```

---

### 💠 Aura
**Doel:** Passieve damage aura rondom caster - gebiedscontrole.

**📥 Input/Output:**
- **Input:** Cast op zelf
- **Output:** Continue damage aan nearby entities

**⚙️ Mechanics:**
```java
startAuraScheduler()
onTick: damageNearbyEntities()
```

**🎯 Events:**
- `PlayerMoveEvent` voor range checks
- `EntityDamageEvent` voor custom damage

**✨ Visuals/SFX:**
- `PORTAL` ambient particles
- `BLOCK_BEACON_AMBIENT` hum sound
- Dark energy aura effect

**⚖️ Balance:**
- Radius ~5 blokken
- Damage ~2 per seconde
- Lange cooldown

**📋 Randvoorwaarden:**
- Caster immuniteit
- Eindigt bij movement of damage
- Duration-based

**💻 Pseudocode:**
```java
onCast:
    startAuraScheduler()

onTick:
    damageNearbyEntities()
```

---

Empire Launch
- Doel: krachtige launch voor crowd control.
- Input/Output: input = target entity; output = entity gelanceerd omhoog.
- Mechanics: `target.setVelocity(upVector * power)`; optioneel richting component.
- Events: `EntityDamageEvent` voor valdamage; target validation.
- Visuals/SFX: `ENTITY_FIREWORK_ROCKET_LAUNCH` sound, `FIREWORK` particles.
- Balance: launch power ~2.0; valdamage ~10 harten; medium cooldown.
- Randvoorwaarden: target moet in range zijn; werkt op mobs/players.
- Pseudocode: onCast: if targetValid then target.setVelocity(upVector * power)

Confuse
- Doel: disorientatie en slow effect voor control.
- Input/Output: input = target; output = confusion + slow + damage.
- Mechanics: `target.addPotionEffect(CONFUSION, duration)` + `SLOW`; direct damage component.
- Events: `EntityDamageByEntityEvent` voor damage; potion effect application.
- Visuals/SFX: `ENTITY_PLAYER_ATTACK_NODAMAGE` dizzy sound, `SPELL_MOB_AMBIENT` particles.
- Balance: duration ~4s; slow amplifier 2; damage ~3 harten.
- Randvoorwaarden: line-of-sight vereist; werkt op players/mobs.
- Pseudocode: onCast: applyConfusion(target); applySlow(target); dealDamage(target)

Teleport
- Doel: instant positionering voor mobiliteit.
- Input/Output: input = target location; output = caster teleport naar locatie.
- Mechanics: `player.teleport(targetLocation)` met range en line-of-sight checks.
- Events: `PlayerTeleportEvent` voor validation; location safety checks.
- Visuals/SFX: `ENTITY_ENDERMAN_TELEPORT` sound, `PORTAL` particles bij cast en aankomst.
- Balance: range ~15 blokken; korte cooldown; geen damage.
- Randvoorwaarden: destination moet veilig zijn; geen teleport door muren.
- Pseudocode: onCast: if validLocation then player.teleport(location)

Thunder Blast
- Doel: bliksem AoE voor openingsaanvallen.
- Input/Output: input = cast; output = lightning explosie op targets.
- Mechanics: `world.strikeLightning(targetLoc)` met custom damage; AoE scan voor multiple targets.
- Events: `LightningStrikeEvent` voor custom handling; area damage application.
- Visuals/SFX: `ENTITY_LIGHTNING_BOLT_THUNDER` sound, `ELECTRIC_SPARK` particles.
- Balance: radius ~6 blokken; damage ~8 harten; lange cooldown.
- Randvoorwaarden: werkt alleen buiten; kan block damage veroorzaken.
- Pseudocode: onCast: strikeLightningArea(center, radius)

Lightning Bolt
- Doel: gerichte bliksem aanval voor precision damage.
- Input/Output: input = target; output = lightning strike op target.
- Mechanics: `world.strikeLightning(target.getLocation())`; custom damage multiplier.
- Events: `LightningStrikeEvent` voor damage control; target validation.
- Visuals/SFX: `ENTITY_LIGHTNING_BOLT_IMPACT` sound, `ELECTRIC_SPARK` trail.
- Balance: damage ~12 harten; kans op fire; medium cooldown.
- Randvoorwaarden: line-of-sight vereist; target moet exposed zijn.
- Pseudocode: onCast: if targetExposed then world.strikeLightning(targetLoc)

Fireball
- Doel: explosieve projectile voor ranged damage.
- Input/Output: input = cast richting; output = fireball projectile met explosie.
- Mechanics: `player.launchProjectile(Fireball.class)` met custom yield en speed.
- Events: `ProjectileHitEvent` voor impact handling; explosion control.
- Visuals/SFX: `ENTITY_BLAZE_SHOOT` sound, `FLAME` trail particles.
- Balance: yield ~3.0; damage ~10 harten; medium cooldown.
- Randvoorwaarden: kan fire veroorzaken; block damage toggle.
- Pseudocode: onCast: fireball = launch Fireball; setYield(3.0); setIsIncendiary(true)

Explosion Trail
- Doel: bewegende explosieve zone voor area denial.
- Input/Output: input = cast; output = explosies volgen caster movement.
- Mechanics: scheduler die elke tick explosie creëert bij caster locatie.
- Events: `PlayerMoveEvent` voor trail updates; explosion events.
- Visuals/SFX: `ENTITY_GENERIC_EXPLODE` repeated, `EXPLOSION_NORMAL` particles.
- Balance: duration ~5s; damage ~4 per explosie; lange cooldown.
- Randvoorwaarden: caster immuniteit; geen block damage.
- Pseudocode: onCast: startTrailScheduler(); onTick: createExplosion(playerLoc)

Blaze Launch
- Doel: zelf-propulsie met vuur effecten.
- Input/Output: input = cast richting; output = caster velocity boost met vuur trail.
- Mechanics: `player.setVelocity(direction * power)` + fire effects op omgeving.
- Events: `PlayerMoveEvent` voor trail effects; velocity application.
- Visuals/SFX: `FLAME` particles, `ENTITY_BLAZE_AMBIENT` sound.
- Balance: power ~1.8; korte cooldown; geen self-damage.
- Randvoorwaarden: werkt in lucht; kan omgeving in brand zetten.
- Pseudocode: onCast: player.setVelocity(direction * power); startFireTrail()

Lightning Storm
- Doel: multiple lightning strikes in gebied.
- Input/Output: input = target area; output = meerdere bliksems in geselecteerd gebied.
- Mechanics: scheduler voor multiple `world.strikeLightning()` calls in radius.
- Events: `LightningStrikeEvent` voor elke strike; area selection.
- Visuals/SFX: `ENTITY_LIGHTNING_BOLT_THUNDER` repeated, `ELECTRIC_SPARK` storm.
- Balance: strikes ~5-8; radius ~10 blokken; zeer lange cooldown.
- Randvoorwaarden: alleen buiten; hoge mana cost.
- Pseudocode: onCast: for i in 1..strikes: scheduleLightning(randomLocInRadius)

*Deze spells zijn volledig uitgewerkt en klaar voor implementatie in toekomstige updates.* ✨



===================================================================================
Upcoming :

Little Spark

Doel: piepkleine poke op range; tikje/mini-knockback.

Input/Output: input = kijkrichting; output = licht elektrisch projectiel.

Mechanics: Snowball of SmallFireball met lage snelheid & mini-damage; geef een korte Vector impuls op hit. Snowball of smallfireball wel onzichtbaar maken en

Events: ProjectileHitEvent voor hit/knockback.

Visuals/SFX: Particle.ELECTRIC_SPARK + zacht bow/blaze-sound. 
hub.spigotmc.org
+1

Balance: 1–2 ♥; knockback klein; cd 0.75s.

Randvoorwaarden: geen block damage.

Pseudocode:

onCast:
  proj = p.launchProjectile(Snowball)
  proj.setVelocity(p.getLocation().getDirection().multiply(0.8))
on ProjectileHit:
  if hitEntity: hitEntity.setVelocity(dirFromCaster.multiply(0.3))

Spark

Doel: zwaardere variant van Little Spark.

Mechanics: SmallFireball met iets hogere snelheid/yield=0 (geen explosie), extra knockback.

Events: ProjectileHitEvent.

Visuals/SFX: ELECTRIC_SPARK trail; ENTITY_BLAZE_SHOOT. 
hub.spigotmc.org
advancedplugins.net

Balance: 3–4 ♥; cd 1.5s.

Pseudocode: als Little Spark maar velocity*1.2 en knockback*1.5.

LightningArrow

Doel: pijl die bij impact bliksem triggert.

Mechanics: bij ProjectileHitEvent op Arrow: world.strikeLightning() of strikeLightningEffect() (schade vs. alleen effect). 
hub.spigotmc.org
helpch.at

Visuals/SFX: vanilla lightning; optioneel GLOWING op doelwit voor 3s. 
hub.spigotmc.org

Balance: 4–6 ♥ plus lightning; cd 3–5s.

Randvoorwaarden: optie “no-block-fire” via strikeLightningEffect. 
helpch.at

Pseudocode:

on ProjectileHit(Arrow):
  if blockDamageOff: world.strikeLightningEffect(hitLoc)
  else: world.strikeLightning(hitLoc)

PoisonWave

Doel: vergiftigende kegel/golf.

Mechanics: zoek entiteiten in kegel voor speler; geef PotionEffectType.POISON. 
Bukkit
helpch.at

Events: none (tick-timer intern).

Visuals/SFX: Particle.SPELL_MOB/ITEM_SLIME; zacht cave-sound.

Balance: Poison I, 5–7s; cd 6s; cone 60°, range 6.

Pseudocode:

targets = coneEntities(p, 6, 60deg)
for e in targets: e.addPotionEffect(POISON, 7*20, 0)

ExplosionWave

Doel: niet-projectiel AOE-explosie (boog/kegel) zonder block damage.

Mechanics: per target: custom knockback + World#createExplosion(loc, 1.5F, false, false) voor geluid/feel; of alleen velocity. 
hub.spigotmc.org

Events: optioneel EntityExplodeEvent cancel voor 100% zekerheid. 
Javatips.net

Visuals/SFX: EXPLOSION_NORMAL, ENTITY_GENERIC_EXPLODE. 
hub.spigotmc.org

Balance: 3–4 ♥; cd 5s; cone 70°, range 6–8.

Pseudocode:

for e in coneEntities(...):
  kb = e.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.9).setY(0.4)
  e.setVelocity(kb)  // knockback tips: normalize+multiply. :contentReference[oaicite:10]{index=10}
world.createExplosion(center, 1.5F, false, false)

FlameWave

Doel: vuurgolf; zet targets (kort) in brand.

Mechanics: cone-hit; setFireTicks(60–100); lichte damage. 
hub.spigotmc.org

Visuals/SFX: FLAME/SMOKE + blaze-shoot sound. 
hub.spigotmc.org

Balance: 2–3 ♥ + burn; cd 5s.

Pseudocode: gelijk aan PoisonWave maar met e.setFireTicks(80).

EmpireAura

Doel: aura rond caster: buff allies, debuff enemies.

Mechanics: start runTaskTimer (tick-loop); per tick scope 5–6 blokken, geef allies REGEN/SPEED I, enemies WEAKNESS I of GLOWING. 
docs.papermc.io
hub.spigotmc.org

Events: task stopt na duur of bij cancel.

Visuals/SFX: ring Particle.SPELL_WITCH of cirkel via spawnParticle(...). 
hub.spigotmc.org

Balance: duur 8s; cd 18s.

Pseudocode:

task = Bukkit.getScheduler().runTaskTimer(..., 0, 10) // 0.5s tick. :contentReference[oaicite:15]{index=15}
each tick: apply buffs/debuffs in radius; spawnParticle(RING)

Empire Levitate

Doel: target tijdelijk laten leviteren.

Mechanics: PotionEffectType.LEVITATION voor X ticks. 
hub.spigotmc.org

Visuals/SFX: CLOUD/ENCHANTMENT_TABLE.

Balance: 2–3s levitation; cd 10s; niet op bosses.

Pseudocode: e.addPotionEffect(new PotionEffect(LEVITATION, 60, 0, false, true)). 
leonardosnt.github.io

Blood Block (Ravasha)

Doel: 1e cast plaatst redstone-blok; 2e cast lanceert ’m richting mikpunt.

Mechanics: stateful: placeLoc onthouden. Tweede cast: spawn FallingBlock(REDSTONE_BLOCK) of BlockDisplay en geef velocity naar raytrace-hit. 
hub.spigotmc.org
+2
hub.spigotmc.org
+2

Visuals/SFX: REDSTONE (DustOptions donkerrood) trail. 
hub.spigotmc.org

Balance: impact 4–6 ♥ + mini-stun (knockup); cd 8s.

Pseudocode:

if !state.hasBlock: state.placeLoc = targetBlockPos; world.getBlockAt(...).setType(REDSTONE_BLOCK)
else:
  fb = world.spawnFallingBlock(state.placeLoc, Material.REDSTONE_BLOCK.createBlockData())
  aim = rayTraceFromPlayer(20).getHitPosition()  // raytrace. :contentReference[oaicite:20]{index=20}
  fb.setVelocity(aim.toVector().subtract(fb.getLocation().toVector()).normalize().multiply(1.2).setY(0.4))
  state.clear()


(Issue “Blood Block Spell” bestaat in EmpireWandPlus.) 
GitHub

EmpireComet

Doel: zware, tragere “comet” met grotere explosie.

Mechanics: Fireball/LargeFireball met verhoogde Explosive#setYield, evt. setIsIncendiary(false) en eigen createExplosion op impact voor controle. 
hub.spigotmc.org
+1

Visuals/SFX: FLAME + SMOKE_LARGE; explode-sound.

Balance: 7–10 ♥ in kleine radius; cd 9s.

Pseudocode:

fb = p.launchProjectile(LargeFireball); fb.setYield(3.5F); fb.setIsIncendiary(false)
on ProjectileHit(fb): world.createExplosion(hitLoc, 3.2F, false, blockDamageToggle)

Blood Spam

Doel: snelle burst van mini “blood” projectielen.

Mechanics: schedule 6–8 korte ticks die Snowball/SmallFireball schieten met REDSTONE (Dust) trail; lichte damage per hit. 
docs.papermc.io
hub.spigotmc.org

Visuals/SFX: “splatter” particles.

Balance: 0.5–1 ♥ per hit, dps via spam; cd 6s.

Pseudocode:

repeat 6 every 2t:
  proj = p.launchProjectile(Snowball); proj.setVelocity(dir*1.3)
  trail REDSTONE dust


(Issue “Blood Spam Spell” bestaat in EmpireWandPlus.) 
GitHub

EmpireLaunch

Doel: krachtige verticale/diagonale launch met val-bescherming.

Mechanics: player.setVelocity(dir.normalize().multiply(0.4).setY(1.0)) + SLOW_FALLING 4s. 
helpch.at

Visuals/SFX: CLOUD burst; elytra-whoosh.

Balance: cd 8s; valdamage mitigatie via effect.

Pseudocode: velocity + addPotionEffect(SLOW_FALLING, 80, 0).

EarthQuake

Doel: grondschok met AOE-knockback.

Mechanics: zoek entiteiten in 6–8 blokken; bereken vector van caster→target, normalize().multiply(strength).setY(0.3); zet velocity. 
Bukkit
SpigotMC

Visuals/SFX: BLOCK_CRACK/DUST, anvil/bass-sound.

Balance: 0–2 ♥ + knockback; cd 7s; werkt niet door muren.

Pseudocode:

for e in nearEntities(p, 7):
  v = e.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.1)
  e.setVelocity(v.setY(0.35))

EmpireEscape

Doel: verbeterde ontsnapping (blink/dash).

Mechanics: rayTraceBlocks max 12–16m; teleport naar veilige hit of fallback 6m vooruit; korte INVISIBILITY/SPEED. 
hub.spigotmc.org

Visuals/SFX: SMOKE_NORMAL puff; enderman-teleport sound.

Balance: cd 10s; reset fall damage.

Pseudocode: dest = safeRayEnd(...); p.teleport(dest); p.addPotionEffect(SPEED, 40, 0).

CometShower

Doel: salvo/regengebied van meerdere kometen.

Mechanics: schedule N keer LargeFireball/createExplosion op willekeurige punten binnen cirkel boven target; dalen met Vector(0, -speed, 0). 
docs.papermc.io
hub.spigotmc.org

Visuals/SFX: herhaalde explode-sounds; FLAME/LAVA drips.

Balance: 5–7 kometen; elk 3–4 ♥; cd 16s; block damage optioneel.

Pseudocode:

center = targetedLocation()
repeat 5 every 6t:
  drop = center.add(rand2D(radius)).add(0, 12, 0)
  world.createExplosion(drop, 2.6F, false, blockDamageToggle)

GodCloud

Doel: cosmetische “vlieg-wolk” onder goden tijdens vliegen.

Mechanics: zolang speler isFlying()/gliding: elke 2–3 ticks spawnParticle(Particle.CLOUD / CAMPFIRE_COSY_SMOKE) net onder voeten. 
hub.spigotmc.org

Events: task start bij enable, stopt bij land.

Visuals/SFX: continue CLOUD; zachte wind-sound.

Balance: alleen FX; performance-vriendelijke particle-count.

Pseudocode:

task.runTaskTimer(..., 0, 2):
  if p.isFlying(): world.spawnParticle(CLOUD, p.getLocation().add(0,-0.9,0), 6, 0.3,0.05,0.3)