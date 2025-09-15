package nl.wantedchef.empirewand.gui;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.service.WandService;
import nl.wantedchef.empirewand.api.spell.SpellRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpellBindingMenuTest {

    @Mock
    private EmpireWandPlugin plugin;

    @Mock
    private Logger logger;

    @Mock
    private Player player;

    @Mock
    private Location location;

    @Mock
    private WandService wandService;

    @Mock
    private SpellRegistry spellRegistry;

    @Mock
    private ItemStack heldWand;

    private SpellBindingMenu menu;

    @BeforeEach
    void setUp() {
        menu = new SpellBindingMenu(plugin, logger);
        when(player.getLocation()).thenReturn(location);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(plugin.getWandService()).thenReturn(wandService);
        when(plugin.getSpellRegistry()).thenReturn(spellRegistry);
    }

    @Test
    void openMenuWithoutHeldWandDoesNotThrow() {
        when(wandService.getHeldWand(player)).thenReturn(null);
        assertDoesNotThrow(() -> menu.openMenu(player, "empirewand"));
    }

    @Test
    void openMenuWithHeldWandDoesNotThrow() {
        when(wandService.getHeldWand(player)).thenReturn(heldWand);
        when(wandService.getSpells(heldWand)).thenReturn(java.util.List.of());
        when(spellRegistry.getAllSpells()).thenReturn(Map.of());
        assertDoesNotThrow(() -> menu.openMenu(player, "empirewand"));
    }
}


