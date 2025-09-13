package nl.wantedchef.empirewand.spell.toggle.movement;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DragonFuryTest {

    private Spell<Void> dragonFury;
    
    @BeforeEach
    void setUp() {
        // Create a new DragonFury spell instance
        dragonFury = new DragonFury.Builder(null).build();
    }
    
    @Test
    void testSpellKey() {
        assertEquals("dragon-fury", dragonFury.key());
    }
    
    @Test
    void testSpellName() {
        assertEquals("Dragon Fury", dragonFury.getName());
    }
    
    @Test
    void testSpellDescription() {
        assertEquals("Transform into a mighty dragon with scale armor, flame breath, and devastating fury.", 
                    dragonFury.getDescription());
    }
    
    @Test
    void testSpellType() {
        assertEquals(SpellType.MOVEMENT, dragonFury.type());
    }
    
    @Test
    void testCooldown() {
        assertEquals(java.time.Duration.ofSeconds(15), dragonFury.getCooldown());
    }
}