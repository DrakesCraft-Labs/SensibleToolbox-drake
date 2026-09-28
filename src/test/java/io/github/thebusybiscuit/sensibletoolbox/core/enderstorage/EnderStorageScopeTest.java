package io.github.thebusybiscuit.sensibletoolbox.core.enderstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class EnderStorageScopeTest {

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
        assertEquals("legacy", EnderStorageScope.forWorld(null));
    }

    @Test
    void distinctWorldsAlwaysUseDistinctStorageNamespaces() {
        World first = server.addSimpleWorld("first");
        World second = server.addSimpleWorld("second");

        assertNotEquals(EnderStorageScope.forWorld(first), EnderStorageScope.forWorld(second));
    }
}
