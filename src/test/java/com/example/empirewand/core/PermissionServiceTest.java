package com.example.empirewand.core;

import com.example.empirewand.EmpireWandTestBase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        PermissionServiceImpl permissionService = new PermissionServiceImpl();
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
        PermissionServiceImpl permissionService = new PermissionServiceImpl();
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
        PermissionServiceImpl permissionService = new PermissionServiceImpl();
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
        PermissionServiceImpl permissionService = new PermissionServiceImpl();
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
        PermissionServiceImpl permissionService = new PermissionServiceImpl();
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
        PermissionServiceImpl permissionService = new PermissionServiceImpl();
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
        PermissionServiceImpl permissionService = new PermissionServiceImpl();
        String spellKey = "magic-missile";

        // When
        String result = permissionService.getSpellUsePermission(spellKey);

        // Then
        assertEquals("empirewand.spell.use.magic-missile", result);
    }

    @Test
    void testGetSpellBindPermission() {
        // Given
        PermissionServiceImpl permissionService = new PermissionServiceImpl();
        String spellKey = "magic-missile";

        // When
        String result = permissionService.getSpellBindPermission(spellKey);

        // Then
        assertEquals("empirewand.spell.bind.magic-missile", result);
    }

    @Test
    void testGetCommandPermission() {
        // Given
        PermissionServiceImpl permissionService = new PermissionServiceImpl();
        String command = "reload";

        // When
        String result = permissionService.getCommandPermission(command);

        // Then
        assertEquals("empirewand.command.reload", result);
    }

    @Test
    void testPermissionNodesWithSpecialCharacters() {
        // Given
        PermissionServiceImpl permissionService = new PermissionServiceImpl();
        String spellKey = "glacial-spike";

        // When
        String usePermission = permissionService.getSpellUsePermission(spellKey);
        String bindPermission = permissionService.getSpellBindPermission(spellKey);

        // Then
        assertEquals("empirewand.spell.use.glacial-spike", usePermission);
        assertEquals("empirewand.spell.bind.glacial-spike", bindPermission);
    }
}
