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
import org.bukkit.Location;
import org.bukkit.Color;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Random;

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
        boolean enhanceItems = spellConfig.getBoolean("flags.enhance_items", false);
        
        // Start spectacular repair forge process
        startMagicalForge(context, player, repairAll, repairInventory, enhanceItems);
    }
    
    private void startMagicalForge(SpellContext context, Player player, boolean repairAll, boolean repairInventory, boolean enhanceItems) {
        Location center = player.getLocation();
        
        // Create magical forge circle
        createForgeCircle(context, center);
        
        // Start forge animation
        new BukkitRunnable() {
            int ticks = 0;
            final int forgeDuration = 60; // 3 seconds
            int repairedCount = 0;
            int enhancedCount = 0;
            
            @Override
            public void run() {
                if (ticks >= forgeDuration) {
                    // Complete the forging
                    completeForging(context, player, repairedCount, enhancedCount);
                    cancel();
                    return;
                }
                
                Location playerLoc = player.getLocation();
                
                // Enhanced magical forge aura with multiple layers
                createSpectacularForgeAura(context, playerLoc, ticks);
                
                // Forge sparks
                if (ticks % 5 == 0) {
                    for (int i = 0; i < 10; i++) {
                        Random random = new Random();
                        double x = (random.nextDouble() - 0.5) * 4;
                        double z = (random.nextDouble() - 0.5) * 4;
                        double y = random.nextDouble() * 3 + 1;
                        
                        Location sparkLoc = playerLoc.clone().add(x, y, z);
                        context.fx().spawnParticles(sparkLoc, Particle.FIREWORK, 1, 0, 0, 0, 0.1);
                        context.fx().spawnParticles(sparkLoc, Particle.CRIT, 1, 0, 0, 0, 0.05);
                    }
                }
                
                // Repair items gradually
                if (ticks % 10 == 0) {
                    repairedCount += performRepairs(player, repairAll, repairInventory, ticks / 10);
                    if (enhanceItems && ticks % 20 == 0) {
                        enhancedCount += performEnhancements(player, ticks / 20);
                    }
                }
                
                // Forge sounds
                if (ticks % 8 == 0) {
                    context.fx().playSound(playerLoc, Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f + (float)(ticks % 20) * 0.1f);
                }
                if (ticks % 15 == 0) {
                    context.fx().playSound(playerLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 1.5f);
                }
                
                ticks += 2;
            }
        }.runTaskTimer(context.plugin(), 0L, 2L);
        
        // Initial forge ignition
        context.fx().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.8f);
        context.fx().playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.2f);
        context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.EXPLOSION, 3, 1, 1, 1, 0);
        
        player.sendMessage("§6§l⚒ Magical Forge §eactivated! Repairing equipment...");
    }
    
    private void createForgeCircle(SpellContext context, Location center) {
        // Create magical forge setup
        for (int i = 0; i < 16; i++) {
            final int step = i;
            context.plugin().getServer().getScheduler().runTaskLater(context.plugin(), () -> {
                // Forge anvil circle
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double radius = 3;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location anvilLoc = center.clone().add(x, 0.1, z);
                    
                    context.fx().spawnParticles(anvilLoc, Particle.DUST, 2, 0.1, 0.1, 0.1, 0, 
                        new Particle.DustOptions(Color.fromRGB(139, 69, 19), 1.0f)); // Brown for anvil
                }
                
                // Inner fire circle
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 12) {
                    double radius = 1.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location fireLoc = center.clone().add(x, 0.2, z);
                    
                    context.fx().spawnParticles(fireLoc, Particle.FLAME, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }, step * 2L);
        }
    }
    
    private int performRepairs(Player player, boolean repairAll, boolean repairInventory, int batch) {
        int count = 0;
        
        // Repair in batches to spread out the effects
        if (batch == 0) {
            // Repair armor first
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (repairItem(armor)) {
                    count++;
                    createItemRepairEffect(player.getLocation(), armor);
                }
            }
        } else if (batch == 1) {
            // Repair hands
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (repairItem(mainHand)) {
                count++;
                createItemRepairEffect(player.getLocation(), mainHand);
            }
            
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (repairItem(offHand)) {
                count++;
                createItemRepairEffect(player.getLocation(), offHand);
            }
        } else if (batch >= 2 && repairAll && repairInventory) {
            // Repair inventory items
            ItemStack[] contents = player.getInventory().getContents();
            int startIndex = (batch - 2) * 5;
            int endIndex = Math.min(startIndex + 5, contents.length);
            
            for (int i = startIndex; i < endIndex; i++) {
                if (contents[i] != null && repairItem(contents[i])) {
                    count++;
                    createItemRepairEffect(player.getLocation(), contents[i]);
                }
            }
        }
        
        return count;
    }
    
    private int performEnhancements(Player player, int batch) {
        int count = 0;
        Random random = new Random();
        
        // Small chance to add temporary enhancement effects
        if (random.nextDouble() < 0.3) { // 30% chance
            for (ItemStack item : player.getInventory().getArmorContents()) {
                if (item != null && item.getItemMeta() instanceof Damageable) {
                    // Add temporary enchantment glow effect (doesn't actually add enchants)
                    createEnhancementEffect(player.getLocation(), item);
                    count++;
                }
            }
        }
        
        return count;
    }
    
    private void createItemRepairEffect(Location center, ItemStack item) {
        if (item == null) return;
        
        Random random = new Random();
        double x = (random.nextDouble() - 0.5) * 2;
        double z = (random.nextDouble() - 0.5) * 2;
        Location itemLoc = center.clone().add(x, 1.5, z);
        
        // Repair sparkles
        center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, itemLoc, 3, 0.2, 0.2, 0.2, 0);
        center.getWorld().spawnParticle(Particle.ENCHANT, itemLoc, 5, 0.3, 0.3, 0.3, 0.1);
        center.getWorld().spawnParticle(Particle.FIREWORK, itemLoc, 2, 0.1, 0.1, 0.1, 0.05);
    }
    
    private void createEnhancementEffect(Location center, ItemStack item) {
        Random random = new Random();
        double x = (random.nextDouble() - 0.5) * 2;
        double z = (random.nextDouble() - 0.5) * 2;
        Location itemLoc = center.clone().add(x, 2, z);
        
        // Spectacular enhancement aura with multiple layers
        createSpectacularEnhancementAura(center, itemLoc);
    }
    
    private void completeForging(SpellContext context, Player player, int repairedCount, int enhancedCount) {
        Location center = player.getLocation();
        
        // Grand forge completion effects
        context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.EXPLOSION_EMITTER, 2, 0.5, 0.5, 0.5, 0);
        context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.FIREWORK, 50, 2, 2, 2, 0.3);
        context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.ENCHANT, 100, 3, 3, 3, 0.5);
        context.fx().spawnParticles(center.clone().add(0, 1, 0), Particle.HAPPY_VILLAGER, 30, 2, 2, 2, 0);
        
        // Completion sounds
        context.fx().playSound(player, Sound.BLOCK_ANVIL_USE, 2.0f, 1.0f);
        context.fx().playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1.3f);
        context.fx().playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        context.fx().playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2.0f, 1.5f);
        
        // Messages
        if (repairedCount > 0) {
            String message = "§6§l⚒ Magical Forge §erestored " + repairedCount + " items!";
            if (enhancedCount > 0) {
                message += " §d+" + enhancedCount + " enhanced!";
            }
            player.sendMessage(message);
        } else {
            player.sendMessage("§7No items needed repair. The forge admires your well-maintained equipment!");
        }
    }
    
    private void createSpectacularForgeAura(SpellContext context, Location center, int ticks) {
        // Enhanced magical forge aura with multiple layers
        
        // Rotating forge flames with intensity
        for (int i = 0; i < 8; i++) {
            double angle = ticks * 0.15 + (i * Math.PI / 4);
            double radius = 2.5 + Math.sin(ticks * 0.1 + i) * 0.5;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = Math.sin(ticks * 0.2 + i) * 0.8 + 1.5;
            
            Location flameLoc = center.clone().add(x, y, z);
            
            // Multi-layered flame effects
            context.fx().spawnParticles(flameLoc, Particle.SOUL_FIRE_FLAME, 3, 0.15, 0.15, 0.15, 0.03);
            context.fx().spawnParticles(flameLoc, Particle.FLAME, 2, 0.1, 0.1, 0.1, 0.02);
            context.fx().spawnParticles(flameLoc, Particle.ENCHANT, 2, 0.2, 0.2, 0.2, 0.08);
            
            // Forge dust aura
            context.fx().spawnParticles(flameLoc, Particle.DUST, 1, 0.1, 0.1, 0.1, 0,
                new Particle.DustOptions(Color.fromRGB(255, 165, 0), 1.2f)); // Orange
        }
        
        // Central forge pillar
        for (int h = 0; h < 6; h++) {
            double y = h * 0.5 + Math.sin(ticks * 0.25 + h) * 0.3;
            Location pillarLoc = center.clone().add(0, y, 0);
            
            context.fx().spawnParticles(pillarLoc, Particle.LAVA, 2, 0.2, 0.2, 0.2, 0.03);
            context.fx().spawnParticles(pillarLoc, Particle.FIREWORK, 1, 0.15, 0.15, 0.15, 0.05);
            
            if (h % 2 == 0) {
                context.fx().spawnParticles(pillarLoc, Particle.DUST, 3, 0.25, 0.25, 0.25, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 69, 0), 1.5f)); // Red Orange
            }
        }
        
        // Forge ring foundation
        if (ticks % 8 == 0) {
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                double x = Math.cos(angle) * 3.5;
                double z = Math.sin(angle) * 3.5;
                Location ringLoc = center.clone().add(x, 0.2, z);
                
                context.fx().spawnParticles(ringLoc, Particle.DUST, 2, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(Color.fromRGB(139, 69, 19), 1.0f)); // Brown anvil
                context.fx().spawnParticles(ringLoc, Particle.CRIT, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }
    
    private void createSpectacularEnhancementAura(Location center, Location itemLoc) {
        // Spectacular enhancement aura with multiple magical layers
        
        // Golden enhancement core
        center.getWorld().spawnParticle(Particle.DUST, itemLoc, 12, 0.4, 0.4, 0.4, 0,
            new Particle.DustOptions(Color.fromRGB(255, 215, 0), 2.0f)); // Gold
        center.getWorld().spawnParticle(Particle.END_ROD, itemLoc, 5, 0.3, 0.3, 0.3, 0.08);
        
        // Enhancement rings
        for (int ring = 1; ring <= 3; ring++) {
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                double radius = ring * 0.5;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location ringLoc = itemLoc.clone().add(x, 0, z);
                
                // Multi-colored enhancement rings
                if (ring == 1) {
                    center.getWorld().spawnParticle(Particle.DUST, ringLoc, 1, 0.05, 0.05, 0.05, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.3f)); // Gold
                } else if (ring == 2) {
                    center.getWorld().spawnParticle(Particle.DUST, ringLoc, 1, 0.05, 0.05, 0.05, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.1f)); // White
                } else {
                    center.getWorld().spawnParticle(Particle.DUST, ringLoc, 1, 0.05, 0.05, 0.05, 0,
                        new Particle.DustOptions(Color.fromRGB(138, 43, 226), 0.9f)); // Blue Violet
                }
                
                center.getWorld().spawnParticle(Particle.ENCHANT, ringLoc, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }
        
        // Enhancement sparks shooting upward
        for (int i = 0; i < 8; i++) {
            double y = Math.random() * 2 + 0.5;
            Location sparkLoc = itemLoc.clone().add(
                (Math.random() - 0.5) * 0.6,
                y,
                (Math.random() - 0.5) * 0.6
            );
            
            center.getWorld().spawnParticle(Particle.FIREWORK, sparkLoc, 1, 0.1, 0.1, 0.1, 0.1);
            center.getWorld().spawnParticle(Particle.CRIT, sparkLoc, 2, 0.2, 0.2, 0.2, 0.05);
        }
        
        // Central enhancement beam
        center.getWorld().spawnParticle(Particle.END_ROD, itemLoc.clone().add(0, 1, 0), 8, 0.2, 1, 0.2, 0.1);
        center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, itemLoc.clone().add(0, 1.5, 0), 3, 0.3, 0.3, 0.3, 0.05);
    }
}
