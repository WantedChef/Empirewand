package nl.wantedchef.empirewand.core.services;

import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.spell.Spell;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SpellRegistryImpl implements SpellRegistry {
    private final Map<String, Spell<?>> spells = new ConcurrentHashMap<>();
    
    @Override
    public void register(@NotNull Spell<?> spell) {
        spells.put(spell.getKey(), spell);
    }
    
    @Override
    public boolean unregister(@NotNull String key) {
        return spells.remove(key) != null;
    }
    
    @Override
    public @NotNull Optional<Spell<?>> getSpell(@NotNull String key) {
        return Optional.ofNullable(spells.get(key));
    }
    
    @Override
    public @NotNull Collection<Spell<?>> getAllSpells() {
        return spells.values();
    }
    
    @Override
    public @NotNull Collection<String> getAllKeys() {
        return spells.keySet();
    }
    
    @Override
    public boolean isRegistered(@NotNull String key) {
        return spells.containsKey(key);
    }
    
    @Override
    public int size() {
        return spells.size();
    }
    
    @Override
    public void clear() {
        spells.clear();
    }
}
