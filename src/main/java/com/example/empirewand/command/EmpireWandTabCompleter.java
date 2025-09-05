package com.example.empirewand.command;

import com.example.empirewand.EmpireWandPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EmpireWandTabCompleter implements TabCompleter {

    private static final List<String> SUBS = List.of("get", "bind", "unbind", "bindall", "set-spell", "list", "reload", "cooldown");
    private final EmpireWandPlugin plugin;

    public EmpireWandTabCompleter(EmpireWandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBS.stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if (!(sender instanceof Player p)) return List.of();

            if (sub.equals("bind")) {
                ItemStack item = p.getInventory().getItemInMainHand();
                var all = new ArrayList<>(plugin.getSpellRegistry().getAllSpells().keySet());
                if (plugin.getWandData().isWand(item)) {
                    var bound = plugin.getWandData().getSpells(item);
                    all.removeAll(bound);
                }
                return all.stream().filter(k -> k.startsWith(prefix)).sorted().collect(Collectors.toList());
            }
            if (sub.equals("unbind")) {
                ItemStack item = p.getInventory().getItemInMainHand();
                if (!plugin.getWandData().isWand(item)) return List.of();
                return plugin.getWandData().getSpells(item).stream().filter(k -> k.startsWith(prefix)).sorted().collect(Collectors.toList());
            }
            if (sub.equals("set-spell")) {
                ItemStack item = p.getInventory().getItemInMainHand();
                if (!plugin.getWandData().isWand(item)) return List.of();
                return plugin.getWandData().getSpells(item).stream().filter(k -> k.startsWith(prefix)).sorted().collect(Collectors.toList());
            }
            if (sub.equals("cooldown")) {
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(org.bukkit.entity.Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String prefix = args[2].toLowerCase(Locale.ROOT);
            if (sub.equals("cooldown") && "clear".startsWith(prefix)) {
                return List.of("clear");
            }
        }
        return List.of();
    }
}
