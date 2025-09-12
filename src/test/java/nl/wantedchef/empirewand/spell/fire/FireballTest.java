package nl.wantedchef.empirewand.spell.fire;

import nl.wantedchef.empirewand.api.EmpireWandAPI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.MockitoAnnotations.openMocks;

class FireballTest {

    private Fireball fireball;
    
    @Mock
    private EmpireWandAPI api;

    @BeforeEach
    void setUp() {
        openMocks(this);
        fireball = (Fireball) new Fireball.Builder(api).build();
    }

    @Test
    @DisplayName("Test fireball spell key")
    void testSpellKey() {
        assertEquals("fireball", fireball.key());
    }

    @Test
    @DisplayName("Test fireball spell prerequisites")
    void testSpellPrerequisites() {
        assertNotNull(fireball.prereq());
    }

    @Test
    @DisplayName("Test fireball spell type")
    void testSpellType() {
        assertNotNull(fireball.type());
    }

    @Test
    @DisplayName("Test fireball spell cooldown")
    void testSpellCooldown() {
        assertNotNull(fireball.getCooldown());
    }
}