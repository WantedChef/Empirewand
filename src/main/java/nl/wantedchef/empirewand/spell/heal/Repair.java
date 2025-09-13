package nl.wantedchef.empirewand.spell.heal;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.spell.PrereqInterface;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Repair - Instantly repairs your gear from real Empirewand
 */
public class Repair extends Spell<Player> {

    public static class Builder extends Spell.Builder<Player> {
        public Builder(EmpireWandAPI api) {
            super(api);
            this.name = "Repair";
            this.description = "Instantly repair all your equipment";
            this.cooldown = Duration.ofSeconds(20);
            this.spellType = SpellType.HEAL;
        }

        @NotNull
        public Spell<Player> build() {
            return new Repair(this);
        }
    }

    private static final boolean DEFAULT_REPAIR_ALL = true;
    private static final boolean DEFAULT_REPAIR_INVENTORY = false;

    private Repair(Builder builder) {
        super(builder);
    }

    @Override
    public String key() {
        return "repair";
    }

    @Override
    public PrereqInterface prereq() {
        return new PrereqInterface.LevelPrereq(10);
    }

    @Override
    protected Player executeSpell(@NotNull SpellContext context) {
        return context.caster();
    }
    
    private boolean repairItem(ItemStack item) {
        if (item == null) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            if (damageable.hasDamage()) {
                damageable.setDamage(0);
                item.setItemMeta(meta);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void handleEffect(@NotNull SpellContext context, @NotNull Player player) {
        boolean repairAll = spellConfig.getBoolean("flags.repair_all", DEFAULT_REPAIR_ALL);
        boolean repairInventory = spellConfig.getBoolean("flags.repair_inventory", DEFAULT_REPAIR_INVENTORY);
        
        int repairedCount = 0;
        
        // Repair armor
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (repairItem(armor)) {
                repairedCount++;
            }
        }
        
        // Repair items in hands
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (repairItem(mainHand)) {
            repairedCount++;
        }
        
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (repairItem(offHand)) {
            repairedCount++;
        }
        
        // Repair entire inventory if enabled
        if (repairAll && repairInventory) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (repairItem(item)) {
                    repairedCount++;
                }
            }
        }
        
        // Visual effects
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 15, 0.5, 0.5, 0.5, 0);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation(), 20, 0.5, 0.5, 0.5, 0);
        
        // Sound effects
        context.fx().playSound(player, Sound.BLOCK_ANVIL_USE, 1.0f, 1.5f);
        context.fx().playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        
        if (repairedCount > 0) {
            player.sendMessage("§b§lRepair §3restored " + repairedCount + " items!");
        } else {
            player.sendMessage("§7No items needed repair.");
        }
    }
}
