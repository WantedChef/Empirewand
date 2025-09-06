package com.example.empirewand.listeners;

import com.example.empirewand.EmpireWandTestBase;
import com.example.empirewand.EmpireWandPlugin;
import com.example.empirewand.core.Keys;
import com.example.empirewand.core.ConfigService;
import org.bukkit.block.Block;
import com.example.empirewand.core.config.ReadableConfig;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class EntityListenerTest extends EmpireWandTestBase {

    @Mock
    private EmpireWandPlugin mockPlugin;
    @Mock
    private ConfigService mockConfig;
    @Mock
    private ReadableConfig mockSpellsConfig;
    @Mock
    private EntityExplodeEvent mockExplodeEvent;
    @Mock
    private Entity mockEntity;
    @Mock
    private PersistentDataContainer mockPdc;

    private EntityListener listener;

    @BeforeEach
    void setup() {
        when(mockPlugin.getConfigService()).thenReturn(mockConfig);
        when(mockConfig.getSpellsConfig()).thenReturn(mockSpellsConfig);
        listener = new EntityListener(mockPlugin);
    }

    @Test
    void explosiveBlocksClearedWhenBlockDamageFalse() {
        // Given
        when(mockExplodeEvent.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getPersistentDataContainer()).thenReturn(mockPdc);
        when(mockPdc.get(Keys.PROJECTILE_SPELL, Keys.STRING_TYPE.getType())).thenReturn("explosive");
        when(mockSpellsConfig.getBoolean("explosive.flags.block-damage", false)).thenReturn(false);

        List<Block> blocks = new ArrayList<>();
        blocks.add(org.mockito.Mockito.mock(Block.class));
        blocks.add(org.mockito.Mockito.mock(Block.class));
        when(mockExplodeEvent.blockList()).thenReturn(blocks);

        // Sanity precondition
        assertEquals(2, blocks.size());

        // When
        listener.onEntityExplode(mockExplodeEvent);

        // Then
        assertEquals(0, blocks.size());
    }
}
