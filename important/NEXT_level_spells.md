Helemaal goed! Ik heb **alle “Doel”-regels uitgebreid naar 2–3 zinnen** en verder de rest ongemoeid gelaten, zodat je dit 1-op-1 kunt overnemen.

---

# Artefact: **Zeis van Mephidantes**

### 1) Levenszuiger (Life Reap)

* **Doel:** Een brede scythe-sweep die vijanden in een boog raakt en hun levensenergie aftapt om jou te healen. Je gebruikt dit om tijdens trades langer overeind te blijven en druk te houden zonder terug te hoeven trekken. Vooral sterk tegen gegroepeerde tegenstanders of targets met weinig armor.
* **Input/Output:** input = melee cast; output = 5m boog-hit, dmg + lifesteal.
* **Mechanics:** zoek entiteiten in een 120° boog voor de speler; `damage()` per target; `player.setHealth(min(max, +heal))`.
* **Events:** none.
* **Visuals/SFX:** `SWEEP_ATTACK` + donkere `REDSTONE`-dust; `ENTITY_WITHER_SPAWN` zacht.
* **Balance:** 3–4♥ totaal; lifesteal 15–20%; cd 7s.
* **Randvoorwaarden:** geen block damage; bossen krijgen halvering lifesteal.
* **Pseudocode:**

  ```java
  targets = coneEntities(player, 5.0, 120);
  for (LivingEntity e : targets) e.damage(4.0, player);
  heal = 0.8 * targets.size(); // ~0.4–1.6♥
  player.setHealth(Math.min(player.getMaxHealth(), player.getHealth()+heal));
  spawnSweepParticles();
  ```

### 2) Ritueel van Ontering (Ritual of Unmaking)

* **Doel:** Je kanaliseert korte tijd om een rituele cirkel te vormen die daarna ontploft in een zware shadow-burst. Perfect om terrein te controleren, pushes te breken en vijanden te dwingen uit de zone te stappen. Het risk/reward-profiel is hoog: wordt je gecanceld, dan valt de burst weg.
* **Input/Output:** input = cast; output = 6m cirkel, burst dmg + `WEAKNESS` aan vijanden.
* **Mechanics:** `runTaskTimer` 40 ticks voor channel; cancel op hit; bij einde: dmg+effect in radius.
* **Events:** on cancel → 50% cd terug.
* **Visuals/SFX:** cirkel van `SOUL`/`SCULK`-particles; diepe bass.
* **Balance:** 6–8♥; `WEAKNESS I` 6s; cd 18s.
* **Randvoorwaarden:** pvp-vriendelijke toggles.
* **Pseudocode:**

  ```java
  startChannel(40t, onInterrupt, onFinish -> {
    for (e in near(6m)) { e.damage(8.0); e.addPotionEffect(WEAKNESS, 120, 0); }
    ringParticles();
  });
  ```

### 3) Zielsplinters (Soul Sever)

* **Doel:** Een snelle, schaduwachtige dash van enkele blokken die vijanden op je lijn snijdt en korte desoriëntatie veroorzaakt. Ideaal om door linies te breken, flanken te openen of een chase af te ronden. Je kunt ‘m defensief gebruiken als repositioning tool of offensief om kills te forceren.
* **Input/Output:** input = richting; output = dash + `SLOW/FATAL_POISON` light bleed.
* **Mechanics:** ray-sample elke 0.5b; raak entiteiten → mini-dmg + `NAUSEA` 2s; eindloc veilig checken.
* **Events:** none.
* **Visuals/SFX:** `SMOKE_LARGE` spoor; enderman-tp sound.
* **Balance:** 2–3♥ totaal; cd 8s.
* **Randvoorwaarden:** safe-dash (geen lava/blokken).
* **Pseudocode:** dash via `setVelocity()` of korte `teleport(dest)` + lijn-hit.

---

# Artefact: **BloedMagie**

### 1) Blood Tap

* **Doel:** Je offert een klein deel van je eigen HP op om tijdelijke “Blood Charges” te verkrijgen die je krachtig maken. Deze charges verhogen je damage of spell-power en zetten druk in gevechten waarin sustain belangrijker is dan brute burst. De vaardigheid beloont goede timing en risicobeheersing.
* **Input/Output:** input = self-cast; output = “Blood Charges” (max 5).
* **Mechanics:** `player.damage(1.0, null)` (bypass armor) + +1 charge; charges geven +5% dmg/spell-power elk, 10s.
* **Events:** on death → verwijder charges.
* **Visuals/SFX:** rode `REDSTONE`-dust om de speler; hartslag-sound.
* **Balance:** risico/risicoreward; cd 3s; max 5 charges.
* **Randvoorwaarden:** min-HP guard (niet onder 2♥).
* **Pseudocode:** store charges in metadata; decay via scheduler.

### 2) Blood Barrier

* **Doel:** Een pulserend bloedschild dat inkomende hits deels absorbeert en aanvallers lichte “thorns”-schade teruggeeft. Je gebruikt het om push-momenten te overleven en ruimte te winnen voor je team. Het schild is vooral waardevol tegen burst-composities en tijdens objective-contests.
* **Input/Output:** input = self; output = 6m aura 6s, reduce 30% dmg; aanvallers krijgen 0.5♥ terug-prik.
* **Mechanics:** `EntityDamageEvent` wrapper; check aura-active; modify damage.
* **Events:** on expire → kleine AOE prikkels.
* **Visuals/SFX:** `BARRIER`/rode swirl-particles.
* **Balance:** cd 16s.
* **Randvoorwaarden:** niet stacken met Absorption.
* **Pseudocode:** flag in metadata; revert on timer end.

### 3) Hemorrhage

* **Doel:** Een bewegings-gevoelige DoT die extra pijn doet wanneer het doel blijft rennen of springen. Daarmee straf je ontsnappingspogingen en kiting, en dwing je tegenstanders tot stilstand of defensieve cooldowns. Uitstekend om kills te zekeren na een initiatie.
* **Input/Output:** input = target; output = bleed 6s; +bonusdmg per 1m beweging.
* **Mechanics:** sla `lastLoc` op; elke 10t: als `dist>0.8` → extra 0.5♥.
* **Events:** remove on cleanse.
* **Visuals/SFX:** druppel-particles.
* **Balance:** basis 2♥ + max \~3♥ extra; cd 8s.
* **Randvoorwaarden:** machtig maar counterbaar.
* **Pseudocode:** small scheduled task per target.

### 4) Blood Nova

* **Doel:** Je ontketent een explosieve bloedgolf die al je charges verbruikt voor een krachtige AOE-burst zonder block-damage. Perfect om een gevecht te beslissen na een goed opgebouwde “Blood Tap”-stack. Werkt zowel als finisher in melee-scrums als om ruimte te creëren.
* **Input/Output:** input = self; output = 4m burst, dmg = 2♥ + 1♥ per charge; knockback.
* **Mechanics:** `createExplosion(..., false,false)` voor SFX of enkel velocity.
* **Events:** consume charges.
* **Visuals/SFX:** rood-zwarte explosie-mix.
* **Balance:** cd 12s; charges reset.
* **Randvoorwaarden:** pvp-vriendelijke toggles.
* **Pseudocode:** read charges; deal damage; charges=0.

---

# Artefact: **Chatage’s Ster**

### 1) Stralingsbaken (Radiant Beacon)

* **Doel:** Je plaatst een lichtbaken dat pulserend healt en negatieve effecten van bondgenoten zuivert, terwijl vijanden licht verschroeit. Dit creëert een veilige zone om in te vechten of te hergroeperen en dwingt de tegenpartij om terrein op te geven. Ideaal bij choke-points en control objectives.
* **Input/Output:** input = plaats baken 8s; output = elke 1s heal 0.5♥ allies + verwijder 1 neg. effect; vijanden ontvangen 0.5♥ dmg.
* **Mechanics:** spawn onzichtbare `ArmorStand` als anker; per tick `nearbyLiving()` en apply effects.
* **Events:** auto-despawn na duur.
* **Visuals/SFX:** `END_ROD`/`GLOW`-achtige beam (particles); zachte bell-sounds.
* **Balance:** cd 16s; max targets 8 per pulse.
* **Randvoorwaarden:** geen stacking met andere bakens.
* **Pseudocode:** scheduler die 8 pulsen uitvoert.

### 2) Lichtmuur (Lightwall)

* **Doel:** Een tijdelijke, lichtgevende barrière die projectielen tegenhoudt en melee-aanvallers licht terugduwt. Je gebruikt het om aanstormende charges te breken, zichtlijnen van boogschutters te sluiten en je team tijd te geven om te repositioneren. Combineert sterk met area-spells achter de muur.
* **Input/Output:** input = richtlijn; output = 6×3 wand voor 5s.
* **Mechanics:** rij **invisible ArmorStands** met `Marker` of tijdelijke `GLASS`-blocks; nabij wand: projectielen cancelen, melee mini-knockback + `BLINDNESS 1.5s`.
* **Events:** cleanup on expire.
* **Visuals/SFX:** `WHITE_ASH`/`GLOW` krullen; glas-ping.
* **Balance:** cd 18s; max 1 actieve per speler.
* **Randvoorwaarden:** region-protections respecteren; block-variant alleen met toggle.
* **Pseudocode:** place anchors; intercept projectiles; timer cleanup.

### 3) Zonschicht (Solar Lance)

* **Doel:** Een gefocusseerde, doorborende lichtstraal die meerdere targets op lijn raakt en kort laat oplichten. Je gebruikt het om priority targets te “markeren”, flinterdunne kansen te pakken op afstand en vijanden achter elkaar te straffen. Zeer effectief in smalle gangen en over bruggen.
* **Input/Output:** input = kijkrichting; output = 20b line-trace; hit → 3–5♥ + `GLOWING 3s`.
* **Mechanics:** raytrace; sample langs lijn; apply dmg/effect.
* **Events:** none.
* **Visuals/SFX:** `CRIT_MAGIC` + goudgele `REDSTONE` dust; lichte harp-toon.
* **Balance:** cd 10s; friendly-fire off.
* **Randvoorwaarden:** max 3 targets pierce.
* **Pseudocode:** iterate samples; affect first 3 entities.

---

## Praktische notities (engine)

* Gebruik `runTaskTimer` voor auras/bakens/channels; **geen blocking sleeps**.
* Voor **safe dashes/teleports**: raytrace, 2-blok-hoogte & vaste bodem check.
* Explosie-SFX zonder grief: `createExplosion(power, setFire=false, breakBlocks=false)`.
* Particles en sounds via `World#spawnParticle` / `Player#playSound`.

Wil je dat ik dit meteen omzet naar **Java stubs (Paper 1.20.6, Java 21)** en een **config.yml** met alle tunables (cooldowns, radii, toggles)?
Top! Ik heb **10 nieuwe, soortgelijke spells** bedacht in dezelfde dev-vriendelijke stijl. Ze zijn **niet canon** maar direct **bouwbaar** op Paper/Spigot 1.20.x (raytrace, particles, potion-effects, velocity, explosions). Onderaan vind je een beknopt blok met **API-referenties** die ik erbij heb gepakt. citeturn0search12turn0search7turn0search3

---

## 1) Sunburst Step
- **Doel:** Een korte licht-dash die je 8–10 blokken verplaatst en op vertrek- of aankomstpunt een stralingspuls achterlaat. Ideaal om boogschutters te flanken of om even ademruimte te creëren in een teamfight. De puls healt allies licht en “seart” vijanden heel kort.
- **Input/Output:** input = kijkrichting; output = veilige teleport ≤10b + radiant pulse (r=3.5).  
- **Mechanics:** `rayTraceBlocks` voor bestemming; `player.teleport(dest)`; pulse: scan radius → allies heal 0.5♥, enemies damage 0.5–1♥.  
- **Events:** optioneel `PlayerTeleportEvent` (cause=PLUGIN).  
- **Visuals/SFX:** `END_ROD` & goudgele `REDSTONE` dust; `ENTITY_ENDERMAN_TELEPORT`. citeturn0search7  
- **Balance:** cd 7s; i-frames 6t.  
- **Randvoorwaarden:** safe-TP check (2 blok hoog, vaste vloer).  
- **Pseudocode:** teleport + radius scan → apply heal/dmg.

---

## 2) Crimson Chains
- **Doel:** Werp bloed-kettingen die het eerste doel vastzetten en een korte **pull** geven. Perfect om targets uit positie te trekken of een vluchtende vijand te “clippen” vóór een team-burst. In small choke-points is de druk maximaal.
- **Input/Output:** input = richtlijn; output = hit → root 1.5s + mini-pull (2–3b).  
- **Mechanics:** schiet `Snowball`; on hit: `SLOWNESS` II 2s + zet `setVelocity` naar caster met demping.  
- **Events:** `ProjectileHitEvent` voor effect.  
- **Visuals/SFX:** rode `REDSTONE` dust-lijn; ketting-clank sound.  
- **Balance:** cd 9s; bosses: geen pull, alleen slow.  
- **Randvoorwaarden:** geen block-interactie.  
- **Pseudocode:** onHit → apply effects; compute pull vector = (caster − target).normalize()*0.5.

---

## 3) Mephidic Reap
- **Doel:** Gooi je “schaduwzeis” als boemerang; twee keer passeren = twee keer pijn. Goed om een wave te clearen of om meerdere targets op rij te raken. Je houdt zelf de mobiliteit, want de zeis keert vanzelf terug.
- **Input/Output:** input = cast; output = boemerang-projectiel (heen/weer), pierce tot 3.  
- **Mechanics:** spawn `ArmorStand` (marker, invisible) als hitbox, beweeg langs spline vooruit en terug; bij entity-overlap: dmg + mini-slow.  
- **Events:** task-loop 12–16 ticks heen en 12–16 terug.  
- **Visuals/SFX:** `SMOKE_LARGE` + donkere `CRIT_MAGIC`.  
- **Balance:** 4–6♥ totaal per target max; cd 10s.  
- **Randvoorwaarden:** cap targets per tick voor performance.  
- **Pseudocode:** path-lerp + overlap-check → damage.

---

## 4) Gale Prison
- **Doel:** Vorm een korte **wind-cilinder** die entiteiten naar het midden trekt en pijlen wegduwt. Je gebruikt dit om pushes te breken of om channel-spells veilig af te krijgen. Werkt ook als anti-kiting tool tegen rangers. 
- **Input/Output:** input = plek; output = 4s vortex (r=4, h=3).  
- **Mechanics:** elke 5t: voor entiteiten in cilinder → velocity naar center met lichte opwaartse Y; `Projectile` in veld → `setVelocity` radiaal naar buiten.  
- **Events:** none.  
- **Visuals/SFX:** `CLOUD` spiraal; zachte wind-loop.  
- **Balance:** cd 16s; geen damage.  
- **Randvoorwaarden:** niet stacken >2 tegelijk per team.  
- **Pseudocode:** scheduler → pull/push vector berekenen.

---

## 5) Shatterfield
- **Doel:** Laat een waaier van **ijspieken** uit de grond schieten; terrein wordt tijdelijk glad. Sterk op bruggen en in smalle gangen. Combineert met knockback-skills om vijanden te laten glijden.
- **Input/Output:** input = voor je; output = 5–7 spikes in waaier + `SLOWNESS` & `freezeTicks`.  
- **Mechanics:** kies 5–7 offsets; per punt: `FROSTED_ICE` vloer 8s, spawn damage-hitbox (kleine `ArmorStand`/AABB) die 1–2♥ doet bij spawn.  
- **Events:** auto-revert blocks.  
- **Visuals/SFX:** `SNOWBALL`/`ITEM_SNOWBALL` + glas-kraak.  
- **Balance:** cd 12s; pierce-dmg 1x per target.  
- **Randvoorwaarden:** geen plaatsing op non-replaceables.  
- **Pseudocode:** plaats frosted-ice + apply slow/freeze. *(frost/ice + raytrace en particles conform Spigot API)* citeturn0search7

---

## 6) Seismic Rift
- **Doel:** Een **aardbreuk** raast 12–14 blok vooruit; vijanden worden opgegooid en verliezen positionering. Geweldig om charges af te straffen en om tijd te kopen voor je backline. 
- **Input/Output:** input = lijn; output = travelende fissure met knock-up + 2–3♥.  
- **Mechanics:** sample de lijn in stappen van 1b; per stap: mini-knockup (Y=0.4) + kleine dmg, met 3–4 stappen max per entity.  
- **Events:** none.  
- **Visuals/SFX:** `BLOCK_CRACK` (STONE/DIRT) + lage bass.  
- **Balance:** cd 14s; immuniteit voor gliding/elytra.  
- **Randvoorwaarden:** geen block-break.  
- **Pseudocode:** iterate points; affect nearby entities.

---

## 7) Thunder Lure
- **Doel:** Markeer één doelwit als **bliksemafleider**; elke hit in de volgende 4s kan mini-bliksem triggeren. Perfect om tanks onder druk te zetten of evasive targets te “pinnen”. Synergiseert met multi-hit skills. 
- **Input/Output:** input = target; output = mark (4s) → 25% kans op `strikeLightningEffect` bij hit (ICD 0.5s).  
- **Mechanics:** metadata op target; onDamageByEntity: roll proc → `world.strikeLightningEffect(targetLoc)` (geen fire/block-damage). citeturn0search7turn0search3  
- **Events:** listen op `EntityDamageByEntityEvent`.  
- **Visuals/SFX:** subtiele `ELECTRIC_SPARK`; korte thunder-tick.  
- **Balance:** cd 13s; bosses: halveer proc-chance.  
- **Randvoorwaarden:** cap strikes per mark (max 4).  
- **Pseudocode:** on mark: set flag; on hit: maybe-proc lightning effect.

---

## 8) Starlight Aegis
- **Doel:** Projecteer een **lichtkoepel** die magic-damage tempert en projectielen reflecteert onder een korte “parry-window”. Heerlijk om objectives te contesten of een revive/heal veilig te maken.  
- **Input/Output:** input = plek; output = dome 5s, −30% magic dmg, 0.4s reflect-window bij spawn.  
- **Mechanics:** dome via onzichtbare anchors; in `ProjectileHitEvent` binnen 0.4s na spawn → `setVelocity` terug (reflect).  
- **Events:** cleanup anchors.  
- **Visuals/SFX:** `WHITE_ASH` + zachte harp.  
- **Balance:** cd 18s; 1 actieve per speler.  
- **Randvoorwaarden:** friendly-fire reflect off (geen grief).  
- **Pseudocode:** spawn dome; set timestamp; intercept projectiles.

---

## 9) Spectral Mirage
- **Doel:** Plaats een **illusie-decoy** die aggro trekt en bij einde een mini-fear pulse geeft. Handig om cooldowns te baiten of om een disengage te forceren. De decoy explodeert in schaduwdeeltjes. 
- **Input/Output:** input = plek; output = decoy (ArmorStand/illusion) 5s → expire: fear 1s + 1♥ dmg.  
- **Mechanics:** ArmorStand marker + nametag; mobs prioriteren dichtsbijzijnde “enemy” (custom AI of simple lure); bij despawn AOE-effect.  
- **Events:** timer end/despawn.  
- **Visuals/SFX:** `ASH`/`SMOKE_NORMAL`.  
- **Balance:** cd 15s; spelers krijgen geen echte mind-control—alleen screen-shake/slow (SLOWNESS I 1s).  
- **Randvoorwaarden:** PVP-vriendelijk.  
- **Pseudocode:** spawn decoy; schedule expire → pulse.

---

## 10) Ember Bloom
- **Doel:** Een **veld van vurige bloemen** dat op voetstappen reageert en in kleine bursts tot ontbranding komt. Je gebruikt dit om zones te “minen” en rotaties te vertragen. Combineert geweldig met knockbacks en displacements. 
- **Input/Output:** input = center; output = 12–16 “bloemen” (proximity mines) 8s; trigger → 1–1.5♥ + kleine burn.  
- **Mechanics:** leg onzichtbare markers; bij entiteit binnen 1b → `createExplosion(power=0F, setFire=true, break=false)` voor SFX + apply burn. citeturn0search12  
- **Events:** scheduler voor auto-cleanup.  
- **Visuals/SFX:** `FLAME`/`FALLING_LAVA`; zachte pop.  
- **Balance:** cd 15s; cap 4 triggers per target.  
- **Randvoorwaarden:** **geen block-damage**, alleen vuur (config-toggle).  
- **Pseudocode:** place markers; on proximity → ignite & damage.

---

## API-referenties die ik erbij heb gepakt
- **Empire Wand (historische plugin & bediening):** Bukkit/Spigot resourcebeschrijvingen (rechtsklik wisselen, linksklik casten). citeturn0search12  
- **Bliksem zonder schade / met effect:** `World#strikeLightning` en `#strikeLightningEffect` (Spigot-API javadocs). citeturn0search7turn0search3  
- **Algemene Spigot-mechanics (particles/effects):** World/Spigot effect & particle API. citeturn0search11

> Wil je dat ik deze 10 meteen omzet in **Java stubs** (Paper 1.20.6, Java 21) + **config.yml** met cooldowns/radii/toggles, dan maak ik ze direct voor je klaar.