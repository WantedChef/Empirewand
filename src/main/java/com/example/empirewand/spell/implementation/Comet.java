package com.example.empirewand.spell.implementation;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.ProjectileSpell;
import com.example.empirewand.spell.SpellContext;
import com.example.empirewand.spell.Prereq;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class Comet implements ProjectileSpell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        var spells = context.config().getSpellsConfig();
        float yield = (float) spells.getDouble("comet.values.yield", 2.5);

        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setYield(Math.max(0.0f, yield));
        fireball.setIsIncendiary(false);
        fireball.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, getName());
        fireball.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING, player.getUniqueId().toString());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (fireball.isDead()) {
                    this.cancel();
                    return;
                }
                context.fx().trail(fireball.getLocation());
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
    }

    @Override
    public void onProjectileHit(SpellContext context, Projectile projectile, ProjectileHitEvent event) {
        context.fx().impact(projectile.getLocation());

        if (event.getHitEntity() instanceof LivingEntity target) {
            String ownerUUID = projectile.getPersistentDataContainer().get(Keys.PROJECTILE_OWNER, PersistentDataType.STRING);
            double damage = context.config().getSpellsConfig().getDouble("comet.values.damage", 7.0);
            boolean hitPlayers = context.config().getSpellsConfig().getBoolean("comet.flags.hit-players", true);
            boolean hitMobs = context.config().getSpellsConfig().getBoolean("comet.flags.hit-mobs", true);
            boolean isPlayer = target instanceof Player;
            if (((isPlayer && hitPlayers) || (!isPlayer && hitMobs)) && ownerUUID != null) {
                Player caster = Bukkit.getPlayer(UUID.fromString(ownerUUID));
                if (caster != null) {
                    boolean friendlyFire = context.config().getConfig().getBoolean("features.friendly-fire", false);
                    boolean hittingSelfWhenNoFF = (target instanceof Player tgtPlayer) && !friendlyFire && tgtPlayer.getUniqueId().equals(caster.getUniqueId());
                    if (!hittingSelfWhenNoFF) {
                        target.damage(damage, caster);
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return "comet";
    }

    @Override
    public String key() {
        return "comet";
    }

    @Override
    public Component displayName() {
        return Component.text("Comet");
    }

    @Override
    public Prereq prereq() {
        return new Prereq(true, Component.text(""));
    }
}
