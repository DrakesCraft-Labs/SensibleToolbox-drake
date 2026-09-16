package io.github.thebusybiscuit.sensibletoolbox.blocks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class SignLabelSafetyTest {

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
    void testSignSetLineAcceptsEmptyStringInsteadOfNull() {
        World world = server.addSimpleWorld("test_world");
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.OAK_WALL_SIGN);
        Sign sign = (Sign) block.getState();

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 4; i++) {
                sign.setLine(i, "");
            }
            sign.setLine(1, "Safe Line");
            sign.update();
        });

        assertEquals("Safe Line", sign.getLine(1));
    }
}
