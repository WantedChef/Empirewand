package nl.wantedchef.empirewand.spell.ice;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IceWallTest {

    private Spell<?> spell;

    @BeforeEach
    void setUp() {
        spell = new IceWall.Builder(null).build();
    }

    @Test
    void testKey() {
        assertEquals("icewall", spell.key());
    }

    @Test
    void testName() {
        assertEquals("Ice Wall", spell.getName());
    }

    @Test
    void testDescription() {
        assertEquals("Create a protective wall of ice", spell.getDescription());
    }

    @Test
    void testType() {
        assertEquals(SpellType.ICE, spell.type());
    }

    @Test
    void testCooldown() {
        assertEquals(java.time.Duration.ofSeconds(20), spell.getCooldown());
    }
}

