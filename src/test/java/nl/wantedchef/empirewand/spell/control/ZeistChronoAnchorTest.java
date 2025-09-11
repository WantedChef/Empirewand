package nl.wantedchef.empirewand.spell.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZeistChronoAnchorTest {

    @Test
    void builderBuildsSpell() {
        ZeistChronoAnchor spell = new ZeistChronoAnchor.Builder(null).build();
        assertNotNull(spell);
        assertEquals("zeist-chrono-anchor", spell.key());
    }
}
