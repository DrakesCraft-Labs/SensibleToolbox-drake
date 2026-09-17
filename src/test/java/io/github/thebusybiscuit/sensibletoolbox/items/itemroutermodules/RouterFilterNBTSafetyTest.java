package io.github.thebusybiscuit.sensibletoolbox.items.itemroutermodules;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import io.github.thebusybiscuit.sensibletoolbox.api.filters.FilterType;
import io.github.thebusybiscuit.sensibletoolbox.core.STBItemRegistry;
import com.github.drakescraft_labs.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;

class RouterFilterNBTSafetyTest {

    private ServerMock server;
    private org.mockbukkit.mockbukkit.plugin.PluginMock pluginMock;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        pluginMock = MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testSanitizeFilterItemMaterialMode() {
        ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = diamondSword.getItemMeta();
        assertNotNull(meta);
        meta.setDisplayName("Massive Blade");
        meta.setLore(List.of("Very long lore line 1", "Very long lore line 2"));
        diamondSword.setItemMeta(meta);

        ItemStack sanitized = DirectionalItemRouterModule.sanitizeFilterItem(diamondSword, FilterType.MATERIAL);
        assertNotNull(sanitized);
        assertEquals(Material.DIAMOND_SWORD, sanitized.getType());
        assertTrue(!sanitized.hasItemMeta() || sanitized.getItemMeta().getDisplayName().isEmpty());
    }

    @Test
    void testSanitizeFilterItemStripsContainerInventory() {
        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        ItemMeta meta = shulker.getItemMeta();
        if (meta instanceof BlockStateMeta bsm) {
            ItemStack sanitized = DirectionalItemRouterModule.sanitizeFilterItem(shulker, FilterType.ITEM_META);
            assertNotNull(sanitized);
            assertEquals(Material.SHULKER_BOX, sanitized.getType());
        }
    }

    @Test
    void testFreezeSizeSafetyUnderLimits() {
        SenderModule module = new SenderModule();
        module.getFilter().setFilterType(FilterType.MATERIAL);

        // Add 9 items
        for (int i = 0; i < 9; i++) {
            ItemStack item = new ItemStack(Material.STONE);
            module.getFilter().addItem(item);
        }

        YamlConfiguration conf = module.freeze();
        String saved = conf.saveToString();
        int byteLen = saved.getBytes(StandardCharsets.UTF_8).length;
        assertTrue(byteLen < 32000, "Serialized filter should be well under 32KB, was: " + byteLen);
    }

    @Test
    void testSanitizeOversizedPDC() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);

        // Create an oversized string simulating > 50KB data
        StringBuilder sb = new StringBuilder();
        sb.append("'*TYPE': itemrouter_sender\n");
        sb.append("direction: NORTH\n");
        sb.append("filtered: \n");
        for (int i = 0; i < 2000; i++) {
            sb.append("  - item_").append(i).append(": 'very_long_nested_attribute_data_bloating_nbt'\n");
        }
        String oversized = sb.toString();
        assertTrue(oversized.getBytes(StandardCharsets.UTF_8).length > 50000);

        STBItemRegistry registry = new STBItemRegistry(pluginMock, "item_data");
        PersistentDataAPI.setString(meta, registry.getKey(), oversized);
        item.setItemMeta(meta);

        registry.sanitizeOversizedPDC(item);

        ItemMeta cleanedMeta = item.getItemMeta();
        assertNotNull(cleanedMeta);
        String cleanedPDC = PersistentDataAPI.getString(cleanedMeta, registry.getKey());
        assertNotNull(cleanedPDC);
        assertTrue(cleanedPDC.getBytes(StandardCharsets.UTF_8).length < 5000, "Cleaned PDC must be small, was: " + cleanedPDC.length());
        assertTrue(cleanedPDC.contains("itemrouter_sender"));
    }
}
