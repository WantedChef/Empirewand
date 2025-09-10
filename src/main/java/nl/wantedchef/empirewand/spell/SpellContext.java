package nl.wantedchef.empirewand.spell;

import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Context information for spell casting.
 */
public class SpellContext {
    private final Player caster;
    private final Location location;
    
    public SpellContext(@NotNull Player caster, @NotNull Location location) {
        this.caster = caster;
        this.location = location;
    }
    
    @NotNull
    public Player getCaster() { return caster; }
    
    @NotNull
    public Location getLocation() { return location; }
}
