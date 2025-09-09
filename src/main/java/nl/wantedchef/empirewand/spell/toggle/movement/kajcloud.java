package nl.wantedchef.empirewand.spell.toggle.movement;

import java.time.Duration;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.spell.ToggleableSpell;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;

public class KajCloud extends Spell<Void> implements ToggleableSpell {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Kaj Cloud";
            description = "Create beautiful cloud particles under you while flying.";
            cooldown = Duration.ofSeconds(5);
            spellType = SpellType.MOVEMENT;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new KajCloud(this);
        }
    }

    private KajCloud(Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull String key() {
        return "kaj-cloud";
    }

    @Override
    public @NotNull PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected @Nullable Void executeSpell(@NotNull SpellContext context) {
        toggle(context.caster(), context);
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect - toggle handling is done in executeSpell
    }

    @Override
    public boolean isActive(@NotNull Player player) {
        // TODO: Implement actual state tracking
        return false;
    }

    @Override
    public void activate(@NotNull Player player, @NotNull SpellContext context) {
        // TODO: Implement toggle on logic (e.g., enable flying with particles)
        // Use context.fx() for particles, as per project guide
    }

    @Override
    public void deactivate(@NotNull Player player, @NotNull SpellContext context) {
        // TODO: Implement toggle off logic (e.g., stop particle effects)
    }
}