package nl.wantedchef.empirewand.api.service;

import org.jetbrains.annotations.NotNull;

public interface MetricsService {
    void recordSpellCast(@NotNull String spellKey);
    void recordCooldownHit(@NotNull String spellKey);
    void recordError(@NotNull String category, @NotNull Throwable error);
    void recordPerformance(@NotNull String operation, long timeNanos);
    void flush();
    boolean isEnabled();
    void setEnabled(boolean enabled);
}
