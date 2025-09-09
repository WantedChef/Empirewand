package nl.wantedchef.empirewand.spell.toggle.movement;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KajCloudTest {

    private Spell<Void> kajCloud;
    
    @BeforeEach
    void setUp() {
        // Create a new KajCloud spell instance
        kajCloud = new KajCloud.Builder(null).build();
    }
    
    @Test
    void testSpellKey() {
        assertEquals("kaj-cloud", kajCloud.key());
    }
    
    @Test
    void testSpellName() {
        assertEquals("Kaj Cloud", kajCloud.getName());
    }
    
    @Test
    void testSpellDescription() {
        assertEquals("Create beautiful cloud particles under you while flying.", 
                    kajCloud.getDescription());
    }
    
    @Test
    void testSpellType() {
        assertEquals(SpellType.MOVEMENT, kajCloud.type());
    }
    
    @Test
    void testCooldown() {
        assertEquals(java.time.Duration.ofSeconds(5), kajCloud.getCooldown());
    }
}