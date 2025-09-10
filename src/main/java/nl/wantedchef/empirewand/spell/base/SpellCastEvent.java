package nl.wantedchef.empirewand.spell.base;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a spell is cast.
 */
public class SpellCastEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player caster;
    private final Spell<?> spell;
    private final SpellContext context;
    private final CastResult result;

    public SpellCastEvent(@NotNull Player caster, @NotNull Spell<?> spell, @NotNull SpellContext context,
            @NotNull CastResult result) {
        this.caster = caster;
        this.spell = spell;
        this.context = context;
        this.result = result;
    }

    @NotNull
    public Player getCaster() {
        return caster;
    }

    @NotNull
    public Spell<?> getSpell() {
        return spell;
    }

    @NotNull
    public SpellContext getContext() {
        return context;
    }

    @NotNull
    public CastResult getResult() {
        return result;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}





