# EmpireWand Spells Documentation

This document describes how spells are structured and configured for the EmpireWand plugin. It also includes a creative catalogue of ideas; consult `src/main/resources/spells.yml` for the currently shipped, configured spells.

Implementation and configuration:
- Keys: use kebab-case (e.g., `glacial-spike`). Display names can include MiniMessage tags.
- Classes: place concrete spells under `src/main/java/com/example/empirewand/spell/implementation/` with UpperCamelCase names (e.g., `GlacialSpike`).
- Values: read all gameplay values from `spells.yml` via `ConfigService`. Do not hardcode.
- Permissions: `empirewand.spell.use.<key>` for using; `empirewand.spell.bind.<key>` for binding.
- Projectile handling (hybrid): implement `ProjectileSpell` for new/complex projectile spells (handled by `ProjectileListener`); legacy/simple effects may remain in `EntityListener` short-term.
- FX: prefer `FxService` helpers (`followParticles`, `followTrail`, `impact`, `trail`) to reduce duplicate schedulers and batch particles.

## Spell 1: Vital Echo (idea)
- **Key**: vital-echo
- **Name**: Vital Echo
- **Description**: Echoes the player's life force to mend wounds, creating a resonant pulse that heals and briefly mirrors vitality to nearby flora.
- **Mechanics**: Heals self by config amount, clamped to max health; spawns temporary grass blocks underfoot that wilt after, providing minor environmental feedback. Self-only, with echo radius check.
- **Effects**: Pulsing heart particles in a spiral pattern (12 counts, offset 0.3, speed 0.02); plays a ethereal chime sound (ENTITY_EXPERIENCE_ORB_PICKUP at 0.5 vol, 1.6 pitch).
- **Config Values**: `vital-echo.values.heal-amount` (default 7.5), `vital-echo.values.echo-radius` (2.0), `vital-echo.effects.particle-count` (12), `vital-echo.effects.sound-volume` (0.5).
- **Permissions**: `empirewand.spell.use.vital-echo`, `empirewand.spell.bypass.cooldown.vital-echo`.
- **Notes**: Promotes eco-magic theme; test with modified max health or in barren biomes where echo fails gracefully.

## Spell 2: Rune Seeker (idea)
- **Key**: rune-seeker
- **Name**: Rune Seeker
- **Description**: Summons a seeking rune that hunts entities, embedding and dealing phased damage over time via ancient glyph erosion.
- **Mechanics**: Launches a glowing rune projectile (using spectral arrow) that homes in 15-block radius, applies stacking damage (ignores half armor) and a debuff that erodes speed.
- **Effects**: Glowing rune trail (CRIT_MAGIC particles, 15 per tick); on embed, rune crackle sound (BLOCK_ANVIL_LAND at 0.8 vol, 0.8 pitch) and erosion sparks.
- **Config Values**: `rune-seeker.values.initial-damage` (5.0), `rune-seeker.values.dot-ticks` (40), `rune-seeker.values.homing-radius` (15.0), `rune-seeker.effects.particle-type` (CRIT_MAGIC).
- **Permissions**: `empirewand.spell.use.rune-seeker`.
- **Notes**: Despawns after 4s or 40 blocks; unique erosion debuff stacks up to 3. Balance for PvE rune hunts.

## Spell 3: Ember Cascade (idea)
- **Key**: ember-cascade
- **Name**: Ember Cascade
- **Description**: Unleashes a cascading wave of embers that chain-ignite surfaces and entities in a fractal pattern.
- **Mechanics**: Raytraces 25 blocks, spawns chained ember entities that spread fire ticks in a branching pattern, damaging and igniting without full explosion.
- **Effects**: Fractal flame particles (FLAME, 20 chained); cascading whoosh sound (ENTITY_BLAZE_SHOOT at 0.7 vol, 1.2 pitch).
- **Config Values**: `ember-cascade.values.chain-length` (4), `ember-cascade.values.fire-ticks` (80), `ember-cascade.values.spread-range` (1.5).
- **Permissions**: `empirewand.spell.use.ember-cascade`.
- **Notes**: Respects fire spread rules; fractal chaining adds unpredictability. Test in forests for balance.

## Spell 4: Storm Whisper (idea)
- **Key**: storm-whisper
- **Name**: Storm Whisper
- **Description**: Whispers to storm spirits, summoning localized lightning that echoes whispers to nearby entities for chain-stun.
- **Mechanics**: Targets 50-block raytrace, strikes lightning and applies whispering debuff (nausea + slowness) that propagates to adjacent entities.
- **Effects**: Whispering thunder (ENTITY_LIGHTNING_STRIKE at 1.0 vol, 0.9 pitch), ethereal wind particles (CLOUD, 10 at impact).
- **Config Values**: `storm-whisper.values.base-damage` (8.0), `storm-whisper.values.echo-count` (2), `storm-whisper.values.debuff-ticks` (50).
- **Permissions**: `empirewand.spell.use.storm-whisper`.
- **Notes**: Echoes only to hostiles; weather-agnostic. Long cooldown prevents storm spam.

## Spell 5: Crystal Bind
- **Key**: crystal-bind
- **Name**: Crystal Bind
- **Description**: Binds targets with growing crystal shards that pierce and resonate to slow via vibrational frequency.
- **Mechanics**: Fires crystal projectile (ender pearl), on hit grows shards in radius, applying slowness and minor pierce damage to lined entities.
- **Config Values**: `crystal-bind.values.pierce-damage` (3.5), `crystal-bind.values.resonance-potency` (3), `crystal-bind.values.growth-radius` (2.5).
- **Permissions**: `empirewand.spell.use.crystal-bind`.
- **Notes**: Shards revert to air after 30s; resonance vibrates armor for immersion. CC-focused.

## Spell 6: Thorn Labyrinth
- **Key**: thorn-labyrinth
- **Name**: Thorn Labyrinth
- **Description**: Weaves a labyrinth of thorny vines that ensnare and redirect projectiles back at intruders.
- **Mechanics**: Places vine blocks in a maze pattern (config size) ahead, thorns damage on contact and reflect arrows/projectiles.
- **Effects**: Twisting vine particles (VILLAGER_HAPPY, green tint), snapping sound (BLOCK_GRASS_BREAK at 0.9 vol, 1.0 pitch).
- **Config Values**: `thorn-labyrinth.values.maze-width` (4), `thorn-labyrinth.values.duration-ticks` (400), `thorn-labyrinth.values.reflect-chance` (0.7).
- **Permissions**: `empirewand.spell.use.thorn-labyrinth`.
- **Notes**: Air-only placement; unique reflection mechanic for defense. Dismantles after time.

## Spell 7: Void Skip
- **Key**: void-skip
- **Name**: Void Skip
- **Description**: Skips through void rifts to relocate, leaving temporary portals that pull in small items.
- **Mechanics**: Raytraces 40 blocks to safe spot, teleports and creates suction portals that vacuum nearby items for 10s.
- **Effects**: Void rift particles (PORTAL, swirling 15 counts), suction hum (ENTITY_ENDERMAN_TELEPORT at 0.4 vol, 1.5 pitch).
- **Config Values**: `void-skip.values.max-skip-distance` (40.0), `void-skip.values.suction-radius` (3.0), `void-skip.values.portal-ticks` (200).
- **Permissions**: `empirewand.spell.use.void-skip`.
- **Notes**: Safety checks for void/lava; logs skips. Item pull adds utility twist.

## Spell 8: Mirage Veil
- **Key**: mirage-veil
- **Name**: Mirage Veil
- **Description**: Veils the player in shifting mirages that duplicate movements and fade on interaction.
- **Mechanics**: Applies invisibility with 2 illusory duplicates that mirror actions briefly, dispelling on touch.
- **Config Values**: `mirage-veil.values.duration-ticks` (180), `mirage-veil.values.duplicate-count` (2), `mirage-veil.values.fade-speed` (0.05).
- **Permissions**: `empirewand.spell.use.mirage-veil`.
- **Notes**: Duplicates unequip items; reveals on damage. Enhances stealth with decoys.

## Spell 9: Zephyr Surge
- **Key**: zephyr-surge
- **Name**: Zephyr Surge
- **Description**: Surges with zephyr winds that accelerate movement and scatter loose blocks lightly.
- **Mechanics**: Applies speed effect; in radius, scatters nearby sand/gravel blocks for environmental chaos.
- **Config Values**: `zephyr-surge.values.surge-duration` (80 ticks), `zephyr-surge.values.speed-amplifier` (2), `zephyr-surge.values.scatter-radius` (4.0).
- **Permissions**: `empirewand.spell.use.zephyr-surge`.
- **Notes**: Block scatter non-destructive; high mobility with disruption.

## Spell 10: Venom Symbiosis
- **Key**: venom-symbiosis
- **Name**: Venom Symbiosis
- **Description**: Forms a symbiotic venom bond with nearby bees, creating a stinging swarm cloud.
- **Mechanics**: Spawns temporary bee entities allied to player, they poison intruders in radius while healing caster slightly per sting.
- **Config Values**: `venom-symbiosis.values.swarm-radius` (4.0), `venom-symbiosis.values.duration-seconds` (6), `venom-symbiosis.values.poison-potency` (1).
- **Permissions**: `empirewand.spell.use.venom-symbiosis`.
- **Notes**: Bees despawn post-use; symbiotic heal caps at 3. Avoid in apiaries.

## Spell 11: Aegis Resonance
- **Key**: aegis-resonance
- **Name**: Aegis Resonance
- **Description**: Resonates an aegis field that harmonizes with player armor, absorbing and redistributing damage as buffs.
- **Mechanics**: Grants absorption; excess damage converts to temporary resistance buffs based on armor type.
- **Config Values**: `aegis-resonance.values.absorb-pool` (5.0), `aegis-resonance.values.resonance-duration` (infinite until depleted), `aegis-resonance.values.buff-multiplier` (0.5).
- **Permissions**: `empirewand.spell.use.aegis-resonance`.
- **Notes**: Armor-specific buffs (e.g., diamond = higher res); visual harmonic waves.

## Spell 12: Entangle Nexus
- **Key**: entangle-nexus
- **Name**: Entangle Nexus
- **Description**: Creates a nexus point that entangles multiple targets in a web of slowing vines.
- **Mechanics**: Targets central entity, webs spread to 3 nearby, applying extreme slowness via nexus pull.
- **Config Values**: `entangle-nexus.values.nexus-range` (12.0), `entangle-nexus.values.entangle-count` (3), `entangle-nexus.values.slow-ticks` (70).
- **Permissions**: `empirewand.spell.use.entangle-nexus`.
- **Notes**: Nexus visible as glowing web; PvP web breaks on damage.

## Spell 13: Phase Flicker
- **Key**: phase-flicker
- **Name**: Phase Flicker
- **Description**: Flickers through phases to dodge forward, leaving afterimages that distract.
- **Mechanics**: Short teleport (4-8 blocks) with 2 flickering afterimages that taunt enemies briefly.
- **Config Values**: `phase-flicker.values.flicker-distance-min` (4.0), `phase-flicker.values.flicker-distance-max` (8.0), `phase-flicker.values.afterimage-ticks` (20).
- **Permissions**: `empirewand.spell.use.phase-flicker`.
- **Notes**: Afterimages despawn; multi-use under global cooldown.


## Spell 15: Gale Repulse
- **Key**: gale-repulse
- **Name**: Gale Repulse
- **Description**: Repulses with gales that carry pollen, applying minor poison on push.
- **Mechanics**: Pushes entities outward, pollen trail poisons lightly in wake.
- **Config Values**: `gale-repulse.values.repulse-strength` (1.8), `gale-repulse.values.pollen-radius` (3.5), `gale-repulse.values.poison-ticks` (30).
- **Permissions**: `empirewand.spell.use.gale-repulse`.
- **Notes**: No damage push with DoT twist; crowd scatter.

## Spell 16: Essence Bargain
- **Key**: essence-bargain
- **Name**: Essence Bargain
- **Description**: Bargains essence for a surge, draining health to fuel a regenerative aura that heals over time.
- **Mechanics**: Costs health upfront, grants regen aura that pulses heal to self and scales with cost.
- **Config Values**: `essence-bargain.values.essence-cost` (3.5), `essence-bargain.values.aura-ticks` (120), `essence-bargain.values.heal-per-pulse` (1.0).
- **Permissions**: `empirewand.spell.use.essence-bargain`.
- **Notes**: Prevents low-health use; aura visual as essence wisps.

## Spell 17: Spectral Companion
- **Key**: spectral-companion
- **Name**: Spectral Companion
- **Description**: Binds a spectral entity to the player, fighting as an ally and sharing a portion of damage taken.
- **Mechanics**: Summons a ghostly ally with health scaling to config; shares 20% of damage taken with the caster. Ghost fades after duration or if health drops to 0.
- **Config Values**: `spectral-companion.values.duration` (300 ticks), `spectral-companion.values.health-scaling` (0.3), `spectral-companion.values.damage-share` (0.2).
- **Permissions**: `empirewand.spell.use.spectral-companion`.
- **Notes**: Visualize with glowing particles; test scaling with player damage types.

## Spell 18: Eldritch Shield
- **Key**: eldritch-shield
- **Name**: Eldritch Shield
- **Description**: An unstable shield of void energy that absorbs damage and reflects it as a void burst.
- **Mechanics**: Absorbs damage up to config value, reflects 50% as void energy in a small radius. Unstable; has a chance to explode after absorbing max damage.
- **Config Values**: `eldritch-shield.values.absorb-amount` (10.0), `eldritch-shield.values.reflect-radius` (3.0), `eldritch-shield.values.unstable-chance` (0.3).
- **Permissions**: `empirewand.spell.use.eldritch-shield`.
- **Notes**: Visual void ripples; balance reflect damage and instability.

## Spell 19: Temporal Distortion
- **Key**: temporal-distortion
- **Name**: Temporal Distortion
- **Description**: Distorts time around the player, slowing enemies and projectiles while hastening allies.
- **Mechanics**: Applies slow to enemies and speed to allies in a radius; projectiles passing through are slowed. Duration and effect strength configurable.
- **Config Values**: `temporal-distortion.values.radius` (5.0), `temporal-distortion.values.duration` (100 ticks), `temporal-distortion.values.speed-multiplier` (1.5).
- **Permissions**: `empirewand.spell.use.temporal-distortion`.
- **Notes**: Visualized with clock particles; strategic use in battles.

## Spell 20: Crystalline Prison
- **Key**: crystalline-prison
- **Name**: Crystalline Prison
- **Description**: Encases a target in a prison of crystals, immobilizing and dealing damage over time.
- **Mechanics**: Targets an entity, encases in crystals that deal damage and prevent movement. Crystals have high durability and explode after duration, dealing area damage.
- **Config Values**: `crystalline-prison.values.damage-per-tick` (2.0), `crystalline-prison.values.prison-duration` (100 ticks), `crystalline-prison.values.explosion-radius` (3.0).
- **Permissions**: `empirewand.spell.use.crystalline-prison`.
- **Notes**: High damage and utility; test crystal durability and explosion balance.

## Spell 21: Nova Pulse
- **Key**: nova-pulse
- **Name**: Nova Pulse
- **Description**: Releases a pulse of nova energy, damaging and knocking back all entities in a wide radius.
- **Mechanics**: Applies damage and knockback to all entities in radius; damage scales with distance from the center. Configurable as a flat value or percentage of max health.
- **Config Values**: `nova-pulse.values.radius` (6.0), `nova-pulse.values.damage-type` (flat), `nova-pulse.values.damage-amount` (8.0), `nova-pulse.values.health-percentage` (0.1).
- **Permissions**: `empirewand.spell.use.nova-pulse`.
- **Notes**: Visualize with a bright nova explosion; balance damage type and amount.

## Spell 22: Shadow Veil
- **Key**: shadow-veil
- **Name**: Shadow Veil
- **Description**: Envelops the player in shadows, granting temporary invisibility and speed.
- **Mechanics**: Applies invisibility and speed boost; breaking blocks or attacking reveals the player. Duration and amplifier configurable.
- **Config Values**: `shadow-veil.values.duration` (100 ticks), `shadow-veil.values.amplifier` (1).
- **Permissions**: `empirewand.spell.use.shadow-veil`.
- **Notes**: Ideal for escapes or ambushes; test visibility and speed balance.

## Spell 23: Arcane Surge
- **Key**: arcane-surge
- **Name**: Arcane Surge
- **Description**: Channels arcane energy to surge forward, damaging and pushing back all in the path.
- **Mechanics**: A forward charge that damages and knocks back all entities in a line. Damage and knockback distance configurable.
- **Config Values**: `arcane-surge.values.distance` (10.0), `arcane-surge.values.damage` (6.0), `arcane-surge.values.knockback` (2.0).
- **Permissions**: `empirewand.spell.use.arcane-surge`.
- **Notes**: Test charge mechanics and ensure config values balance.

## Spell 24: Frostfire
- **Key**: frostfire
- **Name**: Frostfire
- **Description**: Conjures a flame of frost that burns and freezes targets in an area.
- **Mechanics**: Applies fire damage and a freezing effect in an area; frozen entities take extra damage from shattering. Configurable radius and damage values.
- **Config Values**: `frostfire.values.radius` (4.0), `frostfire.values.freeze-duration` (60 ticks), `frostfire.values.damage` (5.0).
- **Permissions**: `empirewand.spell.use.frostfire`.
- **Notes**: Visualize with blue flames; test freeze and damage interactions.

## Spell 25: Entropy Field
- **Key**: entropy-field
- **Name**: Entropy Field
- **Description**: Creates a field of chaotic energy that damages and confuses entities within.
- **Mechanics**: Applies damage over time and a confusion effect to all entities in the field. Field size and damage ticks are configurable.
- **Config Values**: `entropy-field.values.radius` (5.0), `entropy-field.values.damage-per-tick` (2.0), `entropy-field.values.duration` (100 ticks).
- **Permissions**: `empirewand.spell.use.entropy-field`.
- **Notes**: Chaotic visual effects; balance damage and confusion duration.

## Spell 26: Celestial Shield
- **Key**: celestial-shield
- **Name**: Celestial Shield
- **Description**: Summons a shield of celestial energy that blocks projectiles and reflects a portion back to the attacker.
- **Mechanics**: Absorbs a set amount of projectile damage, reflects a percentage back. Configurable absorption amount and reflection percentage.
- **Config Values**: `celestial-shield.values.absorb-amount` (8.0), `celestial-shield.values.reflect-percentage` (0.3).
- **Permissions**: `empirewand.spell.use.celestial-shield`.
- **Notes**: Visualize with a glowing shield; test absorption and reflection mechanics.

## Spell 27: Blight
- **Key**: blight
- **Name**: Blight
- **Description**: Inflicts a target area with a necrotic energy that damages and withers entities over time.
- **Mechanics**: Applies a damage-over-time effect in an area, with a chance to spread to nearby entities. Configurable damage, duration, and spread radius.
- **Config Values**: `blight.values.radius` (4.0), `blight.values.damage-per-tick` (1.5), `blight.values.duration` (100 ticks), `blight.values.spread-chance` (0.2).
- **Permissions**: `empirewand.spell.use.blight`.
- **Notes**: Thematic for decay or necromancy builds; balance damage, duration, and spread.

## Spell 28: Healing Bloom
- **Key**: healing-bloom
- **Name**: Healing Bloom
- **Description**: Conjures a bloom of healing energy that restores health to allies and removes negative effects.
- **Mechanics**: Heals allies in an area and removes harmful effects. Configurable heal amount and radius.
- **Config Values**: `healing-bloom.values.radius` (5.0), `healing-bloom.values.heal-amount` (6.0).
- **Permissions**: `empirewand.spell.use.healing-bloom`.
- **Notes**: Visualize with flower particles; synergizes with nature-themed spells.

## Spell 29: Searing Chains
- **Key**: searing-chains
- **Name**: Searing Chains
- **Description**: Binds a target with chains of fire, damaging and preventing movement.
- **Mechanics**: Chains the target in place, dealing fire damage over time. Chains have a chance to spread to nearby entities.
- **Config Values**: `searing-chains.values.damage-per-tick` (2.0), `searing-chains.values.chain-duration` (60 ticks), `searing-chains.values.spread-chance` (0.3).
- **Permissions**: `empirewand.spell.use.searing-chains`.
- **Notes**: High damage and utility; test chain spread and damage balance.

## Spell 30: Nature's Grasp
- **Key**: natures-grasp
- **Name**: Nature's Grasp
- **Description**: Calls upon nature to ensnare and damage foes with roots and thorns.
- **Mechanics**: Roots and thorns emerge from the ground in an area, damaging and rooting enemies. Configurable radius and damage values.
- **Config Values**: `natures-grasp.values.radius` (5.0), `natures-grasp.values.damage` (4.0), `natures-grasp.values.root-duration` (60 ticks).
- **Permissions**: `empirewand.spell.use.natures-grasp`.
- **Notes**: Visualize with plant growth particles; synergizes with nature-themed spells.

## Spell 31: Abyssal Step
- **Key**: abyssal-step
- **Name**: Abyssal Step
- **Description**: Step through the abyss, teleporting to a nearby location and leaving a lingering curse.
- **Mechanics**: Teleports the player a short distance and applies a curse that damages and slows enemies in the area over time.
- **Config Values**: `abyssal-step.values.teleport-range` (10.0), `abyssal-step.values.curse-damage` (2.0), `abyssal-step.values.curse-duration` (60 ticks).
- **Permissions**: `empirewand.spell.use.abyssal-step`.
- **Notes**: Thematic for shadow or void builds; test curse damage and duration.

## Spell 32: Phoenix Rebirth
- **Key**: phoenix-rebirth
- **Name**: Phoenix Rebirth
- **Description**: Sacrifice yourself to unleash a phoenix, reviving with flames and restoring health to nearby allies.
- **Mechanics**: On death, transforms into a phoenix for 10 seconds, dealing fire damage to nearby enemies and healing allies. Revives with 50% health.
- **Config Values**: `phoenix-rebirth.values.fire-damage` (4.0), `phoenix-rebirth.values.radius` (5.0), `phoenix-rebirth.values.rebirth-health` (10.0).
- **Permissions**: `empirewand.spell.use.phoenix-rebirth`.
- **Notes**: High risk-reward; test damage and healing balances.

## Spell 33: Icebound Fortitude
- **Key**: icebound-fortitude
- **Name**: Icebound Fortitude
- **Description**: Encases the player in ice, reducing damage taken and reflecting ice shards at attackers.
- **Mechanics**: Applies a damage reduction shield and reflects a portion of melee damage as ice shards. Configurable damage reduction and shard damage.
- **Config Values**: `icebound-fortitude.values.damage-reduction` (0.3), `icebound-fortitude.values.shard-damage` (3.0), `icebound-fortitude.values.duration` (100 ticks).
- **Permissions**: `empirewand.spell.use.icebound-fortitude`.
- **Notes**: Visualize with ice crystals; balance damage reduction and reflection mechanics.

## Spell 34: Thunderous Roar
- **Key**: thunderous-roar
- **Name**: Thunderous Roar
- **Description**: Unleashes a mighty roar that damages, stuns, and applies weakness to nearby enemies.
- **Mechanics**: Applies damage and a stun effect in an area; also applies a weakness debuff to affected enemies. Configurable radius and damage values.
- **Config Values**: `thunderous-roar.values.radius` (5.0), `thunderous-roar.values.damage` (6.0), `thunderous-roar.values.stun-duration` (40 ticks).
- **Permissions**: `empirewand.spell.use.thunderous-roar`.
- **Notes**: High damage and utility; test stun and damage interactions.

## Spell 35: Eldritch Blast
- **Key**: eldritch-blast
- **Name**: Eldritch Blast
- **Description**: Fires a blast of eldritch energy that damages and has a chance to confuse the target.
- **Mechanics**: A ranged attack that deals damage and has a 25% chance to apply confusion (nausea) for 3 seconds.
- **Config Values**: `eldritch-blast.values.damage` (5.0), `eldritch-blast.values.confusion-chance` (0.25), `eldritch-blast.values.confusion-duration` (60 ticks).
- **Permissions**: `empirewand.spell.use.eldritch-blast`.
- **Notes**: Thematic for eldritch or cosmic builds; test confusion chance and duration.

## Spell 36: Rune of Protection
- **Key**: rune-of-protection
- **Name**: Rune of Protection
- **Description**: Inscribes a protective rune on the ground that blocks enemies and projectiles.
- **Mechanics**: Creates a circular rune that blocks enemy movement and projectiles. Enemies that touch the rune are damaged and knocked back.
- **Config Values**: `rune-of-protection.values.radius` (3.0), `rune-of-protection.values.damage` (4.0), `rune-of-protection.values.duration` (100 ticks).
- **Permissions**: `empirewand.spell.use.rune-of-protection`.
- **Notes**: Strategic placement for defense; test damage and knockback values.

## Spell 37: Blood Sacrifice
- **Key**: blood-sacrifice
- **Name**: Blood Sacrifice
- **Description**: Sacrifice a portion of your health to empower your spells, increasing their potency for a short time.
- **Mechanics**: Drains a percentage of the caster's max health, boosting spell damage and effects for a duration.
- **Config Values**: `blood-sacrifice.values.health-percentage` (0.2), `blood-sacrifice.values.boost-duration` (100 ticks), `blood-sacrifice.values.damage-multiplier` (1.5).
- **Permissions**: `empirewand.spell.use.blood-sacrifice`.
- **Notes**: High risk-reward; prevents use at low health.

## Spell 38: Web of Shadows
- **Key**: web-of-shadows
- **Name**: Web of Shadows
- **Description**: Envelops an area in shadows, slowing and damaging enemies while granting stealth to allies.
- **Mechanics**: Applies a damage-over-time effect and slowness to enemies in the area; allies gain invisibility while in the web.
- **Config Values**: `web-of-shadows.values.radius` (5.0), `web-of-shadows.values.damage-per-tick` (1.0), `web-of-shadows.values.duration` (100 ticks).
- **Permissions**: `empirewand.spell.use.web-of-shadows`.
- **Notes**: Thematic for shadow or trickster builds; test damage and stealth interactions.

## Spell 39: Celestial Ascension
- **Key**: celestial-ascension
- **Name**: Celestial Ascension
- **Description**: Temporarily transform into a celestial being, gaining flight and a damage aura.
- **Mechanics**: Grants flight for 10 seconds and applies a damage aura that damages nearby enemies. Configurable damage and radius.
- **Config Values**: `celestial-ascension.values.damage` (4.0), `celestial-ascension.values.radius` (5.0), `celestial-ascension.values.duration` (200 ticks).
- **Permissions**: `empirewand.spell.use.celestial-ascension`.
- **Notes**: High mobility and damage; test flight and aura damage balance.

## Spell 40: Abyssal Grasp
- **Key**: abyssal-grasp
- **Name**: Abyssal Grasp
- **Description**: Grasp of the abyss that pulls enemies into the void, dealing damage and removing them from the battlefield temporarily.
- **Mechanics**: Pulls enemies within range towards the caster, dealing damage and applying a debuff that prevents teleportation or exiting the void for a short time.
- **Config Values**: `abyssal-grasp.values.radius` (6.0), `abyssal-grasp.values.damage` (5.0), `abyssal-grasp.values.debuff-duration` (60 ticks).
- **Permissions**: `empirewand.spell.use.abyssal-grasp`.
- **Notes**: Thematic for void or shadow builds; test pull strength and debuff effects.

## Spell 41: Stormcaller
- **Key**: stormcaller
- **Name**: Stormcaller
- **Description**: Calls down a storm to damage and debuff enemies with lightning and thunder.
- **Mechanics**: Summons a storm in an area that deals damage and applies slowness and blindness to enemies. Configurable storm duration and damage values.
- **Config Values**: `stormcaller.values.radius` (5.0), `stormcaller.values.damage` (6.0), `stormcaller.values.duration` (100 ticks).
- **Permissions**: `empirewand.spell.use.stormcaller`.
- **Notes**: High damage and utility; test storm effects and balance.

## Spell 42: Mirage Step
- **Key**: mirage-step
- **Name**: Mirage Step
- **Description**: Step into a mirage, becoming briefly untargetable and teleporting to a nearby location.
- **Mechanics**: On activation, the player becomes untargetable and teleports a short distance. Duration and teleport distance are configurable.
- **Config Values**: `mirage-step.values.untargetable-duration` (2.0), `mirage-step.values.teleport-distance` (5.0).
- **Permissions**: `empirewand.spell.use.mirage-step`.
- **Notes**: Ideal for dodging attacks; test untargetable duration and teleport distance.

## Spell 43: Frost Nova
- **Key**: frost-nova
- **Name**: Frost Nova
- **Description**: Releases a wave of frost that damages and freezes enemies in place.
- **Mechanics**: Applies damage and a freezing effect in a radius around the player. Frozen enemies take extra damage from subsequent attacks.
- **Config Values**: `frost-nova.values.radius` (4.0), `frost-nova.values.damage` (5.0), `frost-nova.values.freeze-duration` (60 ticks).
- **Permissions**: `empirewand.spell.use.frost-nova`.
- **Notes**: High damage and crowd control; test freeze and damage interactions.

## Spell 44: Ethereal Chains
- **Key**: ethereal-chains
- **Name**: Ethereal Chains
- **Description**: Binds enemies in ethereal chains, preventing movement and dealing damage over time.
- **Mechanics**: Chains enemies in place, dealing damage over time. Chains have a chance to spread to nearby enemies.
- **Config Values**: `ethereal-chains.values.damage-per-tick` (2.0), `ethereal-chains.values.chain-duration` (60 ticks), `ethereal-chains.values.spread-chance` (0.3).
- **Permissions**: `empirewand.spell.use.ethereal-chains`.
- **Notes**: High damage and utility; test chain spread and damage balance.

## Spell 45: Celestial Intervention
- **Key**: celestial-intervention
- **Name**: Celestial Intervention
- **Description**: Calls upon celestial beings to intervene, dealing damage and applying buffs to allies in an area.
- **Mechanics**: Deals damage to enemies and applies a random beneficial effect to allies in the area. Configurable radius and damage values.
- **Config Values**: `celestial-intervention.values.radius` (5.0), `celestial-intervention.values.damage` (6.0).
- **Permissions**: `empirewand.spell.use.celestial-intervention`.
- **Notes**: High utility and damage; test buff effects and balance.

## Spell 46: Shadow Strike
- **Key**: shadow-strike
- **Name**: Shadow Strike
- **Description**: Strike from the shadows, dealing bonus damage and applying a bleed effect.
- **Mechanics**: Teleports behind the target, dealing bonus damage and applying a bleed effect that damages over time.
- **Config Values**: `shadow-strike.values.bonus-damage` (4.0), `shadow-strike.values.bleed-duration` (60 ticks), `shadow-strike.values.bleed-damage` (2.0).
- **Permissions**: `empirewand.spell.use.shadow-strike`.
- **Notes**: High burst damage; test bleed damage and duration.

## Spell 47: Temporal Shift
- **Key**: temporal-shift
- **Name**: Temporal Shift
- **Description**: Shift forward in time, avoiding damage and repositioning.
- **Mechanics**: On activation, the player shifts forward in time, avoiding all damage and negative effects for a short duration.
- **Config Values**: `temporal-shift.values.shift-duration` (2.0), `temporal-shift.values.cooldown` (100 ticks).
- **Permissions**: `empirewand.spell.use.temporal-shift`.
- **Notes**: Ideal for avoiding lethal damage; test shift duration and cooldown.

## Spell 48: Arcane Ward
- **Key**: arcane-ward
- **Name**: Arcane Ward
- **Description**: Creates a ward that absorbs magical damage and reflects a portion back to the attacker.
- **Mechanics**: Absorbs a set amount of magical damage, reflecting a percentage back. Configurable absorption amount and reflection percentage.
- **Config Values**: `arcane-ward.values.absorb-amount` (8.0), `arcane-ward.values.reflect-percentage` (0.3).
- **Permissions**: `empirewand.spell.use.arcane-ward`.
- **Notes**: Visualize with arcane runes; test absorption and reflection mechanics.

## Spell 49: Eldritch Shield
- **Key**: eldritch-shield
- **Name**: Eldritch Shield
- **Description**: An unstable shield of void energy that absorbs damage and reflects it as a void burst.
- **Mechanics**: Absorbs damage up to config value, reflects 50% as void energy in a small radius. Unstable; has a chance to explode after absorbing max damage.
- **Config Values**: `eldritch-shield.values.absorb-amount` (10.0), `eldritch-shield.values.reflect-radius` (3.0), `eldritch-shield.values.unstable-chance` (0.3).
- **Permissions**: `empirewand.spell.use.eldritch-shield`.
- **Notes**: Visual void ripples; balance reflect damage and instability.

## Spell 50: Resurrection
- **Key**: resurrection
- **Name**: Resurrection
- **Description**: Revives a fallen ally, restoring them to life with a portion of their health.
- **Mechanics**: Targets a dead player within range, respawning them with configurable health percentage. High mana cost and long cooldown.
- **Config Values**: `resurrection.values.range` (10.0), `resurrection.values.respawn-health` (10.0), `resurrection.values.mana-cost` (50).
- **Permissions**: `empirewand.spell.use.resurrection` (admin only?).
- **Notes**: Rare, high mana cost. Team play essential.

This list provides a balanced mix of offensive, defensive, utility, and support spells. Implement based on priority, ensuring all use config-driven values and respect permissions/cooldowns. For implementation, follow the project structure and testing guidelines.
