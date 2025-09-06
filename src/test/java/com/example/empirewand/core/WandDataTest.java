package com.example.empirewand.core;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.empirewand.EmpireWandTestBase;

/**
 * Unit tests for WandData.
 */
class WandDataTest extends EmpireWandTestBase {

    @Mock
    private Player mockPlayer;

    @Mock
    private PlayerInventory mockInventory;

    @Mock
    private ItemMeta mockItemMeta;

    @Mock
    private PersistentDataContainer mockPDC;

    private WandData wandData;
    private ItemStack mockWand;

    @BeforeEach
    void setUp() {
        wandData = new WandData(null); // Plugin not needed for these tests
        mockWand = mock(ItemStack.class);

        when(mockPlayer.getInventory()).thenReturn(mockInventory);
        when(mockWand.getItemMeta()).thenReturn(mockItemMeta);
        when(mockItemMeta.getPersistentDataContainer()).thenReturn(mockPDC);
    }

    @Test
    void testCreateWand() {
        // When
        ItemStack wand = wandData.createWand();

        // Then
        assertNotNull(wand);
        assertEquals(Material.STICK, wand.getType());
        // Note: Further verification would require ItemMeta mocking
    }

    @Test
    void testIsWandTrue() {
        // Given
        when(mockPDC.has(Keys.WAND_KEY, PersistentDataType.BYTE)).thenReturn(true);

        // When
        boolean result = wandData.isWand(mockWand);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsWandFalse() {
        // Given
        when(mockPDC.has(Keys.WAND_KEY, PersistentDataType.BYTE)).thenReturn(false);

        // When
        boolean result = wandData.isWand(mockWand);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsWandNullItem() {
        // When
        boolean result = wandData.isWand(null);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsWandNullMeta() {
        // Given
        when(mockWand.getItemMeta()).thenReturn(null);

        // When
        boolean result = wandData.isWand(mockWand);

        // Then
        assertFalse(result);
    }

    @Test
    void testMarkWand() {
        // When
        wandData.markWand(mockWand);

        // Then
        verify(mockPDC).set(Keys.WAND_KEY, PersistentDataType.BYTE, (byte) 1);
        verify(mockWand).setItemMeta(mockItemMeta);
    }

    @Test
    void testMarkWandNullMeta() {
        // Given
        when(mockWand.getItemMeta()).thenReturn(null);

        // When
        wandData.markWand(mockWand);

        // Then
        // Should not throw exception, just return early
        verify(mockWand).setItemMeta(null);
    }

    @Test
    void testGetBoundSpells() {
        // Given
        String csv = "fireball,heal,lightning";
        when(mockPDC.get(Keys.WAND_SPELLS, PersistentDataType.STRING)).thenReturn(csv);

        // When
        List<String> spells = wandData.getBoundSpells(mockWand);

        // Then
        assertEquals(List.of("fireball", "heal", "lightning"), spells);
    }

    @Test
    void testGetBoundSpellsEmpty() {
        // Given
        when(mockPDC.get(Keys.WAND_SPELLS, PersistentDataType.STRING)).thenReturn(null);

        // When
        List<String> spells = wandData.getBoundSpells(mockWand);

        // Then
        assertTrue(spells.isEmpty());
    }

    @Test
    void testBindSpellSuccess() {
        // Given
        when(mockPDC.get(Keys.WAND_SPELLS, PersistentDataType.STRING)).thenReturn("fireball,heal");

        // When
        boolean result = wandData.bindSpell(mockWand, "lightning");

        // Then
        assertTrue(result);
        verify(mockPDC).set(Keys.WAND_SPELLS, PersistentDataType.STRING, "fireball,heal,lightning");
    }

    @Test
    void testBindSpellAlreadyBound() {
        // Given
        when(mockPDC.get(Keys.WAND_SPELLS, PersistentDataType.STRING)).thenReturn("fireball,heal");

        // When
        boolean result = wandData.bindSpell(mockWand, "fireball");

        // Then
        assertFalse(result);
    }

    @Test
    void testBindSpellEmptyList() {
        // Given
        when(mockPDC.get(Keys.WAND_SPELLS, PersistentDataType.STRING)).thenReturn(null);

        // When
        boolean result = wandData.bindSpell(mockWand, "fireball");

        // Then
        assertTrue(result);
        verify(mockPDC).set(Keys.WAND_SPELLS, PersistentDataType.STRING, "fireball");
    }

    @Test
    void testUnbindSpellSuccess() {
        // Given
        when(mockPDC.get(Keys.WAND_SPELLS, PersistentDataType.STRING)).thenReturn("fireball,heal,lightning");

        // When
        boolean result = wandData.unbindSpell(mockWand, "heal");

        // Then
        assertTrue(result);
        verify(mockPDC).set(Keys.WAND_SPELLS, PersistentDataType.STRING, "fireball,lightning");
    }

    @Test
    void testUnbindSpellNotBound() {
        // Given
        when(mockPDC.get(Keys.WAND_SPELLS, PersistentDataType.STRING)).thenReturn("fireball,heal");

        // When
        boolean result = wandData.unbindSpell(mockWand, "lightning");

        // Then
        assertFalse(result);
    }

    @Test
    void testSetActiveSpellValidIndex() {
        // Given
        when(mockPDC.get(Keys.WAND_SPELLS, PersistentDataType.STRING)).thenReturn("fireball,heal,lightning");

        // When
        boolean result = wandData.setActiveSpell(mockWand, 1);

        // Then
        assertTrue(result);
        verify(mockPDC).set(Keys.WAND_ACTIVE_INDEX, PersistentDataType.INTEGER, 1);
    }

    @Test
    void testSetActiveSpellInvalidIndex() {
        // Given
        when(mockPDC.get(Keys.WAND_SPELLS, PersistentDataType.STRING)).thenReturn("fireball,heal");

        // When
        boolean result = wandData.setActiveSpell(mockWand, 5);

        // Then
        assertFalse(result);
    }

    @Test
    void testSetActiveSpellNegativeIndex() {
        // Given
        when(mockPDC.get(Keys.WAND_SPELLS, PersistentDataType.STRING)).thenReturn("fireball,heal");

        // When
        boolean result = wandData.setActiveSpell(mockWand, -1);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetActiveSpellIndex() {
        // Given
        when(mockPDC.get(Keys.WAND_ACTIVE_INDEX, PersistentDataType.INTEGER)).thenReturn(2);

        // When
        int result = wandData.getActiveSpellIndex(mockWand);

        // Then
        assertEquals(2, result);
    }

    @Test
    void testGetActiveSpellIndexNull() {
        // Given
        when(mockPDC.get(Keys.WAND_ACTIVE_INDEX, PersistentDataType.INTEGER)).thenReturn(null);

        // When
        int result = wandData.getActiveSpellIndex(mockWand);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testGetActiveSpellIndexNullMeta() {
        // Given
        when(mockWand.getItemMeta()).thenReturn(null);

        // When
        int result = wandData.getActiveSpellIndex(mockWand);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testGetActiveSpellKey() {
        // Given
        when(mockPDC.get(Keys.WAND_SPELLS, PersistentDataType.STRING)).thenReturn("fireball,heal,lightning");
        when(mockPDC.get(Keys.WAND_ACTIVE_INDEX, PersistentDataType.INTEGER)).thenReturn(1);

        // When
        String result = wandData.getActiveSpellKey(mockWand);

        // Then
        assertEquals("heal", result);
    }

    @Test
    void testGetActiveSpellKeyInvalidIndex() {
        // Given
        when(mockPDC.get(Keys.WAND_SPELLS, PersistentDataType.STRING)).thenReturn("fireball,heal");
        when(mockPDC.get(Keys.WAND_ACTIVE_INDEX, PersistentDataType.INTEGER)).thenReturn(5);

        // When
        String result = wandData.getActiveSpellKey(mockWand);

        // Then
        assertNull(result);
    }

    @Test
    void testGetHeldWand() {
        // Given
        ItemStack heldItem = mock(ItemStack.class);
        when(mockInventory.getItemInMainHand()).thenReturn(heldItem);
        when(heldItem.getItemMeta()).thenReturn(mockItemMeta);
        when(mockPDC.has(Keys.WAND_KEY, PersistentDataType.BYTE)).thenReturn(true);

        // When
        ItemStack result = wandData.getHeldWand(mockPlayer);

        // Then
        assertEquals(heldItem, result);
    }

    @Test
    void testGetHeldWandNotWand() {
        // Given
        ItemStack heldItem = mock(ItemStack.class);
        when(mockInventory.getItemInMainHand()).thenReturn(heldItem);
        when(heldItem.getItemMeta()).thenReturn(mockItemMeta);
        when(mockPDC.has(Keys.WAND_KEY, PersistentDataType.BYTE)).thenReturn(false);

        // When
        ItemStack result = wandData.getHeldWand(mockPlayer);

        // Then
        assertNull(result);
    }

    @Test
    void testGiveWand() {
        // Given
        when(mockInventory.addItem(any(ItemStack.class))).thenReturn(null); // No overflow

        // When
        boolean result = wandData.giveWand(mockPlayer);

        // Then
        assertTrue(result);
        verify(mockInventory).addItem(any(ItemStack.class));
    }
}