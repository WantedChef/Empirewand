package nl.wantedchef.empirewand.api;

import org.jetbrains.annotations.NotNull;

public final class Version implements Comparable<Version> {
    private final int major, minor, patch;
    
    private Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }
    
    @NotNull
    public static Version of(int major, int minor, int patch) {
        return new Version(major, minor, patch);
    }
    
    public int getMajor() { return major; }
    public int getMinor() { return minor; }
    public int getPatch() { return patch; }
    
    @Override
    public int compareTo(@NotNull Version other) {
        int result = Integer.compare(this.major, other.major);
        if (result != 0) return result;
        result = Integer.compare(this.minor, other.minor);
        if (result != 0) return result;
        return Integer.compare(this.patch, other.patch);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Version)) return false;
        Version other = (Version) obj;
        return major == other.major && minor == other.minor && patch == other.patch;
    }
    
    @Override
    public int hashCode() {
        return (major << 16) | (minor << 8) | patch;
    }
    
    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
