package nl.wantedchef.empirewand.gui.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import nl.wantedchef.empirewand.core.config.model.WandSettings;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Manages per-player GUI sessions for wand settings editing.
 * Handles session creation, tracking, cleanup, and pending changes.
 */
public class WandSessionManager {

    private static final Duration SESSION_TTL = Duration.ofMinutes(10);
    private static final int MAX_SESSIONS = 100;

    private final Logger logger;
    private final Cache<UUID, WandSession> sessions;
    private final AtomicLong sessionIdGenerator = new AtomicLong(0);

    public WandSessionManager(@NotNull Logger logger) {
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");

        // Initialize session cache with TTL and removal listener
        this.sessions = Caffeine.newBuilder()
                .maximumSize(MAX_SESSIONS)
                .expireAfterAccess(SESSION_TTL)
                .removalListener(this::onSessionRemoved)
                .build();
    }

    /**
     * Creates or gets an existing session for a player.
     *
     * @param player The player to create/get session for
     * @return The player's session
     */
    @NotNull
    public WandSession getOrCreateSession(@NotNull Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        UUID playerId = player.getUniqueId();
        WandSession session = sessions.getIfPresent(playerId);

        if (session == null) {
            long sessionId = sessionIdGenerator.incrementAndGet();
            session = new WandSession(sessionId, playerId, player.getName());
            sessions.put(playerId, session);

            logger.info("Created new GUI session for player: " + player.getName() + " (ID: " + sessionId + ")");
        }

        return session;
    }

    /**
     * Gets an existing session for a player.
     *
     * @param player The player to get session for
     * @return The player's session, or null if none exists
     */
    @Nullable
    public WandSession getSession(@NotNull Player player) {
        Objects.requireNonNull(player, "Player cannot be null");
        return sessions.getIfPresent(player.getUniqueId());
    }

    /**
     * Gets an existing session by UUID.
     *
     * @param playerId The player's UUID
     * @return The player's session, or null if none exists
     */
    @Nullable
    public WandSession getSession(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        return sessions.getIfPresent(playerId);
    }

    /**
     * Removes a session for a player.
     *
     * @param player The player to remove session for
     * @return True if a session was removed
     */
    public boolean removeSession(@NotNull Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        WandSession session = sessions.getIfPresent(player.getUniqueId());
        if (session != null) {
            sessions.invalidate(player.getUniqueId());
            logger.info("Manually removed GUI session for player: " + player.getName());
            return true;
        }

        return false;
    }

    /**
     * Gets the current number of active sessions.
     *
     * @return The number of active sessions
     */
    public long getActiveSessionCount() {
        return sessions.estimatedSize();
    }

    /**
     * Cleans up all expired sessions.
     * This is called automatically by the cache, but can be called manually.
     */
    public void cleanupExpiredSessions() {
        sessions.cleanUp();
        logger.fine("Cleaned up expired GUI sessions");
    }

    /**
     * Clears all sessions. Used for plugin shutdown or emergency cleanup.
     */
    public void clearAllSessions() {
        long count = sessions.estimatedSize();
        sessions.invalidateAll();
        logger.info("Cleared all GUI sessions (" + count + " sessions removed)");
    }

    private void onSessionRemoved(@Nullable UUID playerId, @Nullable WandSession session, @NotNull RemovalCause cause) {
        if (session != null && playerId != null) {
            String playerName = session.getPlayerName();

            // Log pending changes if any
            if (session.hasPendingChanges()) {
                logger.warning("Session removed with pending changes for player: " + playerName +
                             " (Cause: " + cause + ")");
            } else {
                logger.fine("Session removed for player: " + playerName + " (Cause: " + cause + ")");
            }

            // Clean up any resources if needed
            session.cleanup();
        }
    }

    /**
     * Represents a GUI session for a player editing wand settings.
     */
    public static class WandSession {
        private final long sessionId;
        private final UUID playerId;
        private final String playerName;
        private final long createdTime;

        // Session state
        private final ConcurrentHashMap<String, WandSettings> pendingChanges = new ConcurrentHashMap<>();
        private volatile String currentWandKey;
        private volatile String currentCategory;
        private volatile long lastActivity;

        private WandSession(long sessionId, @NotNull UUID playerId, @NotNull String playerName) {
            this.sessionId = sessionId;
            this.playerId = playerId;
            this.playerName = playerName;
            this.createdTime = System.currentTimeMillis();
            this.lastActivity = createdTime;
        }

        /**
         * Gets the unique session ID.
         *
         * @return The session ID
         */
        public long getSessionId() {
            return sessionId;
        }

        /**
         * Gets the player's UUID.
         *
         * @return The player's UUID
         */
        @NotNull
        public UUID getPlayerId() {
            return playerId;
        }

        /**
         * Gets the player's name.
         *
         * @return The player's name
         */
        @NotNull
        public String getPlayerName() {
            return playerName;
        }

        /**
         * Gets when this session was created.
         *
         * @return Creation timestamp in milliseconds
         */
        public long getCreatedTime() {
            return createdTime;
        }

        /**
         * Gets the last activity timestamp.
         *
         * @return Last activity timestamp in milliseconds
         */
        public long getLastActivity() {
            return lastActivity;
        }

        /**
         * Updates the last activity timestamp.
         */
        public void updateActivity() {
            this.lastActivity = System.currentTimeMillis();
        }

        /**
         * Gets the currently selected wand key.
         *
         * @return The current wand key, or null if none selected
         */
        @Nullable
        public String getCurrentWandKey() {
            return currentWandKey;
        }

        /**
         * Sets the currently selected wand key.
         *
         * @param wandKey The wand key to select
         */
        public void setCurrentWandKey(@Nullable String wandKey) {
            this.currentWandKey = wandKey;
            updateActivity();
        }

        /**
         * Gets the currently selected category.
         *
         * @return The current category, or null if none selected
         */
        @Nullable
        public String getCurrentCategory() {
            return currentCategory;
        }

        /**
         * Sets the currently selected category.
         *
         * @param category The category to select
         */
        public void setCurrentCategory(@Nullable String category) {
            this.currentCategory = category;
            updateActivity();
        }

        /**
         * Adds pending changes for a wand.
         *
         * @param wandKey The wand key
         * @param settings The new settings
         */
        public void setPendingChanges(@NotNull String wandKey, @NotNull WandSettings settings) {
            Objects.requireNonNull(wandKey, "Wand key cannot be null");
            Objects.requireNonNull(settings, "Settings cannot be null");

            pendingChanges.put(wandKey, settings);
            updateActivity();
        }

        /**
         * Gets pending changes for a wand.
         *
         * @param wandKey The wand key
         * @return The pending settings, or null if none
         */
        @Nullable
        public WandSettings getPendingChanges(@NotNull String wandKey) {
            Objects.requireNonNull(wandKey, "Wand key cannot be null");
            return pendingChanges.get(wandKey);
        }

        /**
         * Removes pending changes for a wand.
         *
         * @param wandKey The wand key
         * @return The removed settings, or null if none existed
         */
        @Nullable
        public WandSettings removePendingChanges(@NotNull String wandKey) {
            Objects.requireNonNull(wandKey, "Wand key cannot be null");
            updateActivity();
            return pendingChanges.remove(wandKey);
        }

        /**
         * Checks if there are any pending changes.
         *
         * @return True if there are pending changes
         */
        public boolean hasPendingChanges() {
            return !pendingChanges.isEmpty();
        }

        /**
         * Checks if there are pending changes for a specific wand.
         *
         * @param wandKey The wand key to check
         * @return True if there are pending changes for this wand
         */
        public boolean hasPendingChanges(@NotNull String wandKey) {
            Objects.requireNonNull(wandKey, "Wand key cannot be null");
            return pendingChanges.containsKey(wandKey);
        }

        /**
         * Gets the number of pending changes.
         *
         * @return The number of wands with pending changes
         */
        public int getPendingChangesCount() {
            return pendingChanges.size();
        }

        /**
         * Clears all pending changes.
         */
        public void clearPendingChanges() {
            pendingChanges.clear();
            updateActivity();
        }

        /**
         * Gets the session duration in milliseconds.
         *
         * @return The session duration
         */
        public long getSessionDuration() {
            return lastActivity - createdTime;
        }

        /**
         * Gets the time since last activity in milliseconds.
         *
         * @return Time since last activity
         */
        public long getTimeSinceLastActivity() {
            return System.currentTimeMillis() - lastActivity;
        }

        /**
         * Cleans up session resources. Called when session is removed.
         */
        void cleanup() {
            pendingChanges.clear();
            currentWandKey = null;
            currentCategory = null;
        }

        @Override
        public String toString() {
            return "WandSession{" +
                    "sessionId=" + sessionId +
                    ", playerId=" + playerId +
                    ", playerName='" + playerName + '\'' +
                    ", currentWandKey='" + currentWandKey + '\'' +
                    ", pendingChanges=" + pendingChanges.size() +
                    ", duration=" + getSessionDuration() + "ms" +
                    '}';
        }
    }
}