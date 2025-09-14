package nl.wantedchef.empirewand.gui.session;

import nl.wantedchef.empirewand.core.config.model.WandDifficulty;
import nl.wantedchef.empirewand.core.config.model.WandSettings;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test class for WandSessionManager.
 * Tests session creation, management, and cleanup functionality.
 */
@ExtendWith(MockitoExtension.class)
class WandSessionManagerTest {

    @Mock
    private Logger mockLogger;

    @Mock
    private Player mockPlayer;

    private WandSessionManager sessionManager;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        sessionManager = new WandSessionManager(mockLogger);
        playerId = UUID.randomUUID();

        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
    }

    @Test
    void testCreateSession() {
        // Test session creation
        WandSessionManager.WandSession session = sessionManager.getOrCreateSession(mockPlayer);

        assertNotNull(session);
        assertEquals(playerId, session.getPlayerId());
        assertEquals("TestPlayer", session.getPlayerName());
        assertTrue(session.getSessionId() > 0);
        assertEquals(1, sessionManager.getActiveSessionCount());
    }

    @Test
    void testGetExistingSession() {
        // Create initial session
        WandSessionManager.WandSession session1 = sessionManager.getOrCreateSession(mockPlayer);

        // Get session again - should return the same instance
        WandSessionManager.WandSession session2 = sessionManager.getOrCreateSession(mockPlayer);

        assertSame(session1, session2);
        assertEquals(1, sessionManager.getActiveSessionCount());
    }

    @Test
    void testSessionState() {
        WandSessionManager.WandSession session = sessionManager.getOrCreateSession(mockPlayer);

        // Test initial state
        assertNull(session.getCurrentWandKey());
        assertNull(session.getCurrentCategory());
        assertFalse(session.hasPendingChanges());
        assertEquals(0, session.getPendingChangesCount());

        // Set current wand
        session.setCurrentWandKey("test_wand");
        assertEquals("test_wand", session.getCurrentWandKey());

        // Set current category
        session.setCurrentCategory("difficulty");
        assertEquals("difficulty", session.getCurrentCategory());
    }

    @Test
    void testPendingChanges() {
        WandSessionManager.WandSession session = sessionManager.getOrCreateSession(mockPlayer);

        // Test adding pending changes
        WandSettings testSettings = WandSettings.builder("test_wand")
                .difficulty(WandDifficulty.HARD)
                .cooldownBlock(true)
                .griefBlockDamage(false)
                .playerDamage(true)
                .build();

        session.setPendingChanges("test_wand", testSettings);

        assertTrue(session.hasPendingChanges());
        assertTrue(session.hasPendingChanges("test_wand"));
        assertFalse(session.hasPendingChanges("other_wand"));
        assertEquals(1, session.getPendingChangesCount());

        WandSettings retrievedSettings = session.getPendingChanges("test_wand");
        assertNotNull(retrievedSettings);
        assertEquals(WandDifficulty.HARD, retrievedSettings.getDifficulty());
        assertTrue(retrievedSettings.isCooldownBlock());
        assertFalse(retrievedSettings.isGriefBlockDamage());
        assertTrue(retrievedSettings.isPlayerDamage());
    }

    @Test
    void testRemovePendingChanges() {
        WandSessionManager.WandSession session = sessionManager.getOrCreateSession(mockPlayer);

        // Add pending changes
        WandSettings testSettings = WandSettings.builder("test_wand")
                .difficulty(WandDifficulty.EASY)
                .build();
        session.setPendingChanges("test_wand", testSettings);

        assertTrue(session.hasPendingChanges());

        // Remove pending changes
        WandSettings removedSettings = session.removePendingChanges("test_wand");

        assertNotNull(removedSettings);
        assertEquals(testSettings, removedSettings);
        assertFalse(session.hasPendingChanges());
        assertFalse(session.hasPendingChanges("test_wand"));
        assertEquals(0, session.getPendingChangesCount());

        // Removing non-existent changes should return null
        assertNull(session.removePendingChanges("non_existent"));
    }

    @Test
    void testClearPendingChanges() {
        WandSessionManager.WandSession session = sessionManager.getOrCreateSession(mockPlayer);

        // Add multiple pending changes
        session.setPendingChanges("wand1", WandSettings.builder("wand1").build());
        session.setPendingChanges("wand2", WandSettings.builder("wand2").build());

        assertEquals(2, session.getPendingChangesCount());
        assertTrue(session.hasPendingChanges());

        // Clear all changes
        session.clearPendingChanges();

        assertEquals(0, session.getPendingChangesCount());
        assertFalse(session.hasPendingChanges());
    }

    @Test
    void testSessionTimestamps() {
        WandSessionManager.WandSession session = sessionManager.getOrCreateSession(mockPlayer);

        long createdTime = session.getCreatedTime();
        long lastActivity = session.getLastActivity();

        assertTrue(createdTime > 0);
        assertTrue(lastActivity >= createdTime);

        // Update activity
        long oldActivity = lastActivity;
        try {
            Thread.sleep(1); // Ensure time difference
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        session.updateActivity();
        assertTrue(session.getLastActivity() > oldActivity);

        // Test duration calculations
        assertTrue(session.getSessionDuration() >= 0);
        assertTrue(session.getTimeSinceLastActivity() >= 0);
    }

    @Test
    void testRemoveSession() {
        // Create session
        WandSessionManager.WandSession session = sessionManager.getOrCreateSession(mockPlayer);
        assertNotNull(session);
        assertEquals(1, sessionManager.getActiveSessionCount());

        // Remove session
        boolean removed = sessionManager.removeSession(mockPlayer);
        assertTrue(removed);
        assertEquals(0, sessionManager.getActiveSessionCount());

        // Removing non-existent session should return false
        boolean removedAgain = sessionManager.removeSession(mockPlayer);
        assertFalse(removedAgain);
    }

    @Test
    void testGetSessionByUuid() {
        // Create session
        WandSessionManager.WandSession session = sessionManager.getOrCreateSession(mockPlayer);

        // Get by UUID
        WandSessionManager.WandSession retrievedSession = sessionManager.getSession(playerId);
        assertSame(session, retrievedSession);

        // Get non-existent session
        UUID randomId = UUID.randomUUID();
        assertNull(sessionManager.getSession(randomId));
    }

    @Test
    void testGetSessionByPlayer() {
        // Test getting non-existent session
        assertNull(sessionManager.getSession(mockPlayer));

        // Create session
        WandSessionManager.WandSession session = sessionManager.getOrCreateSession(mockPlayer);

        // Get existing session
        WandSessionManager.WandSession retrievedSession = sessionManager.getSession(mockPlayer);
        assertSame(session, retrievedSession);
    }

    @Test
    void testClearAllSessions() {
        // Create multiple sessions
        Player mockPlayer2 = org.mockito.Mockito.mock(Player.class);
        UUID player2Id = UUID.randomUUID();
        when(mockPlayer2.getUniqueId()).thenReturn(player2Id);
        when(mockPlayer2.getName()).thenReturn("TestPlayer2");

        sessionManager.getOrCreateSession(mockPlayer);
        sessionManager.getOrCreateSession(mockPlayer2);

        assertEquals(2, sessionManager.getActiveSessionCount());

        // Clear all sessions
        sessionManager.clearAllSessions();

        assertEquals(0, sessionManager.getActiveSessionCount());
        assertNull(sessionManager.getSession(mockPlayer));
        assertNull(sessionManager.getSession(mockPlayer2));
    }

    @Test
    void testSessionCleanup() {
        // This would be more complex to test with actual TTL behavior
        // For now, we'll just test that cleanup doesn't break anything
        WandSessionManager.WandSession session = sessionManager.getOrCreateSession(mockPlayer);

        assertDoesNotThrow(() -> sessionManager.cleanupExpiredSessions());

        // Session should still exist (not expired)
        assertNotNull(sessionManager.getSession(mockPlayer));
    }

    @Test
    void testSessionToString() {
        WandSessionManager.WandSession session = sessionManager.getOrCreateSession(mockPlayer);
        session.setCurrentWandKey("test_wand");
        session.setPendingChanges("test_wand", WandSettings.builder("test_wand").build());

        String sessionString = session.toString();
        assertNotNull(sessionString);
        assertTrue(sessionString.contains("WandSession"));
        assertTrue(sessionString.contains("TestPlayer"));
        assertTrue(sessionString.contains("test_wand"));
    }

    @Test
    void testNullPointerHandling() {
        WandSessionManager.WandSession session = sessionManager.getOrCreateSession(mockPlayer);

        // Test null safety
        assertThrows(NullPointerException.class, () ->
                session.setPendingChanges(null, WandSettings.builder("test").build()));

        assertThrows(NullPointerException.class, () ->
                session.setPendingChanges("test", null));

        assertThrows(NullPointerException.class, () ->
                session.getPendingChanges(null));

        assertThrows(NullPointerException.class, () ->
                session.hasPendingChanges(null));

        assertThrows(NullPointerException.class, () ->
                sessionManager.getOrCreateSession(null));

        assertThrows(NullPointerException.class, () ->
                sessionManager.getSession((Player) null));

        assertThrows(NullPointerException.class, () ->
                sessionManager.getSession((UUID) null));
    }
}