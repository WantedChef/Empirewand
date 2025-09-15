package nl.wantedchef.empirewand.spell.lightning.advanced;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.ConfigService;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.ProjectileSpell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class LightningArrow extends ProjectileSpell<Arrow> {

    public static class Builder extends ProjectileSpell.Builder<Arrow> {
        public Builder(EmpireWandAPI api) {
            super(api, Arrow.class);
            this.name = "Lightning Arrow";
            this.description = "Fires an arrow that calls down a lightning strike on impact.";
            this.cooldown = java.time.Duration.ofSeconds(8);
            this.spellType = SpellType.LIGHTNING;
            this.trailParticle = Particle.ELECTRIC_SPARK;
            this.hitSound = Sound.ENTITY_LIGHTNING_BOLT_THUNDER;
        }

        @Override
        @NotNull
        public ProjectileSpell<Arrow> build() {
            return new LightningArrow(this);
        }
    }

    private LightningArrow(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "lightning-arrow";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected void launchProjectile(@NotNull SpellContext context) {
        Player caster = context.caster();
        caster.launchProjectile(Arrow.class, caster.getEyeLocation().getDirection().multiply(2.0), arrow -> {
            arrow.setShooter(caster);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            arrow.setCritical(true);
            arrow.setDamage(0.0); // We handle damage manually.
            arrow.setKnockbackStrength(0);
            // PDC tags are set automatically by the base ProjectileSpell class if we were
            // to call super,
            // but since we are customizing the launch, we set them manually.
            arrow.getPersistentDataContainer().set(nl.wantedchef.empirewand.core.storage.Keys.PROJECTILE_SPELL,
                    org.bukkit.persistence.PersistentDataType.STRING, key());
            arrow.getPersistentDataContainer().set(nl.wantedchef.empirewand.core.storage.Keys.PROJECTILE_OWNER,
                    org.bukkit.persistence.PersistentDataType.STRING, caster.getUniqueId().toString());
        });
        context.fx().playSound(caster, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
    }

    @Override
    protected void handleHit(@NotNull SpellContext context, @NotNull Projectile projectile,
            @NotNull ProjectileHitEvent event) {
        Location hitLoc = projectile.getLocation();

        // Always strike lightning on impact - this creates the visual and sound effect
        hitLoc.getWorld().strikeLightning(hitLoc);

        // Apply additional spell damage to entities in radius
        double spellDamage = spellConfig.getDouble("values.damage", 8.0);
        double lightningDamage = spellConfig.getDouble("values.lightning-damage", 12.0);
        double radius = spellConfig.getDouble("values.radius", 3.0);
        boolean glowing = spellConfig.getBoolean("flags.glowing", true);
        int glowTicks = spellConfig.getInt("values.glowing-duration-ticks", 60);

        for (var e : hitLoc.getWorld().getNearbyLivingEntities(hitLoc, radius)) {
            if (e.equals(context.caster()) && !EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                    .getBoolean("features.friendly-fire", false))
                continue;

            // Apply both spell damage and lightning damage
            double totalDamage = spellDamage + lightningDamage;
            e.damage(totalDamage, context.caster());
            if (glowing) {
                e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowTicks, 0, false, true));
            }
        }
    }
}
