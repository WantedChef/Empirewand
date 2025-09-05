package com.example.empirewand.spell.implementation;

import com.example.empirewand.core.Keys;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.persistence.PersistentDataType;

public class Explosive implements Spell {
    @Override
    public void execute(SpellContext context) {
        Player player = context.caster();
        WitherSkull skull = player.launchProjectile(WitherSkull.class);
        skull.setYield(4.0f);
        skull.setIsIncendiary(false);
        skull.getPersistentDataContainer().set(Keys.PROJECTILE_SPELL, PersistentDataType.STRING, getName());
        skull.getPersistentDataContainer().set(Keys.PROJECTILE_OWNER, PersistentDataType.STRING, player.getUniqueId().toString());
        // TODO: Add trail effect
    }

    @Override
    public String getName() {
        return "explosive";
    }
}
