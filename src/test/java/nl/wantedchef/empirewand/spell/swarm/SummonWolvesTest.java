package nl.wantedchef.empirewand.spell.swarm;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.swarns.SummonWolves;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SummonWolvesTest {
    private Spell<Void> spell;

    @BeforeEach
    void setUp() {
        spell = new SummonWolves.Builder(null).build();
    }

    @Test void testKey() { assertEquals("summon-wolves", spell.key()); }
    @Test void testName() { assertEquals("Summon Wolves", spell.getName()); }
    @Test void testDescription() { assertEquals("Summons a pack of wolves that defend you.", spell.getDescription()); }
    @Test void testType() { assertEquals(SpellType.LIFE, spell.type()); }
    @Test void testCooldown() { assertEquals(java.time.Duration.ofSeconds(55), spell.getCooldown()); }
}
