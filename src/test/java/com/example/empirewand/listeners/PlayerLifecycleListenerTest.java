package com.example.empirewand.listeners;

import com.example.empirewand.EmpireWandTestBase;
import com.example.empirewand.core.CooldownService;
import com.example.empirewand.EmpireWandPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerLifecycleListenerTest extends EmpireWandTestBase {

    @Mock private EmpireWandPlugin mockPlugin;
    @Mock private CooldownService mockCooldowns;
    @Mock private Player mockPlayer;
    @Mock private PlayerQuitEvent mockEvent;

    @Test
    void clearsCooldownsOnQuit() {
        // Given
        UUID id = UUID.randomUUID();
        when(mockPlugin.getCooldownService()).thenReturn(mockCooldowns);
        when(mockEvent.getPlayer()).thenReturn(mockPlayer);
        when(mockPlayer.getUniqueId()).thenReturn(id);

        var listener = new PlayerLifecycleListener(mockPlugin);

        // When
        listener.onPlayerQuit(mockEvent);

        // Then
        verify(mockCooldowns).clearAll(id);
    }
}

