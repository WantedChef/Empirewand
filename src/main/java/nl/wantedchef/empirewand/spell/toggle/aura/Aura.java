package nl.wantedchef.empirewand.spell.toggle.aura;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.api.spell.toggle.ToggleableSpell;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public final class Aura extends Spell<Void> implements ToggleableSpell {

    public static class Builder extends Spell.Builder<Void> {
        public Builder(EmpireWandAPI api) {
            super(api);
            name = "Aura";
            description = "Surround yourself with a magical aura.";
            cooldown = Duration.ofSeconds(10);
            spellType = SpellType.AURA;
        }

        @Override
        public @NotNull Spell<Void> build() {
            return new Aura(this);
        }
    }

    private Aura(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "aura";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.NonePrereq();
    }

    @Override
    protected Void executeSpell(SpellContext context) {
        // Toggle implementation would go here
        return null;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Void result) {
        // Instant effect
    }

    @Override
    public boolean isActive(Player player) {
        // Implementation would go here
        return false;
    }

    @Override
    public void activate(Player player, SpellContext context) {
        // Implementation would go here
    }

    @Override
    public void deactivate(Player player, SpellContext context) {
        // Implementation would go here
    }

    @Override
    public void forceDeactivate(Player player) {
        // Implementation would go here
    }
}