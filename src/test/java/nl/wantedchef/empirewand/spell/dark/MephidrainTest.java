package nl.wantedchef.empirewand.spell.dark;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MephidrainTest {

    private Spell<Void> mephidrain;
    
    @BeforeEach
    void setUp() {
        // Create a new Mephidrain spell instance
        mephidrain = new Mephidrain.Builder(null).build();
    }
    
    @Test
    void testSpellKey() {
        assertEquals("mephidrain", mephidrain.key());
    }
    
    @Test
    void testSpellName() {
        assertEquals("Mephidrain", mephidrain.getName());
    }
    
    @Test
    void testSpellDescription() {
        assertEquals("Drain health from nearby enemies and transfer it to yourself.", 
                    mephidrain.getDescription());
    }
    
    @Test
    void testSpellType() {
        assertEquals(SpellType.DARK, mephidrain.type());
    }
    
    @Test
    void testCooldown() {
        assertEquals(java.time.Duration.ofSeconds(12), mephidrain.getCooldown());
    }
}