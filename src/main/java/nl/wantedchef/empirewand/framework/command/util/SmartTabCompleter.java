package nl.wantedchef.empirewand.framework.command.util;

import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import nl.wantedchef.empirewand.framework.command.SubCommand;

import nl.wantedchef.empirewand.spell.SpellTypes;
import org.bukkit.Bukkit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Advanced tab completion system with smart suggestions, fuzzy matching,
 * context-aware completions, and user preference learning.
 */
public class SmartTabCompleter {
    
    private final CommandCache cache;
    private final SpellRegistry spellRegistry;
    private final Map<String, List<String>> userPreferences = new HashMap<>();
    
    // Common completion categories
    private static final List<String> BOOLEAN_VALUES = List.of("true", "false");
    private static final List<String> COMMON_NUMBERS = List.of("1", "5", "10", "30", "60", "300");
    
    public SmartTabCompleter(@NotNull CommandCache cache, @NotNull SpellRegistry spellRegistry) {
        this.cache = cache;
        this.spellRegistry = spellRegistry;
    }
    
    /**
     * Provides smart tab completion with caching, fuzzy matching, and context awareness.
     * 
     * @param sender The command sender
     * @param subCommand The subcommand being completed
     * @param args The current command arguments
     * @param currentArg The argument currently being typed
     * @return List of completion suggestions
     */
    @NotNull
    public List<String> getSmartCompletions(@NotNull CommandSender sender,
                                           @NotNull SubCommand subCommand,
                                           @NotNull String[] args,
                                           @NotNull String currentArg) {
        
        // Check cache first
        List<String> cached = cache.getCachedTabCompletion(currentArg, args);
        if (cached != null) {
            return applyFuzzyMatching(cached, currentArg);
        }
        
        // Generate completions based on context
        List<String> completions = generateContextualCompletions(sender, subCommand, args, currentArg);
        
        // Apply user preferences and learning
        completions = applyUserPreferences(sender.getName(), completions);
        
        // Cache the results
        cache.cacheTabCompletion(currentArg, args, completions);
        
        return applyFuzzyMatching(completions, currentArg);
    }
    
    /**
     * Generates context-aware completions based on the command and argument position.
     */
    @NotNull
    private List<String> generateContextualCompletions(@NotNull CommandSender sender,
                                                      @NotNull SubCommand subCommand,
                                                      @NotNull String[] args,
                                                      @NotNull String currentArg) {
        String commandName = subCommand.getName().toLowerCase();
        int argIndex = args.length - 1;
        
        return switch (commandName) {
            case "bind", "unbind", "set-spell" -> getSpellCompletions(currentArg);
            case "bindtype" -> getSpellTypeCompletions(currentArg);
            case "bindcat", "bindcategory" -> getSpellCategoryCompletions(currentArg);
            case "spells" -> {
                if (argIndex == 1) {
                    yield getSpellTypeCompletions(currentArg);
                }
                yield List.of();
            }
            case "get" -> {
                if (argIndex == 1 && sender instanceof Player) {
                    yield getPlayerCompletions(currentArg);
                }
                yield List.of();
            }
            case "toggle" -> {
                if (argIndex == 1) {
                    yield getToggleSpellCompletions(currentArg);
                } else if (argIndex == 2) {
                    yield BOOLEAN_VALUES;
                }
                yield List.of();
            }
            case "cooldown", "cd" -> {
                if (argIndex == 1) {
                    yield List.of("set", "get", "reset", "clear");
                } else if (argIndex == 2 && args.length > 1 && "set".equals(args[1])) {
                    yield getSpellCompletions(currentArg);
                } else if (argIndex == 3 && args.length > 2 && "set".equals(args[1])) {
                    yield COMMON_NUMBERS;
                }
                yield List.of();
            }
            case "help" -> getHelpCompletions(currentArg);
            case "switcheffect" -> {
                if (argIndex == 1) {
                    yield getEffectTypeCompletions(currentArg);
                }
                yield List.of();
            }
            default -> subCommand.tabComplete(createDummyContext(sender, args));
        };
    }
    
    /**
     * Gets spell name completions with intelligent filtering.
     */
    @NotNull
    private List<String> getSpellCompletions(@NotNull String partial) {
        String cacheKey = "spells:" + partial.toLowerCase();
        List<String> cached = cache.getCachedSpellFilter(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        List<String> allSpells = new ArrayList<>(spellRegistry.getAllSpells().keySet());
        List<String> filtered = allSpells.stream()
            .filter(spell -> spell.toLowerCase().contains(partial.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
        
        // Add display names for better user experience
        Set<String> results = new LinkedHashSet<>(filtered);
        for (String spell : allSpells) {
            String displayName = spellRegistry.getSpellDisplayName(spell);
            if (displayName != null && displayName.toLowerCase().contains(partial.toLowerCase())) {
                results.add(spell);
            }
        }
        
        List<String> finalResults = new ArrayList<>(results);
        cache.cacheSpellFilter(cacheKey, finalResults);
        return finalResults;
    }
    
    /**
     * Gets spell type completions.
     */
    @NotNull
    private List<String> getSpellTypeCompletions(@NotNull String partial) {
        return SpellTypes.validTypeNames().stream()
            .filter(type -> type.toLowerCase().startsWith(partial.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Gets spell category completions (broader than types).
     */
    @NotNull
    private List<String> getSpellCategoryCompletions(@NotNull String partial) {
        List<String> categories = Arrays.asList(
            "combat", "utility", "movement", "defensive", "offensive",
            "fire", "water", "earth", "air", "lightning", "ice",
            "heal", "poison", "dark", "light", "enhanced"
        );
        
        return categories.stream()
            .filter(cat -> cat.toLowerCase().startsWith(partial.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Gets player name completions.
     */
    @NotNull
    private List<String> getPlayerCompletions(@NotNull String partial) {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Gets toggle-able spell completions (spells that can be toggled on/off).
     */
    @NotNull
    private List<String> getToggleSpellCompletions(@NotNull String partial) {
        // Filter for spells that are likely toggle-able
        return spellRegistry.getAllSpells().keySet().stream()
            .filter(spell -> {
                String displayName = spellRegistry.getSpellDisplayName(spell);
                return spell.toLowerCase().contains("toggle") ||
                       spell.toLowerCase().contains("cloak") ||
                       spell.toLowerCase().contains("cloud") ||
                       spell.toLowerCase().contains("aura") ||
                       (displayName != null && (
                           displayName.toLowerCase().contains("toggle") ||
                           displayName.toLowerCase().contains("cloak") ||
                           displayName.toLowerCase().contains("cloud") ||
                           displayName.toLowerCase().contains("aura")
                       ));
            })
            .filter(spell -> spell.toLowerCase().contains(partial.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Gets help command completions.
     */
    @NotNull
    private List<String> getHelpCompletions(@NotNull String partial) {
        List<String> helpCommands = Arrays.asList(
            "get", "bind", "unbind", "bindall", "bindtype", "bindcat",
            "set-spell", "list", "spells", "toggle", "stats", "help",
            "reload", "migrate", "cooldown", "switcheffect"
        );
        
        return helpCommands.stream()
            .filter(cmd -> cmd.toLowerCase().startsWith(partial.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Gets effect type completions for switch effect command.
     */
    @NotNull
    private List<String> getEffectTypeCompletions(@NotNull String partial) {
        List<String> effectTypes = Arrays.asList(
            "particles", "sound", "both", "none",
            "minimal", "normal", "enhanced", "maximum"
        );
        
        return effectTypes.stream()
            .filter(type -> type.toLowerCase().startsWith(partial.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Applies fuzzy matching to filter and rank completion suggestions.
     */
    @NotNull
    private List<String> applyFuzzyMatching(@NotNull List<String> completions, @NotNull String partial) {
        if (partial.isEmpty()) {
            return completions.stream().limit(20).collect(Collectors.toList()); // Limit for performance
        }
        
        String lowerPartial = partial.toLowerCase();
        
        // Score and sort completions
        return completions.stream()
            .map(completion -> new ScoredCompletion(completion, calculateFuzzyScore(completion, lowerPartial)))
            .filter(scored -> scored.score > 0)
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .map(scored -> scored.completion)
            .limit(20) // Limit results for performance
            .collect(Collectors.toList());
    }
    
    /**
     * Calculates a fuzzy matching score for completion ranking.
     */
    private double calculateFuzzyScore(@NotNull String completion, @NotNull String partial) {
        String lowerCompletion = completion.toLowerCase();
        
        // Exact match gets highest score
        if (lowerCompletion.equals(partial)) {
            return 1000.0;
        }
        
        // Starts with gets high score
        if (lowerCompletion.startsWith(partial)) {
            return 500.0 + (100.0 - partial.length()); // Shorter partials rank higher
        }
        
        // Contains gets medium score
        if (lowerCompletion.contains(partial)) {
            int index = lowerCompletion.indexOf(partial);
            return 200.0 - index; // Earlier matches rank higher
        }
        
        // Character overlap gets low score
        double overlap = calculateCharacterOverlap(lowerCompletion, partial);
        return overlap > 0.5 ? overlap * 100.0 : 0.0;
    }
    
    /**
     * Calculates character overlap ratio for fuzzy matching.
     */
    private double calculateCharacterOverlap(@NotNull String str1, @NotNull String str2) {
        Set<Character> chars1 = str1.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
        Set<Character> chars2 = str2.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
        
        Set<Character> intersection = new HashSet<>(chars1);
        intersection.retainAll(chars2);
        
        Set<Character> union = new HashSet<>(chars1);
        union.addAll(chars2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Applies user preferences to reorder completions based on usage history.
     */
    @NotNull
    private List<String> applyUserPreferences(@NotNull String playerName, @NotNull List<String> completions) {
        List<String> userPrefs = userPreferences.get(playerName);
        if (userPrefs == null || userPrefs.isEmpty()) {
            return completions;
        }
        
        // Move preferred completions to the top
        List<String> reordered = new ArrayList<>();
        List<String> remaining = new ArrayList<>(completions);
        
        for (String pref : userPrefs) {
            if (remaining.remove(pref)) {
                reordered.add(pref);
            }
        }
        
        reordered.addAll(remaining);
        return reordered;
    }
    
    /**
     * Records a user's selection to improve future suggestions.
     * 
     * @param playerName The player name
     * @param selection The completion they selected
     */
    public void recordUserSelection(@NotNull String playerName, @NotNull String selection) {
        userPreferences.computeIfAbsent(playerName, k -> new ArrayList<>());
        List<String> prefs = userPreferences.get(playerName);
        
        // Move selection to front, or add if new
        prefs.remove(selection);
        prefs.add(0, selection);
        
        // Keep only recent preferences (last 20)
        if (prefs.size() > 20) {
            prefs.subList(20, prefs.size()).clear();
        }
    }
    
    /**
     * Creates a minimal context for delegating to subcommand tab completion.
     */
    @NotNull
    private nl.wantedchef.empirewand.framework.command.CommandContext createDummyContext(@NotNull CommandSender sender, @NotNull String[] args) {
        // This is a simplified context just for tab completion
        // In a real implementation, you'd need to properly inject dependencies
        return new nl.wantedchef.empirewand.framework.command.CommandContext(
            null, sender, args, null, null, spellRegistry, null, null, null
        );
    }
    
    /**
     * Helper class for scoring completion suggestions.
     */
    private static class ScoredCompletion {
        final String completion;
        final double score;
        
        ScoredCompletion(@NotNull String completion, double score) {
            this.completion = completion;
            this.score = score;
        }
    }
}