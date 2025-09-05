package com.example.empirewand.core;

import com.example.empirewand.EmpireWandTestBase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PermissionService.
 */
class PermissionServiceTest extends EmpireWandTestBase {

    @Mock
    private Player mockPlayer;

    @Mock
    private CommandSender mockCommandSender;

    @Test
    void testHasPermission() {
        // Given
        PermissionService permissionService = new PermissionService();
        when(mockCommandSender.hasPermission("test.permission")).thenReturn(true);

        // When
        boolean result = permissionService.has(mockCommandSender, "test.permission");

        // Then
        assertTrue(result);
        verify(mockCommandSender).hasPermission("test.permission");
    }

    @Test
    void testHasNoPermission() {
        // Given
        PermissionService permissionService = new PermissionService();
        when(mockCommandSender.hasPermission("test.permission")).thenReturn(false);

        // When
        boolean result = permissionService.has(mockCommandSender, "test.permission");

        // Then
        assertFalse(result);
        verify(mockCommandSender).hasPermission("test.permission");
    }

    @Test
    void testCanUseSpell() {
        // Given
        PermissionService permissionService = new PermissionService();
        String spellKey = "magic-missile";
        String expectedPermission = "empirewand.spell.use." + spellKey;
        when(mockPlayer.hasPermission(expectedPermission)).thenReturn(true);

        // When
        boolean result = permissionService.canUseSpell(mockPlayer, spellKey);

        // Then
        assertTrue(result);
        verify(mockPlayer).hasPermission(expectedPermission);
    }

    @Test
    void testCannotUseSpell() {
        // Given
        PermissionService permissionService = new PermissionService();
        String spellKey = "magic-missile";
        String expectedPermission = "empirewand.spell.use." + spellKey;
        when(mockPlayer.hasPermission(expectedPermission)).thenReturn(false);

        // When
        boolean result = permissionService.canUseSpell(mockPlayer, spellKey);

        // Then
        assertFalse(result);
        verify(mockPlayer).hasPermission(expectedPermission);
    }

    @Test
    void testCanBindSpell() {
        // Given
        PermissionService permissionService = new PermissionService();
        String spellKey = "magic-missile";
        String expectedPermission = "empirewand.spell.bind." + spellKey;
        when(mockPlayer.hasPermission(expectedPermission)).thenReturn(true);

        // When
        boolean result = permissionService.canBindSpell(mockPlayer, spellKey);

        // Then
        assertTrue(result);
        verify(mockPlayer).hasPermission(expectedPermission);
    }

    @Test
    void testCannotBindSpell() {
        // Given
        PermissionService permissionService = new PermissionService();
        String spellKey = "magic-missile";
        String expectedPermission = "empirewand.spell.bind." + spellKey;
        when(mockPlayer.hasPermission(expectedPermission)).thenReturn(false);

        // When
        boolean result = permissionService.canBindSpell(mockPlayer, spellKey);

        // Then
        assertFalse(result);
        verify(mockPlayer).hasPermission(expectedPermission);
    }

    @Test
    void testGetSpellUsePermission() {
        // Given
        PermissionService permissionService = new PermissionService();
        String spellKey = "magic-missile";

        // When
        String result = permissionService.getSpellUsePermission(spellKey);

        // Then
        assertEquals("empirewand.spell.use.magic-missile", result);
    }

    @Test
    void testGetSpellBindPermission() {
        // Given
        PermissionService permissionService = new PermissionService();
        String spellKey = "magic-missile";

        // When
        String result = permissionService.getSpellBindPermission(spellKey);

        // Then
        assertEquals("empirewand.spell.bind.magic-missile", result);
    }

    @Test
    void testGetCommandPermission() {
        // Given
        PermissionService permissionService = new PermissionService();
        String command = "reload";

        // When
        String result = permissionService.getCommandPermission(command);

        // Then
        assertEquals("empirewand.command.reload", result);
    }

    @Test
    void testPermissionNodesWithSpecialCharacters() {
        // Given
        PermissionService permissionService = new PermissionService();
        String spellKey = "glacial-spike";

        // When
        String usePermission = permissionService.getSpellUsePermission(spellKey);
        String bindPermission = permissionService.getSpellBindPermission(spellKey);

        // Then
        assertEquals("empirewand.spell.use.glacial-spike", usePermission);
        assertEquals("empirewand.spell.bind.glacial-spike", bindPermission);
    }
}