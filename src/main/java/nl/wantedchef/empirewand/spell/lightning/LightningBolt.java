package nl.wantedchef.empirewand.spell.lightning;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.ConfigService;

import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LightningBolt extends Spell<Void> {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Lightning Bolt";
            this.description = "Calls down a powerful lightning bolt on your target.";
            this.cooldown = java.time.Duration.ofSeconds(10);
            this.spellType = SpellType.LIGHTNING;
        }

        @Override
        @NotNull
        public Spell<Void> build() {
            return new LightningBolt(this);
        }
    }

    private LightningBolt(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "lightning-bolt";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(SpellContext context) {
        Player player = context.caster();

        double range = spellConfig.getDouble("values.range", 20.0);
        double damage = spellConfig.getDouble("values.damage", 24.0);
        boolean friendlyFire = EmpireWandAPI.getService(ConfigService.class).getMainConfig()
                .getBoolean("features.friendly-fire", false);

        var targetEntity = player.getTargetEntity((int) range);
        if (!(targetEntity instanceof LivingEntity)) {
            context.fx().fizzle(player);
            return null;
        }
        LivingEntity target = (LivingEntity) targetEntity;
        if (target.isDead() || !target.isValid()) {
            context.fx().fizzle(player);
            return null;
        }

        Location targetLoc = target.getLocation();
        if (targetLoc.getWorld().getHighestBlockYAt(targetLoc) < targetLoc.getY()) {
            context.fx().fizzle(player);
            return null;
        }

        target.getWorld().strikeLightning(targetLoc);
        damageAtStrike(context, targetLoc, damage, friendlyFire);

        context.fx().playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect.
    }

    private void damageAtStrike(SpellContext context, Location strikeLoc, double damage, boolean friendlyFire) {
        for (var entity : strikeLoc.getWorld().getNearbyLivingEntities(strikeLoc, 2.0)) {
            if (entity.equals(context.caster()) && !friendlyFire)
                continue;
            entity.damage(damage, context.caster());
            context.fx().spawnParticles(entity.getLocation(), Particle.ELECTRIC_SPARK, 15, 0.3, 0.3, 0.3, 0.1);
        }
    }
}
