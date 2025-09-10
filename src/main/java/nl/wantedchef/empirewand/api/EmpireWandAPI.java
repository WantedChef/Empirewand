package nl.wantedchef.empirewand.api;

import org.jetbrains.annotations.NotNull;

public final class EmpireWandAPI {
    private static EmpireWandProvider provider;

    @NotNull
    public static EmpireWandProvider getProvider() {
        if (provider == null) {
            throw new IllegalStateException("EmpireWand API provider not set");
        }
        return provider;
    }

    public static void setProvider(@NotNull EmpireWandProvider provider) {
        EmpireWandAPI.provider = provider;
    }

    public static void clearProvider() {
        EmpireWandAPI.provider = null;
    }

    public static boolean isProviderSet() {
        return provider != null;
    }

    public interface EmpireWandProvider {
        @NotNull
        nl.wantedchef.empirewand.api.spell.SpellRegistry getSpellRegistry();

        @NotNull
        nl.wantedchef.empirewand.api.service.CooldownService getCooldownService();

        @NotNull
        nl.wantedchef.empirewand.api.service.EffectService getEffectService();

        @NotNull
        nl.wantedchef.empirewand.api.service.ConfigService getConfigService();

        @NotNull
        nl.wantedchef.empirewand.api.service.PermissionService getPermissionService();

        @NotNull
        nl.wantedchef.empirewand.api.service.WandService getWandService();

        @NotNull
        nl.wantedchef.empirewand.api.service.MetricsService getMetricsService();

        @NotNull
        Version getVersion();

        boolean isCompatible(@NotNull Version version);
    }
}
