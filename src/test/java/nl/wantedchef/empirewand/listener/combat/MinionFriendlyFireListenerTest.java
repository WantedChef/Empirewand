package nl.wantedchef.empirewand.listener.combat;

import nl.wantedchef.empirewand.core.storage.Keys;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinionFriendlyFireListenerTest {

    private MinionFriendlyFireListener listener;

    @Mock
    private Vex vex;

    @Mock
    private Player player;

    @Mock
    private PersistentDataContainer pdc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new MinionFriendlyFireListener();

        when(vex.getPersistentDataContainer()).thenReturn(pdc);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        // Return owner id for any minion.owner lookup
        when(pdc.get(any(NamespacedKey.class), eq(Keys.STRING_TYPE.getType())))
                .thenReturn(player.getUniqueId().toString());
    }

    @Test
    void onEntityTarget_cancels_whenVexTargetsOwner() {
        EntityTargetEvent event = mock(EntityTargetEvent.class);
        when(event.getEntity()).thenReturn(vex);
        when(event.getTarget()).thenReturn(player);

        listener.onEntityTarget(event);

        verify(event).setCancelled(true);
        verify(event).setTarget(null);
    }

    @Test
    void onDamage_cancels_whenVexDamagesOwner() {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(vex);
        when(event.getEntity()).thenReturn(player);

        listener.onDamage(event);

        verify(event).setCancelled(true);
    }
}


