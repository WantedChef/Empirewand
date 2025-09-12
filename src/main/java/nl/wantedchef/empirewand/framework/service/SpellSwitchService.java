package nl.wantedchef.empirewand.framework.service;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.service.WandService;
import nl.wantedchef.empirewand.core.wand.WandSettings;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Service that manages spell switching effects and related functionality.
 */
public class SpellSwitchService {

    private final EmpireWandPlugin plugin;
    private final WandService wandService;
    private final Map<UUID, Long> lastSwitchTime = new WeakHashMap<>();
    private final Map<String, Consumer<Player>> effects;

    private static final Set<String> AVAILABLE_EFFECTS =
            Set.of("default", "spiral", "explosion", "portal", "fire", "ice", "lightning", "nether",
                    "enchant", "hearts", "music", "ender", "dragon", "void", "lapis", "redstone", "emerald", "gold", "iron");

    public SpellSwitchService(EmpireWandPlugin plugin, WandService wandService) {
        this.plugin = plugin;
        this.wandService = wandService;
        this.effects = Map.ofEntries(
                Map.entry("spiral", this::playSpiralEffect),
                Map.entry("explosion", this::playExplosionEffect),
                Map.entry("portal", this::playPortalEffect),
                Map.entry("fire", this::playFireEffect),
                Map.entry("ice", this::playIceEffect),
                Map.entry("lightning", this::playLightningEffect),
                Map.entry("nether", this::playNetherEffect),
                Map.entry("enchant", this::playEnchantEffect),
                Map.entry("hearts", this::playHeartsEffect),
                Map.entry("music", this::playMusicEffect),
                Map.entry("ender", this::playEnderEffect),
                Map.entry("dragon", this::playDragonEffect),
                Map.entry("void", this::playVoidEffect),
                Map.entry("lapis", p -> playColorDustEffect(p, Color.BLUE)),
                Map.entry("redstone", p -> playColorDustEffect(p, Color.RED)),
                Map.entry("emerald", p -> playColorDustEffect(p, Color.GREEN)),
                Map.entry("gold", p -> playColorDustEffect(p, Color.YELLOW)),
                Map.entry("iron", p -> playColorDustEffect(p, Color.SILVER))
        );
    }

    /**
     * Gets the list of available switch effects.
     *
     * @return Set of available effect names
     */
    public Set<String> getAvailableEffects() {
        return Collections.unmodifiableSet(AVAILABLE_EFFECTS);
    }

    /**
     * Plays the spell switch effect for a player's wand.
     *
     * @param player The player
     * @param wand   The wand item
     */
    public void playSpellSwitchEffect(Player player, ItemStack wand) {
        if (isRateLimited(player.getUniqueId())) {
            return;
        }

        WandSettings settings = new WandSettings(wand);
        String effect = settings.getSpellSwitchEffect().toLowerCase();

        Consumer<Player> action = effects.get(effect);
        if (action != null) {
            action.accept(player);
        } else {
            playDefaultEffect(player, wand);
        }
    }

    private boolean isRateLimited(UUID playerId) {
        long now = System.currentTimeMillis();
        Long lastSwitch = lastSwitchTime.get(playerId);
        if (lastSwitch != null && (now - lastSwitch) < 100) { // 100ms cooldown
            return true;
        }
        lastSwitchTime.put(playerId, now);
        return false;
    }

    private void playColorDustEffect(Player player, Color color) {
        Location loc = player.getLocation().add(0, 0.5, 0);
        World world = player.getWorld();
        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.5f);
        world.spawnParticle(Particle.DUST, loc, 50, 0.3, 0.3, 0.3, 0.15, dustOptions);
        world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
    }

    private void playDefaultEffect(Player player, ItemStack wand) {
        if (wandService.isMephidantesZeist(wand)) {
            playColorDustEffect(player, Color.RED);
        } else {
            playColorDustEffect(player, Color.BLUE);
        }
    }

    private void playSpiralEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        new BukkitRunnable() {
            int step = 0;
            final int maxSteps = 20;

            @Override
            public void run() {
                if (step >= maxSteps) {
                    cancel();
                    return;
                }

                double height = step * 0.1;
                double radius = 0.5 + (step * 0.05);
                double angle = step * 0.5;

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location particleLoc = loc.clone().add(x, height, z);
                world.spawnParticle(Particle.ENCHANT, particleLoc, 2, 0, 0, 0, 0);

                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.2f);
    }

    private void playExplosionEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        world.spawnParticle(Particle.EXPLOSION, loc, 3, 0, 0, 0, 0);

        new BukkitRunnable() {
            int radius = 1;
            final int maxRadius = 3;

            @Override
            public void run() {
                if (radius > maxRadius) {
                    cancel();
                    return;
                }
                spawnParticleCircle(loc, Particle.FLAME, 12, radius);
                radius++;
            }
        }.runTaskTimer(plugin, 2L, 2L);

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.8f);
    }

    private void playPortalEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        for (int i = 0; i < 20; i++) {
            double angle = 2 * Math.PI * i / 20;
            double x = Math.cos(angle) * 1.0;
            double z = Math.sin(angle) * 1.0;
            Location particleLoc = loc.clone().add(x, Math.sin(i * 0.3) * 0.3, z);
            world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.0f);
    }

    private void playFireEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        spawnParticleCircle(loc, Particle.FLAME, 30, 0.7, 0.5);
        player.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.2f);
    }

    private void playIceEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        spawnParticleCircle(loc, Particle.SNOWFLAKE, 25, 0.8, 0.3);
        player.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.5f);
    }

    private void playLightningEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        for (int i = 0; i < 15; i++) {
            Location particleLoc = loc.clone().add((Math.random() - 0.5) * 1.5,
                    (Math.random() - 0.5) * 1.0, (Math.random() - 0.5) * 1.5);
            world.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 2, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.8f);
    }

    private void playNetherEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        for (int i = 0; i < 20; i++) {
            Location particleLoc = loc.clone().add((Math.random() - 0.5) * 1.2,
                    (Math.random() - 0.5) * 0.8, (Math.random() - 0.5) * 1.2);
            world.spawnParticle(Math.random() < 0.5 ? Particle.ASH : Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.AMBIENT_NETHER_WASTES_LOOP, 0.6f, 0.9f);
    }

    private void playEnchantEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        spawnParticleCircle(loc, Particle.ENCHANT, 35, 0.5 + (Math.random() * 0.8), 0.5);
        player.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.7f, 1.3f);
    }

    private void playHeartsEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        for (int i = 0; i < 12; i++) {
            Location particleLoc = loc.clone().add((Math.random() - 0.5) * 1.0,
                    (Math.random() - 0.5) * 0.5, (Math.random() - 0.5) * 1.0);
            world.spawnParticle(Particle.HEART, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.8f);
    }

    private void playMusicEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        for (int i = 0; i < 18; i++) {
            double angle = 2 * Math.PI * i / 18;
            double x = Math.cos(angle) * 0.9;
            double z = Math.sin(angle) * 0.9;
            Location particleLoc = loc.clone().add(x, Math.sin(i * 0.4) * 0.2, z);
            world.spawnParticle(Particle.NOTE, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.2f);
    }

    private void playEnderEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        spawnParticleCircle(loc, Particle.REVERSE_PORTAL, 22, 0.6 + (Math.random() * 0.5), 0.4);
        player.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.0f);
    }

    private void playDragonEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        for (int i = 0; i < 16; i++) {
            Location particleLoc = loc.clone().add((Math.random() - 0.5) * 1.3,
                    (Math.random() - 0.5) * 0.7, (Math.random() - 0.5) * 1.3);
            world.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.9f);
    }

    private void playVoidEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        spawnParticleCircle(loc, Particle.SQUID_INK, 25, 0.4 + (Math.random() * 0.7), 0.6);
        player.getWorld().playSound(loc, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, 0.6f, 0.7f);
    }

    private void spawnParticleCircle(Location center, Particle particle, int count, double radius) {
        spawnParticleCircle(center, particle, count, radius, 0.0);
    }

    private void spawnParticleCircle(Location center, Particle particle, int count, double radius, double yOffsetRange) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = yOffsetRange == 0.0 ? 0 : (Math.random() - 0.5) * yOffsetRange;
            Location particleLoc = center.clone().add(x, y, z);
            world.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
        }
    }
}
