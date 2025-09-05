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

import java.util.List;

public class EmpireWandCommand implements CommandExecutor {

    private final EmpireWandPlugin plugin;

    public EmpireWandCommand(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /ew <get|...>").color(NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("get")) {
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

        if (args[0].equalsIgnoreCase("bind")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("This command can only be run by a player.").color(NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(Component.text("Usage: /ew bind <spell>").color(NamedTextColor.RED));
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

            // TODO: Check for permission: empirewand.spell.bind.<spellKey>

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

        if (args[0].equalsIgnoreCase("unbind")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("This command can only be run by a player.").color(NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(Component.text("Usage: /ew unbind <spell>").color(NamedTextColor.RED));
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

        if (args[0].equalsIgnoreCase("bindall")) {
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

            // TODO: Check for permission: empirewand.command.bindall

            plugin.getWandData().setSpells(item, new java.util.ArrayList<>(plugin.getSpellRegistry().getAllSpells().keySet()));
            player.sendMessage(Component.text("Bound all available spells to your wand.").color(NamedTextColor.GREEN));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            // TODO: Check for permission: empirewand.command.reload
            plugin.getConfigService().loadConfigs();
            sender.sendMessage(Component.text("Configuration reloaded.").color(NamedTextColor.GREEN));
            return true;
        }

        return false;
    }
}
