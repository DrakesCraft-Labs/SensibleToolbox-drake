package io.github.thebusybiscuit.sensibletoolbox.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for server version detection. The old detection searched for "1.(\d+)"
 * anywhere in the version string, so "26.1.2" was read as Minecraft 1.2 and the plugin
 * disabled itself on every 26.x server.
 */
class MinecraftVersionTest {

    @ParameterizedTest
    @CsvSource({
        "1.21.11-R0.1-SNAPSHOT, MINECRAFT_1_21",
        "1.21-R0.1-SNAPSHOT, MINECRAFT_1_21",
        "1.20.6-R0.1-SNAPSHOT, MINECRAFT_1_20",
        "26.1-R0.1-SNAPSHOT, MINECRAFT_26_1",
        "26.1.2-R0.1-SNAPSHOT, MINECRAFT_26_1",
        "26.1.2.build.74-stable, MINECRAFT_26_1",
        "26.2-R0.1-SNAPSHOT, MINECRAFT_26_2",
        "26.2.build.129-stable, MINECRAFT_26_2",
        "' 26.2.1', MINECRAFT_26_2"
    })
    void detectsSupportedServerVersions(String bukkitVersion, MinecraftVersion expected) {
        assertEquals(expected, MinecraftVersion.fromBukkitVersion(bukkitVersion));
    }

    @ParameterizedTest
    @ValueSource(strings = { "26.3-R0.1-SNAPSHOT", "27.1", "1.13.2-R0.1-SNAPSHOT", "1.22", "2.1" })
    void rejectsParsableButUnsupportedVersions(String bukkitVersion) {
        assertNull(MinecraftVersion.fromBukkitVersion(bukkitVersion));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "unknown", "git-Paper-123", "R0.1-SNAPSHOT" })
    void unparsableVersionsAreUnknown(String bukkitVersion) {
        assertEquals(MinecraftVersion.UNKNOWN, MinecraftVersion.fromBukkitVersion(bukkitVersion));
    }

    @Test
    void nullVersionIsUnknown() {
        assertEquals(MinecraftVersion.UNKNOWN, MinecraftVersion.fromBukkitVersion(null));
    }

    @Test
    void yearBasedVersionsAreNewerThanLegacyVersions() {
        assertTrue(MinecraftVersion.MINECRAFT_26_1.isAtLeast(MinecraftVersion.MINECRAFT_1_21));
        assertTrue(MinecraftVersion.MINECRAFT_26_2.isAtLeast(MinecraftVersion.MINECRAFT_26_1));
        assertTrue(MinecraftVersion.MINECRAFT_26_1.isAtLeast(MinecraftVersion.MINECRAFT_1_16));
        assertTrue(MinecraftVersion.MINECRAFT_1_21.isBefore(MinecraftVersion.MINECRAFT_26_1));
        assertFalse(MinecraftVersion.MINECRAFT_26_1.isBefore(MinecraftVersion.MINECRAFT_1_21));
        assertFalse(MinecraftVersion.UNKNOWN.isAtLeast(MinecraftVersion.MINECRAFT_1_14));
    }

    @Test
    void legacyMatcherOnlyMatchesLegacyReleases() {
        assertTrue(MinecraftVersion.MINECRAFT_1_21.isMinecraftVersion(21));
        assertFalse(MinecraftVersion.MINECRAFT_26_1.isMinecraftVersion(1));
        assertTrue(MinecraftVersion.MINECRAFT_26_1.isMinecraftVersion(26, 1));
        assertFalse(MinecraftVersion.UNIT_TEST.isMinecraftVersion(0, 0));
    }

    @Test
    void displayNamesListTheNewVersions() {
        assertEquals("26.1.x", MinecraftVersion.MINECRAFT_26_1.getName());
        assertEquals("26.2.x", MinecraftVersion.MINECRAFT_26_2.getName());
    }
}
