package nl.wantedchef.empirewand.api.impl;

import nl.wantedchef.empirewand.api.service.CooldownService;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public class CooldownServiceAdapter implements CooldownService {
    private final nl.wantedchef.empirewand.core.services.CooldownService delegate;
    
    public CooldownServiceAdapter(nl.wantedchef.empirewand.core.services.CooldownService delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public boolean isOnCooldown(@NotNull UUID playerId, @NotNull String spellKey, long currentTicks) {
        return delegate.isOnCooldown(playerId, spellKey, currentTicks);
    }
    
    @Override
    public void set(@NotNull UUID playerId, @NotNull String spellKey, long cooldownEndTicks) {
        delegate.set(playerId, spellKey, cooldownEndTicks);
    }
    
    @Override
    public void clear(@NotNull UUID playerId, @NotNull String spellKey) {
        delegate.clear(playerId, spellKey);
    }
    
    @Override
    public void clearAll(@NotNull UUID playerId) {
        delegate.clearAll(playerId);
    }
    
    @Override
    public long getRemainingTicks(@NotNull UUID playerId, @NotNull String spellKey, long currentTicks) {
        return delegate.getRemainingTicks(playerId, spellKey, currentTicks);
    }
    
    @Override
    public void setCooldownDisabled(@NotNull UUID playerId, boolean disabled) {
        delegate.setCooldownDisabled(playerId, disabled);
    }
    
    @Override
    public boolean isCooldownDisabled(@NotNull UUID playerId) {
        return delegate.isCooldownDisabled(playerId);
    }
}
