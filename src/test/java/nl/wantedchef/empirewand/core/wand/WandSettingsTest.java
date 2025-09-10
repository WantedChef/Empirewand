package nl.wantedchef.empirewand.core.wand;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WandSettingsTest {

    @Test
    void testWandSettingsClassExists() {
        // This is a simple test to verify the class can be loaded
        // In a real test, we would mock the ItemStack and test the actual functionality
        assertNotNull(WandSettings.class);
    }
}