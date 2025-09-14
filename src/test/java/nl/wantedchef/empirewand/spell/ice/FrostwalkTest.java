package nl.wantedchef.empirewand.spell.ice;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrostwalkTest {

    private Spell<?> spell;

    @BeforeEach
    void setUp() {
        spell = new Frostwalk.Builder(null).build();
    }

    @Test
    void testKey() {
        assertEquals("frostwalk", spell.key());
    }

    @Test
    void testName() {
        assertEquals("Frostwalk", spell.getName());
    }

    @Test
    void testDescription() {
        assertEquals("Freeze water beneath your feet and slow nearby enemies", spell.getDescription());
    }

    @Test
    void testType() {
        assertEquals(SpellType.ICE, spell.type());
    }

    @Test
    void testCooldown() {
        assertEquals(java.time.Duration.ofSeconds(10), spell.getCooldown());
    }
}

