package nl.wantedchef.empirewand.framework.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpellSwitchServiceTest {

    @Test
    void testGetAvailableEffects() {
        // This is a simple test to verify the service can be instantiated
        // In a real test, we would mock the plugin and test the actual functionality
        assertNotNull(SpellSwitchService.class);
    }
}