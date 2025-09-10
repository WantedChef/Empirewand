package nl.wantedchef.empirewand.spell;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of a spell cast operation.
 */
public class CastResult {
    private final boolean success;
    private final String message;
    
    private CastResult(boolean success, @Nullable String message) {
        this.success = success;
        this.message = message;
    }
    
    public static CastResult success() {
        return new CastResult(true, null);
    }
    
    public static CastResult fail(@NotNull String message) {
        return new CastResult(false, message);
    }
    
    public boolean isSuccess() { return success; }
    
    @Nullable
    public String getMessage() { return message; }
}
