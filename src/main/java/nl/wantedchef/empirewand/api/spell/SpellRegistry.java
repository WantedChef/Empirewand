package nl.wantedchef.empirewand.api.spell;

import nl.wantedchef.empirewand.spell.base.Spell;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.Optional;

public interface SpellRegistry {
    void register(@NotNull Spell<?> spell);

    boolean unregister(@NotNull String key);

    @NotNull
    Optional<Spell<?>> getSpell(@NotNull String key);

    @NotNull
    Collection<Spell<?>> getAllSpells();

    @NotNull
    Collection<String> getAllKeys();

    boolean isRegistered(@NotNull String key);

    int size();

    void clear();
}
