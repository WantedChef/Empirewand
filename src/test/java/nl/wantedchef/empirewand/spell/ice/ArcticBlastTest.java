package nl.wantedchef.empirewand.spell.ice;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArcticBlastTest {

    private Spell<?> spell;

    @BeforeEach
    void setUp() {
        spell = new ArcticBlast.Builder(null).build();
    }

    @Test
    void testKey() {
        assertEquals("arctic-blast", spell.key());
    }

    @Test
    void testName() {
        assertEquals("Arctic Blast", spell.getName());
    }

    @Test
    void testDescription() {
        assertEquals("Unleash a devastating arctic explosion", spell.getDescription());
    }

    @Test
    void testType() {
        assertEquals(SpellType.ICE, spell.type());
    }

    @Test
    void testCooldown() {
        assertEquals(java.time.Duration.ofSeconds(30), spell.getCooldown());
    }
}

