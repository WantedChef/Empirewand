package nl.wantedchef.empirewand.spell;

/**
 * Spell cast result.
 */
public class CastResult {
    private final boolean success;
    private final String message;
    
    public CastResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public static CastResult success() {
        return new CastResult(true, "");
    }
    
    public static CastResult fail(String message) {
        return new CastResult(false, message);
    }
}