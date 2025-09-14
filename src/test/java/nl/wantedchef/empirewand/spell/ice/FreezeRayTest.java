package nl.wantedchef.empirewand.spell.ice;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FreezeRayTest {

    private Spell<?> spell;

    @BeforeEach
    void setUp() {
        spell = new FreezeRay.Builder(null).build();
    }

    @Test
    void testKey() {
        assertEquals("freeze-ray", spell.key());
    }

    @Test
    void testName() {
        assertEquals("Freeze Ray", spell.getName());
    }

    @Test
    void testDescription() {
        assertEquals("Fire a continuous freezing beam", spell.getDescription());
    }

    @Test
    void testType() {
        assertEquals(SpellType.ICE, spell.type());
    }

    @Test
    void testCooldown() {
        assertEquals(java.time.Duration.ofSeconds(12), spell.getCooldown());
    }
}

