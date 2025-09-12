package nl.wantedchef.empirewand.spell.fire;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FlameWaveRefactoredTest {

    private Spell<Void> flameWave;
    
    @BeforeEach
    void setUp() {
        // Create a new FlameWaveRefactored spell instance
        flameWave = new FlameWaveRefactored.Builder(null).build();
    }
    
    @Test
    void testSpellKey() {
        assertEquals("flame-wave", flameWave.key());
    }
    
    @Test
    void testSpellName() {
        assertEquals("Flame Wave", flameWave.getName());
    }
    
    @Test
    void testSpellDescription() {
        assertEquals("Unleashes a wave of fire in a cone.", 
                    flameWave.getDescription());
    }
    
    @Test
    void testSpellType() {
        assertEquals(SpellType.FIRE, flameWave.type());
    }
    
    @Test
    void testCooldown() {
        assertEquals(java.time.Duration.ofSeconds(6), flameWave.getCooldown());
    }
}