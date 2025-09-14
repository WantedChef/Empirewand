package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.weather.MeteorShower;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MeteorShowerTest {

    private Spell<Void> meteorShower;
    
    @BeforeEach
    void setUp() {
        // Create a new MeteorShower spell instance
        meteorShower = new MeteorShower.Builder(null).build();
    }
    
    @Test
    void testSpellKey() {
        assertEquals("meteor-shower", meteorShower.key());
    }
    
    @Test
    void testSpellName() {
        assertEquals("Meteor Shower", meteorShower.getName());
    }
    
    @Test
    void testSpellDescription() {
        assertEquals("Calls down a devastating meteor shower that rains destruction upon your enemies.", 
                    meteorShower.getDescription());
    }
    
    @Test
    void testSpellType() {
        assertEquals(SpellType.FIRE, meteorShower.type());
    }
    
    @Test
    void testCooldown() {
        assertEquals(java.time.Duration.ofSeconds(45), meteorShower.getCooldown());
    }
}