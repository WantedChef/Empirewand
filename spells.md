EmpireWand – New Spells Plan

This plan follows the structure and conventions used in `important/spells.md` and the repository guidelines. Keys use kebab‑case, Java classes use UpperCamelCase under `com.example.empirewand.spell.implementation`.

Spells (10)

1) frost-nova (`frost-nova`)
- Summary: Instant icy blast around caster; damages and heavily slows.
- Mechanics: AoE within radius; apply damage + Slowness (amplifier configurable).
- Config: radius, damage, slow-duration-ticks, slow-amplifier, cooldown.
- FX: SNOWFLAKE/CLOUD particles; ice break sound.

2) chain-lightning (`chain-lightning`)
- Summary: Zaps a target and jumps to nearby enemies up to N times.
- Mechanics: Start at crosshair target in range, then hop to nearest within jump-radius; per‑hit damage and brief stun (nausea) optional.
- Config: range, jump-radius, jumps, damage, cooldown.
- FX: ELECTRIC_SPARK trail/impact; thunder sounds with reduced volume.

3) blink-strike (`blink-strike`)
- Summary: Teleport behind targeted enemy and backstab.
- Mechanics: Find looked‑at target in range; teleport to a safe spot behind target and deal burst damage.
- Config: range, behind-distance, damage, cooldown; flags.requires-los.
- FX: ENDERMAN teleport sounds; small crit particles on hit.

4) shadow-cloak (`shadow-cloak`)
- Summary: Temporary invisibility + speed boost for evasive play.
- Mechanics: Apply INVISIBILITY and SPEED to caster for duration.
- Config: duration-ticks, speed-amplifier, cooldown.
- FX: ambient particles (WITCH/SPELL) and soft sound.

5) stasis-field (`stasis-field`)
- Summary: Lock enemies in place for a short time.
- Mechanics: AoE around caster; apply extreme SLOW (amplifier ~250) and mining fatigue.
- Config: radius, duration-ticks, cooldown.
- FX: ENCHANTMENT_TABLE particles; low hum sound.

6) gust (`gust`)
- Summary: Wind cone that pushes entities back.
- Mechanics: Cone in front; apply knockback vector and small fall‑safe vertical push; optional minor damage.
- Config: range, angle, knockback, damage, cooldown.
- FX: CLOUD/SWEEP_ATTACK particles; windish sound.

7) arcane-orb (`arcane-orb`)
- Summary: Slow orb projectile that explodes in a small AoE.
- Mechanics: Launch Snowball; on hit, damage + knockback within radius (no block damage).
- Config: speed, radius, damage, knockback, cooldown.
- FX: ENCHANTMENT_TABLE trail; EXPLOSION_NORMAL on impact.

8) void-swap (`void-swap`)
- Summary: Swap positions with the targeted entity.
- Mechanics: Get looked‑at target in range; validate safety; teleport caster and target to each other.
- Config: range, cooldown; flags.requires-los.
- FX: portal particles on both spots; enderman sounds.

9) sandstorm (`sandstorm`)
- Summary: Dust cloud that blinds and slows around the caster.
- Mechanics: AoE around caster; apply BLINDNESS and SLOW for duration.
- Config: radius, blind-duration-ticks, slow-duration-ticks, slow-amplifier, cooldown.
- FX: BLOCK_DUST (sand) particles; sand/grit sound.

10) tornado (`tornado`)
- Summary: Uplift nearby enemies with a swirling burst.
- Mechanics: AoE; apply upward velocity and LEVITATION for a brief time; small fall protection via slow falling not applied to targets.
- Config: radius, lift-velocity, levitation-duration-ticks, levitation-amplifier, damage, cooldown.
- FX: CLOUD/SWEEP particles in a spiral; whoosh sound.

Implementation Notes
- Respect `features.friendly-fire` for player self/ally checks.
- Use `FxService` for particles, sounds, and messages; avoid heavy allocations in loops.
- Tag projectiles with `Keys.PROJECTILE_SPELL` and `Keys.PROJECTILE_OWNER` for `arcane-orb` and handle via `ProjectileSpell`.
- Register spells explicitly in `core.SpellRegistry` and add config entries in `spells.yml`.
