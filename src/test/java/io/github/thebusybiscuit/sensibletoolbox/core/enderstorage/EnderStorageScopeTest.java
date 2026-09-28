package io.github.thebusybiscuit.sensibletoolbox.core.enderstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class EnderStorageScopeTest {

    private static final Map<String, String> MODALITIES = Map.of(
            "world", "slimefun",
            "bskyblock", "skyblock",
            "oneblock", "oneblock",
            "clasico", "clasico");

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void nullWorldKeepsTheLegacyNamespaceForApiCompatibility() {
        assertEquals("legacy", EnderStorageScope.forWorld(null, MODALITIES));
    }

    @Test
    void dimensionsAndSubworldsOfSameModalityShareScope() {
        World overworld = server.addSimpleWorld("world");
        World nether = server.addSimpleWorld("world_nether");
        World end = server.addSimpleWorld("world_the_end");
        World mars = server.addSimpleWorld("world_galactifun_mars");

        assertEquals("slimefun", EnderStorageScope.forWorld(overworld, MODALITIES));
        assertEquals("slimefun", EnderStorageScope.forWorld(nether, MODALITIES));
        assertEquals("slimefun", EnderStorageScope.forWorld(end, MODALITIES));
        assertEquals("slimefun", EnderStorageScope.forWorld(mars, MODALITIES));
    }

    @Test
    void differentModalitiesUseDifferentScopes() {
        World survival = server.addSimpleWorld("world");
        World skyblock = server.addSimpleWorld("bskyblock_world");

        assertEquals("skyblock", EnderStorageScope.forWorld(skyblock, MODALITIES));
        assertNotEquals(EnderStorageScope.forWorld(survival, MODALITIES), EnderStorageScope.forWorld(skyblock, MODALITIES));
    }

    @Test
    void fallsBackToBaseWorldWhenNoMappingConfigured() {
        World nether = server.addSimpleWorld("customworld_nether");
        assertEquals("customworld", EnderStorageScope.forWorld(nether, Map.of()));
    }

    @Test
    void delayedSaveIdentityKeepsSameFrequencyInDistinctScopes() {
        TestHolder slimefun = new TestHolder(new File("slimefun/global/7"), 7);
        TestHolder skyblock = new TestHolder(new File("skyblock/global/7"), 7);
        TestHolder duplicateSlimefun = new TestHolder(new File("slimefun/global/7"), 7);

        assertNotEquals(slimefun, skyblock);
        assertEquals(slimefun, duplicateSlimefun);

        Set<STBEnderStorageHolder> pendingSaves = new HashSet<>();
        pendingSaves.add(slimefun);
        pendingSaves.add(skyblock);

        assertEquals(2, pendingSaves.size());
    }

    private static final class TestHolder extends STBEnderStorageHolder {

        private final File saveFile;

        private TestHolder(File saveFile, int frequency) {
            super(null, frequency);
            this.saveFile = saveFile;
        }

        @Override
        public File getSaveFile() {
            return saveFile;
        }

        @Override
        public String getInventoryTitle() {
            return "test";
        }

        @Override
        public boolean isGlobal() {
            return true;
        }
    }
}
