package nl.wantedchef.empirewand.api;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a version with semantic versioning support.
 * Follows the MAJOR.MINOR.PATCH format with optional pre-release and build
 * metadata.
 *
 * @since 2.0.0
 */
public final class Version implements Comparable<Version> {

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;
    private final String buildMetadata;

    private Version(int major, int minor, int patch, String preRelease, String buildMetadata) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
        this.buildMetadata = buildMetadata;
    }

    /**
     * Creates a new version from major, minor, and patch numbers.
     *
     * @param major the major version number
     * @param minor the minor version number
     * @param patch the patch version number
     * @return a new Version instance
     */
    @NotNull
    public static Version of(int major, int minor, int patch) {
        return new Version(major, minor, patch, null, null);
    }

    /**
     * Creates a new version from major, minor, patch, and pre-release identifier.
     *
     * @param major      the major version number
     * @param minor      the minor version number
     * @param patch      the patch version number
     * @param preRelease the pre-release identifier (e.g., "alpha", "beta", "rc.1")
     * @return a new Version instance
     */
    @NotNull
    public static Version of(int major, int minor, int patch, @NotNull String preRelease) {
        return new Version(major, minor, patch, preRelease, null);
    }

    /**
     * Parses a version string into a Version object.
     * Supports formats like "1.0.0", "2.1.3-alpha", "1.0.0+build.1".
     *
     * @param versionString the version string to parse
     * @return a new Version instance
     * @throws IllegalArgumentException if the version string is invalid
     */
    @NotNull
    public static Version parse(@NotNull String versionString) {
        // Implementation for parsing version strings
        String[] parts = versionString.split("[+\\-]");
        String[] versionParts = parts[0].split("\\.");

        if (versionParts.length < 3) {
            throw new IllegalArgumentException("Invalid version format: " + versionString);
        }

        int major = Integer.parseInt(versionParts[0]);
        int minor = Integer.parseInt(versionParts[1]);
        int patch = Integer.parseInt(versionParts[2]);

        String preRelease = null;
        String buildMetadata = null;

        if (parts.length > 1) {
            if (versionString.contains("-") && versionString.contains("+")) {
                String[] metaParts = versionString.split("\\+");
                preRelease = metaParts[0].split("-")[1];
                buildMetadata = metaParts[1];
            } else if (versionString.contains("-")) {
                preRelease = versionString.split("-")[1];
            } else if (versionString.contains("+")) {
                buildMetadata = versionString.split("\\+")[1];
            }
        }

        return new Version(major, minor, patch, preRelease, buildMetadata);
    }

    /**
     * Gets the major version number.
     *
     * @return the major version
     */
    public int getMajor() {
        return major;
    }

    /**
     * Gets the minor version number.
     *
     * @return the minor version
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Gets the patch version number.
     *
     * @return the patch version
     */
    public int getPatch() {
        return patch;
    }

    /**
     * Gets the pre-release identifier.
     *
     * @return the pre-release identifier, or null if not present
     */
    @org.jetbrains.annotations.Nullable
    public String getPreRelease() {
        return preRelease;
    }

    /**
     * Gets the build metadata.
     *
     * @return the build metadata, or null if not present
     */
    @org.jetbrains.annotations.Nullable
    public String getBuildMetadata() {
        return buildMetadata;
    }

    /**
     * Checks if this version is a pre-release.
     *
     * @return true if this is a pre-release version
     */
    public boolean isPreRelease() {
        return preRelease != null;
    }

    /**
     * Converts this version to its string representation.
     *
     * @return the version as a string
     */
    @Override
    @NotNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(patch);

        if (preRelease != null) {
            sb.append('-').append(preRelease);
        }

        if (buildMetadata != null) {
            sb.append('+').append(buildMetadata);
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Version version = (Version) obj;
        return major == version.major &&
                minor == version.minor &&
                patch == version.patch &&
                java.util.Objects.equals(preRelease, version.preRelease) &&
                java.util.Objects.equals(buildMetadata, version.buildMetadata);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(major, minor, patch, preRelease, buildMetadata);
    }

    @Override
    public int compareTo(@NotNull Version other) {
        int result = Integer.compare(major, other.major);
        if (result != 0)
            return result;

        result = Integer.compare(minor, other.minor);
        if (result != 0)
            return result;

        result = Integer.compare(patch, other.patch);
        if (result != 0)
            return result;

        // Handle pre-release comparison
        if (preRelease == null && other.preRelease != null)
            return 1;
        if (preRelease != null && other.preRelease == null)
            return -1;
        if (preRelease != null && other.preRelease != null) {
            return preRelease.compareTo(other.preRelease);
        }

        return 0;
    }
}





