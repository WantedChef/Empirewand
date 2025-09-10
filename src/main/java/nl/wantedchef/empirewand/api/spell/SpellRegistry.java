package nl.wantedchef.empirewand.api.spell;

import java.util.Collection;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import nl.wantedchef.empirewand.spell.Spell;

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
