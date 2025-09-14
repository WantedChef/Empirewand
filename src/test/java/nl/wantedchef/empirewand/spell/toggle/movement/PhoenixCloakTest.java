package nl.wantedchef.empirewand.spell.toggle.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;

class PhoenixCloakTest {

    private Spell<Void> phoenixCloak;

    @BeforeEach
    void setUp() {
        // Create a new PhoenixCloak spell instance
        phoenixCloak = new PhoenixCloak.Builder(null).build();
    }

    @Test
    void testKey() {
        assertEquals("phoenix-cloak", phoenixCloak.key());
    }

    @Test
    void testName() {
        assertEquals("Phoenix Cloak", phoenixCloak.getName());
    }

    @Test
    void testDescription() {
        assertEquals("Transform into a magnificent phoenix with stunning wings and fire cloak.",
                phoenixCloak.getDescription());
    }

    @Test
    void testType() {
        assertEquals(SpellType.MOVEMENT, phoenixCloak.type());
    }

    @Test
    void testCooldown() {
        assertEquals(java.time.Duration.ofSeconds(8), phoenixCloak.getCooldown());
    }
}