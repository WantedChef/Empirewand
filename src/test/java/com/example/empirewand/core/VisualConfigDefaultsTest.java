package com.example.empirewand.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a representative subset of visual config keys have sane default
 * fallbacks
 * and that absence of keys does not cause NPE risks when plugins request them
 * with defaults.
 * Instead of relying on the real ConfigService (already smoke-tested), this
 * test
 * directly exercises Bukkit's YamlConfiguration default getters mirroring usage
 * patterns.
 */
class VisualConfigDefaultsTest {

    private static final List<String> DOUBLE_KEYS = List.of(
            "glacial-spike.visuals.trail_speed",
            "chain-lightning.visuals.arc_jitter",
            "arcane-orb.visuals.halo_radius");

    private static final List<String> INT_KEYS = List.of(
            "magic-missile.visuals.particle_count",
            "comet.visuals.trail_length",
            "empire-comet.visuals.trail_length");

    private static final List<String> BOOL_KEYS = List.of(
            "explosion-trail.flags.enable_reversible_blocks",
            "arcane-orb.flags.enable_reversible_blocks");

    @Test
    @DisplayName("Missing visual keys return supplied defaults from YamlConfiguration")
    void missingKeysReturnProvidedDefaults() throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        // Intentionally do not add any of the probed keys
        yaml.loadFromString("# empty visual config subset for test\n");

        // Probe doubles
        DOUBLE_KEYS.forEach(k -> assertEquals(0.5d, yaml.getDouble(k, 0.5d), "Double default mismatch for " + k));
        // Probe ints
        INT_KEYS.forEach(k -> assertEquals(10, yaml.getInt(k, 10), "Int default mismatch for " + k));
        // Probe booleans
        BOOL_KEYS.forEach(
                k -> assertFalse(yaml.getBoolean(k, false), "Boolean default should be false for missing key " + k));
    }

    @Test
    @DisplayName("Present visual keys override defaults")
    void presentKeysOverrideDefaults() throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        StringBuilder sb = new StringBuilder();
        sb.append("magic-missile:\n  visuals:\n    particle_count: 77\n");
        sb.append("arcane-orb:\n  visuals:\n    halo_radius: 2.75\n");
        sb.append("explosion-trail:\n  flags:\n    enable_reversible_blocks: true\n");
        yaml.loadFromString(sb.toString());

        assertEquals(77, yaml.getInt("magic-missile.visuals.particle_count", 10));
        assertEquals(2.75d, yaml.getDouble("arcane-orb.visuals.halo_radius", 0.5d));
        assertTrue(yaml.getBoolean("explosion-trail.flags.enable_reversible_blocks", false));
    }

    @Test
    @DisplayName("No unexpected NPE pathways for chained retrieval of many keys")
    void bulkRetrievalDoesNotThrow() throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("# still empty\n");

        assertDoesNotThrow(() -> {
            Stream.concat(Stream.concat(DOUBLE_KEYS.stream(), INT_KEYS.stream()), BOOL_KEYS.stream())
                    .forEach(k -> {
                        // Simulate mixed retrieval styles
                        yaml.getDouble(k, 1.0d);
                        yaml.getInt(k, 5);
                        yaml.getBoolean(k, false);
                    });
        });
    }
}
