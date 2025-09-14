package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BlizzardTest {

    private Spell<Void> blizzard;
    
    @BeforeEach
    void setUp() {
        // Create a new Blizzard spell instance
        blizzard = new Blizzard.Builder(null).build();
    }
    
    @Test
    void testSpellKey() {
        assertEquals("blizzard", blizzard.key());
    }
    
    @Test
    void testSpellName() {
        assertEquals("Glacial Wrath", blizzard.getName());
    }
    
    @Test
    void testSpellDescription() {
        assertEquals("Creates a devastating blizzard that slows and damages enemies while covering the area in ice.", 
                    blizzard.getDescription());
    }
    
    @Test
    void testSpellType() {
        assertEquals(SpellType.ICE, blizzard.type());
    }
    
    @Test
    void testCooldown() {
        assertEquals(java.time.Duration.ofSeconds(55), blizzard.getCooldown());
    }
}