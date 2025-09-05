package com.example.empirewand.core;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {

    public static final NamespacedKey WAND_KEY = new NamespacedKey("empirewand", "wand.key");
    public static final NamespacedKey WAND_SPELLS = new NamespacedKey("empirewand", "wand.spells");
    public static final NamespacedKey WAND_ACTIVE_INDEX = new NamespacedKey("empirewand", "wand.active_index");
    public static final NamespacedKey PROJECTILE_SPELL = new NamespacedKey("empirewand", "projectile.spell");
    public static final NamespacedKey PROJECTILE_OWNER = new NamespacedKey("empirewand", "projectile.owner");
    public static final NamespacedKey ETHEREAL_ACTIVE = new NamespacedKey("empirewand", "ethereal.active");
    public static final NamespacedKey ETHEREAL_EXPIRES_TICK = new NamespacedKey("empirewand", "ethereal.expires_tick");

    private Keys() { }
}
