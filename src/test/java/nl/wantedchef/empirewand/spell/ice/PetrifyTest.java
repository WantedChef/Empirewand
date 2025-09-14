package nl.wantedchef.empirewand.spell.ice;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PetrifyTest {

    private Spell<?> spell;

    @BeforeEach
    void setUp() {
        spell = new Petrify.Builder(null).build();
    }

    @Test
    void testKey() {
        assertEquals("petrify", spell.key());
    }

    @Test
    void testName() {
        assertEquals("Petrify", spell.getName());
    }

    @Test
    void testDescription() {
        assertEquals("Freeze your target solid in ice", spell.getDescription());
    }

    @Test
    void testType() {
        assertEquals(SpellType.ICE, spell.type());
    }

    @Test
    void testCooldown() {
        assertEquals(java.time.Duration.ofSeconds(15), spell.getCooldown());
    }
}

