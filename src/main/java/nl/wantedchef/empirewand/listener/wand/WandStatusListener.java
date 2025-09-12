package nl.wantedchef.empirewand.listener.wand;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.framework.service.CooldownService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listener that manages persistent action bar status updates for wand users.
 * Shows current spell selection or cooldown information continuously.
 */
public final class WandStatusListener implements Listener {
    private final EmpireWandPlugin plugin;
    private final Map<UUID, BukkitTask> activeUpdaters = new HashMap<>();

    public WandStatusListener(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Use async task to avoid blocking main thread during join
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                scheduleStatusUpdates(player);
            }
        }, 5L); // Slight delay to let join process complete
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        stopStatusUpdates(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        // Schedule status updates with a slight delay to ensure the item change is processed
        // Use task manager for better resource tracking
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                scheduleStatusUpdates(player);
            }
        }, 2L);
        plugin.getTaskManager().registerTask(task);
    }

    private void scheduleStatusUpdates(Player player) {
        UUID playerId = player.getUniqueId();

        // Stop any existing updater for this player
        stopStatusUpdates(playerId);

        // Only start updates if player is holding a wand
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!plugin.getWandService().isWand(heldItem)) {
            return;
        }

        // Create a new repeating task that updates every 10 ticks (0.5 seconds)
        BukkitTask task = new BukkitRunnable() {
            private int updateCount = 0;
            
            @Override
            public void run() {
                try {
                    // Quick validation checks for performance
                    if (!player.isOnline() || !player.isValid()) {
                        cancel();
                        activeUpdaters.remove(playerId);
                        return;
                    }

                    ItemStack currentItem = player.getInventory().getItemInMainHand();
                    if (!plugin.getWandService().isWand(currentItem)) {
                        // Player is no longer holding a wand, stop updates
                        cancel();
                        activeUpdaters.remove(playerId);
                        return;
                    }

                    updateWandStatus(player, currentItem);
                    
                    // Periodic cleanup to prevent memory leaks
                    updateCount++;
                    if (updateCount > 1200) { // Every 10 minutes (1200 * 0.5s)
                        // Restart task to clean up any potential memory issues
                        cancel();
                        activeUpdaters.remove(playerId);
                        scheduleStatusUpdates(player);
                        return;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning(String.format("Error in WandStatusListener for player %s: %s", 
                        player.getName(), e.getMessage()));
                    cancel();
                    activeUpdaters.remove(playerId);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Run immediately, then every 10 ticks

        activeUpdaters.put(playerId, task);
    }

    private void stopStatusUpdates(UUID playerId) {
        BukkitTask existingTask = activeUpdaters.remove(playerId);
        if (existingTask != null && !existingTask.isCancelled()) {
            existingTask.cancel();
        }
    }

    private void updateWandStatus(Player player, ItemStack wand) {
        List<String> spells = new ArrayList<>(plugin.getWandService().getSpells(wand));
        if (spells.isEmpty()) {
            plugin.getFxService().actionBar(player, "§7No spells bound");
            return;
        }

        int activeIndex = Math.max(0, Math.min(plugin.getWandService().getActiveIndex(wand), spells.size() - 1));
        String currentSpellKey = spells.get(activeIndex);
        String displayName = plugin.getSpellRegistry().getSpellDisplayName(currentSpellKey);

        // Check if the current spell is on cooldown
        CooldownService cooldownService = plugin.getCooldownService();
        long currentTicks = player.getWorld().getFullTime();

        if (cooldownService.isOnCooldown(player.getUniqueId(), currentSpellKey, currentTicks, wand)) {
            // Show cooldown information
            long remainingTicks = cooldownService.remaining(player.getUniqueId(), currentSpellKey, currentTicks, wand);
            double remainingSeconds = remainingTicks / 20.0;

            String message = String.format("§c%s §7(%.1fs)", displayName, remainingSeconds);
            plugin.getFxService().actionBar(player, message);
        } else {
            // Show current spell ready to cast
            String message = String.format("§a%s §7(%d/%d)", displayName, activeIndex + 1, spells.size());
            plugin.getFxService().actionBar(player, message);
        }
    }

    /**
     * Cleanup method to stop all active updaters when the plugin is disabled.
     */
    public void shutdown() {
        for (BukkitTask task : activeUpdaters.values()) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        activeUpdaters.clear();
    }
}





