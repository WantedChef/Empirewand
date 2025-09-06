package com.example.empirewand.command;

import com.example.empirewand.EmpireWandPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public class EmpireWandCommand implements CommandExecutor {

    private final EmpireWandPlugin plugin;
    private final Map<String, SubCommand> subcommands = new LinkedHashMap<>();

    public EmpireWandCommand(EmpireWandPlugin plugin) {
        this.plugin = plugin;
        registerSubcommands();
    }

    private interface SubCommand {
        String name();
        boolean execute(CommandSender sender, String[] args);
    }

    private void registerSubcommands() {
        subcommands.put("get", new SubCommand() {
            public String name() { return "get"; }
            public boolean execute(CommandSender sender, String[] args) { return handleGet(sender, args); }
        });
        subcommands.put("bind", new SubCommand() {
            public String name() { return "bind"; }
            public boolean execute(CommandSender sender, String[] args) { return handleBind(sender, args); }
        });
        subcommands.put("unbind", new SubCommand() {
            public String name() { return "unbind"; }
            public boolean execute(CommandSender sender, String[] args) { return handleUnbind(sender, args); }
        });
        subcommands.put("bindall", new SubCommand() {
            public String name() { return "bindall"; }
            public boolean execute(CommandSender sender, String[] args) { return handleBindAll(sender, args); }
        });
        subcommands.put("set-spell", new SubCommand() {
            public String name() { return "set-spell"; }
            public boolean execute(CommandSender sender, String[] args) { return handleSetSpell(sender, args); }
        });
        subcommands.put("reload", new SubCommand() {
            public String name() { return "reload"; }
            public boolean execute(CommandSender sender, String[] args) { return handleReload(sender, args); }
        });
        subcommands.put("cooldown", new SubCommand() {
            public String name() { return "cooldown"; }
            public boolean execute(CommandSender sender, String[] args) { return handleCooldown(sender, args); }
        });
        subcommands.put("migrate", new SubCommand() {
            public String name() { return "migrate"; }
            public boolean execute(CommandSender sender, String[] args) { return handleMigrate(sender, args); }
        });
        subcommands.put("list", new SubCommand() {
            public String name() { return "list"; }
            public boolean execute(CommandSender sender, String[] args) { return handleList(sender, args); }
        });
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return sendUsage(sender);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        SubCommand handler = subcommands.get(sub);
        if (handler != null) {
            return handler.execute(sender, args);
        }

        return sendUsage(sender);
    }

    private boolean sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /ew <get|bind|unbind|bindall|set-spell|list|reload|cooldown|migrate>").color(NamedTextColor.RED));
        return true;
    }

    private boolean handleGet(CommandSender sender, String[] args) {
        if (!plugin.getPermissionService().has(sender, "empirewand.command.get")) {
            sender.sendMessage(Component.text("No permission").color(NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        var meta = wand.getItemMeta();
        meta.displayName(Component.text("Empire Wand").color(NamedTextColor.GOLD));
        wand.setItemMeta(meta);

        plugin.getWandData().markWand(wand);

        player.getInventory().addItem(wand);
        player.sendMessage(Component.text("You have received an Empire Wand!").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleBind(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ew bind <spell>").color(NamedTextColor.RED));
            return true;
        }
        if (!plugin.getPermissionService().has(sender, "empirewand.command.bind")) {
            sender.sendMessage(Component.text("No permission").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!plugin.getWandData().isWand(item)) {
            player.sendMessage(Component.text("You must be holding a wand to bind a spell.").color(NamedTextColor.RED));
            return true;
        }

        String spellKey = args[1].toLowerCase();
        if (plugin.getSpellRegistry().getSpell(spellKey) == null) {
            player.sendMessage(Component.text("Unknown spell: " + spellKey).color(NamedTextColor.RED));
            return true;
        }

        // Check per-spell bind permission
        if (!plugin.getPermissionService().has(sender, "empirewand.spell.bind." + spellKey)) {
            player.sendMessage(Component.text("No permission to bind this spell").color(NamedTextColor.RED));
            return true;
        }

        List<String> spells = plugin.getWandData().getSpells(item);
        if (spells.contains(spellKey)) {
            player.sendMessage(Component.text("This spell is already bound to your wand.").color(NamedTextColor.YELLOW));
            return true;
        }

        spells.add(spellKey);
        plugin.getWandData().setSpells(item, spells);
        player.sendMessage(Component.text("Bound spell " + spellKey + " to your wand.").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleUnbind(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ew unbind <spell>").color(NamedTextColor.RED));
            return true;
        }
        if (!plugin.getPermissionService().has(sender, "empirewand.command.unbind")) {
            sender.sendMessage(Component.text("No permission").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!plugin.getWandData().isWand(item)) {
            player.sendMessage(Component.text("You must be holding a wand to unbind a spell.").color(NamedTextColor.RED));
            return true;
        }

        String spellKey = args[1].toLowerCase();
        List<String> spells = plugin.getWandData().getSpells(item);

        if (!spells.contains(spellKey)) {
            player.sendMessage(Component.text("That spell is not bound to your wand.").color(NamedTextColor.YELLOW));
            return true;
        }

        spells.remove(spellKey);
        plugin.getWandData().setSpells(item, spells);
        player.sendMessage(Component.text("Unbound spell " + spellKey + " from your wand.").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleBindAll(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!plugin.getWandData().isWand(item)) {
            player.sendMessage(Component.text("You must be holding a wand.").color(NamedTextColor.RED));
            return true;
        }

        if (!plugin.getPermissionService().has(sender, "empirewand.command.bindall")) {
            sender.sendMessage(Component.text("No permission").color(NamedTextColor.RED));
            return true;
        }

        plugin.getWandData().setSpells(item, new java.util.ArrayList<>(plugin.getSpellRegistry().getAllSpells().keySet()));
        player.sendMessage(Component.text("Bound all available spells to your wand.").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetSpell(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ew set-spell <spell>").color(NamedTextColor.RED));
            return true;
        }
        if (!plugin.getPermissionService().has(sender, "empirewand.command.set-spell")) {
            sender.sendMessage(Component.text("No permission").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!plugin.getWandData().isWand(item)) {
            player.sendMessage(Component.text("You must be holding a wand.").color(NamedTextColor.RED));
            return true;
        }

        String spellKey = args[1].toLowerCase();
        List<String> spells = plugin.getWandData().getSpells(item);

        if (!spells.contains(spellKey)) {
            player.sendMessage(Component.text("That spell is not bound to your wand.").color(NamedTextColor.RED));
            return true;
        }

        int index = spells.indexOf(spellKey);
        plugin.getWandData().setActiveIndex(item, index);
        String display = plugin.getConfigService().getSpellsConfig().getString(spellKey + ".display-name", spellKey);
        display = plugin.getTextService().stripMiniTags(display);
        player.sendMessage(Component.text("Active spell set to: ").color(NamedTextColor.GREEN)
                .append(Component.text(display).color(NamedTextColor.AQUA)));
        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!plugin.getPermissionService().has(sender, "empirewand.command.reload")) {
            sender.sendMessage(Component.text("No permission").color(NamedTextColor.RED));
            return true;
        }
        plugin.getConfigService().loadConfigs();
        sender.sendMessage(Component.text("Configuration reloaded.").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleCooldown(CommandSender sender, String[] args) {
        if (!plugin.getPermissionService().has(sender, "empirewand.command.cooldown")) {
            sender.sendMessage(Component.text("No permission").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /ew cooldown <player> clear").color(NamedTextColor.RED));
            return true;
        }
        if (!args[2].equalsIgnoreCase("clear")) {
            sender.sendMessage(Component.text("Usage: /ew cooldown <player> clear").color(NamedTextColor.RED));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found or not online.").color(NamedTextColor.RED));
            return true;
        }

        plugin.getCooldownService().clearAll(target.getUniqueId());
        sender.sendMessage(Component.text("Cleared all cooldowns for ").color(NamedTextColor.GREEN)
                .append(Component.text(target.getName()).color(NamedTextColor.AQUA)));
        return true;
    }

    private boolean handleMigrate(CommandSender sender, String[] args) {
        if (!plugin.getPermissionService().has(sender, "empirewand.command.migrate")) {
            sender.sendMessage(Component.text("No permission").color(NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("Starting configuration migration...").color(NamedTextColor.YELLOW));

        // Run migration in a separate thread to avoid blocking the main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean migrated = plugin.getConfigService().getMigrationService().migrateAllConfigs();
                if (migrated) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(Component.text("Migration completed. Please restart the server for changes to take effect.").color(NamedTextColor.GREEN))
                    );
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(Component.text("No migrations were necessary.").color(NamedTextColor.GREEN))
                    );
                }
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Migration failed", e);
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        sender.sendMessage(Component.text("Migration failed. Check server logs for details.").color(NamedTextColor.RED))
                );
            }
        });

        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.").color(NamedTextColor.RED));
            return true;
        }
        if (!plugin.getPermissionService().has(sender, "empirewand.command.list")) {
            sender.sendMessage(Component.text("No permission").color(NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.getWandData().isWand(item)) {
            player.sendMessage(Component.text("You must be holding a wand.").color(NamedTextColor.RED));
            return true;
        }
        var spells = plugin.getWandData().getSpells(item);
        if (spells.isEmpty()) {
            player.sendMessage(Component.text("No spells bound.").color(NamedTextColor.YELLOW));
            return true;
        }
        int idx = plugin.getWandData().getActiveIndex(item);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spells.size(); i++) {
            if (i > 0) sb.append(", ");
            String key = spells.get(i);
            String display = plugin.getConfigService().getSpellsConfig().getString(key + ".display-name", key);
            if (i == idx) {
                sb.append("[").append(plugin.getTextService().stripMiniTags(display)).append("]");
            } else {
                sb.append(plugin.getTextService().stripMiniTags(display));
            }
        }
        player.sendMessage(Component.text("Spells: ").color(NamedTextColor.GRAY)
                .append(Component.text(sb.toString()).color(NamedTextColor.AQUA)));
        return true;
    }
}
