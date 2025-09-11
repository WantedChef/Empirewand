package nl.wantedchef.empirewand.spell.toggle.movement;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MephiCloudTest {

    private Spell<Void> mephiCloud;
    
    @BeforeEach
    void setUp() {
        // Create a new MephiCloud spell instance
        mephiCloud = new MephiCloud.Builder(null).build();
    }
    
    @Test
    void testSpellKey() {
        assertEquals("mephi-cloud", mephiCloud.key());
    }
    
    @Test
    void testSpellName() {
        assertEquals("Mephi Cloud", mephiCloud.getName());
    }
    
    @Test
    void testSpellDescription() {
        assertEquals("Create nether-themed ash and fire particles under you while flying.", 
                    mephiCloud.getDescription());
    }
    
    @Test
    void testSpellType() {
        assertEquals(SpellType.MOVEMENT, mephiCloud.type());
    }
    
    @Test
    void testCooldown() {
        assertEquals(java.time.Duration.ofSeconds(5), mephiCloud.getCooldown());
    }
}