package io.github.thebusybiscuit.sensibletoolbox.blocks.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import io.github.thebusybiscuit.sensibletoolbox.SensibleToolboxPlugin;
import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBBlock;
import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBMachine;
import me.desht.dhutils.blocks.PersistableLocation;

class AutoFarmTest {

    private ServerMock server;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SensibleToolboxPlugin.class);
        world = server.addSimpleWorld("test_world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static void setLocationReflectively(BaseSTBBlock block, Location loc) {
        try {
            Field field = BaseSTBBlock.class.getDeclaredField("persistableLocation");
            field.setAccessible(true);
            field.set(block, new PersistableLocation(loc));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ItemStack getInventoryItemReflectively(BaseSTBMachine machine, int slot) {
        try {
            Method method = BaseSTBMachine.class.getDeclaredMethod("getInventoryItem", int.class);
            method.setAccessible(true);
            return (ItemStack) method.invoke(machine, slot);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testAutoFarmHarvestsWheatAndResetsAge() {
        Location machineLoc = new Location(world, 10, 64, 10);
        AutoFarm farm = new AutoFarm();
        setLocationReflectively(farm, machineLoc);
        farm.setCharge(100.0);

        Block cropBlock = world.getBlockAt(10, 65, 10);
        cropBlock.setType(Material.WHEAT);
        Ageable ageable = (Ageable) cropBlock.getBlockData();
        ageable.setAge(ageable.getMaximumAge());
        cropBlock.setBlockData(ageable);

        farm.onBlockRegistered(machineLoc, true);
        farm.onServerTick();

        Ageable harvestedAge = (Ageable) cropBlock.getBlockData();
        assertEquals(0, harvestedAge.getAge(), "Wheat age should be reset to 0 after harvest");
        assertEquals(75.0, farm.getCharge(), 0.01, "Charge should be deducted by 25.0 SCU");

        boolean foundWheat = false;
        for (int slot : farm.getOutputSlots()) {
            ItemStack item = getInventoryItemReflectively(farm, slot);
            if (item != null && item.getType() == Material.WHEAT && item.getAmount() > 0) {
                foundWheat = true;
                break;
            }
        }
        assertTrue(foundWheat, "AutoFarm output inventory should contain harvested wheat");
    }

    @Test
    void testAutoFarm2HarvestsSugarCaneTopBlock() {
        Location machineLoc = new Location(world, 20, 64, 20);
        AutoFarm2 farm2 = new AutoFarm2();
        setLocationReflectively(farm2, machineLoc);
        farm2.setCharge(100.0);

        Block baseCane = world.getBlockAt(20, 65, 20);
        baseCane.setType(Material.SUGAR_CANE);

        Block topCane = world.getBlockAt(20, 66, 20);
        topCane.setType(Material.SUGAR_CANE);

        farm2.onBlockRegistered(machineLoc, true);
        farm2.onServerTick();

        assertEquals(Material.SUGAR_CANE, baseCane.getType(), "Base sugar cane must not be broken");
        assertEquals(Material.AIR, topCane.getType(), "Top sugar cane should be harvested and set to AIR");
        assertEquals(70.0, farm2.getCharge(), 0.01, "Charge should be deducted by 30.0 SCU");

        boolean foundCane = false;
        for (int slot : farm2.getOutputSlots()) {
            ItemStack item = getInventoryItemReflectively(farm2, slot);
            if (item != null && item.getType() == Material.SUGAR_CANE && item.getAmount() > 0) {
                foundCane = true;
                break;
            }
        }
        assertTrue(foundCane, "AutoFarm2 output inventory should contain harvested sugar cane");
    }

    @Test
    void testInfernalFarmHarvestsNetherWart() {
        Location machineLoc = new Location(world, 30, 64, 30);
        InfernalFarm farm = new InfernalFarm();
        setLocationReflectively(farm, machineLoc);
        farm.setCharge(100.0);

        Block wartBlock = world.getBlockAt(30, 65, 30);
        wartBlock.setType(Material.NETHER_WART);
        Ageable ageable = (Ageable) wartBlock.getBlockData();
        ageable.setAge(ageable.getMaximumAge());
        wartBlock.setBlockData(ageable);

        farm.onBlockRegistered(machineLoc, true);
        farm.onServerTick();

        Ageable harvestedAge = (Ageable) wartBlock.getBlockData();
        assertEquals(0, harvestedAge.getAge(), "Nether wart age should be reset to 0 after harvest");
        assertEquals(50.0, farm.getCharge(), 0.01, "Charge should be deducted by 50.0 SCU");

        boolean foundWart = false;
        for (int slot : farm.getOutputSlots()) {
            ItemStack item = getInventoryItemReflectively(farm, slot);
            if (item != null && item.getType() == Material.NETHER_WART && item.getAmount() > 0) {
                foundWart = true;
                break;
            }
        }
        assertTrue(foundWart, "InfernalFarm output inventory should contain harvested nether wart");
    }
}
