package nl.wantedchef.empirewand.spell;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.api.service.CooldownService;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CooldownPrereq implements PrereqInterface {

    private final String spellKey;

    public CooldownPrereq(@NotNull String spellKey) {
        this.spellKey = Objects.requireNonNull(spellKey, "Spell key cannot be null");
    }

    @Override
    @NotNull
    public CheckResult check(@NotNull SpellContext context) {
        if (!(context.caster() instanceof Player player)) {
            return CheckResult.SUCCESS; // Non-players don't have cooldowns
        }

        if (!EmpireWandAPI.isAvailable()) {
            return CheckResult.SUCCESS; // API not ready
        }

        CooldownService cooldownService = EmpireWandAPI.getService(CooldownService.class);
        if (cooldownService == null) {
            return CheckResult.SUCCESS; // Service not available
        }

        long nowTicks = player.getWorld().getFullTime();
        long remainingTicks = cooldownService.remaining(player.getUniqueId(), this.spellKey, nowTicks);
        if (remainingTicks > 0) {
            double seconds = remainingTicks / 20.0;
            return CheckResult.failure(Component.text("This spell is on cooldown for " + String.format("%.1f", seconds) + "s"));
        }

        return CheckResult.SUCCESS;
    }
}
