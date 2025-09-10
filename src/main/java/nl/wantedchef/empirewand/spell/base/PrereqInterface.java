package nl.wantedchef.empirewand.spell.base;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for spell prerequisites. Defines the contract for checking if a spell can be cast.
 */
public interface PrereqInterface {

    /**
     * Checks if the prerequisite is met for the given spell context.
     *
     * @param api The EmpireWand API instance.
     * @param spellKey The key of the spell being checked.
     * @return true if the prerequisite is met, false otherwise.
     */
    boolean check(@NotNull EmpireWandAPI api, @NotNull String spellKey);

    /**
     * A default implementation that always returns true (no prerequisites).
     */
    class NonePrereq implements PrereqInterface {
        @Override
        public boolean check(@NotNull EmpireWandAPI api, @NotNull String spellKey) {
            return true;
        }
    }
}
