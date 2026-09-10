package io.github.thebusybiscuit.sensibletoolbox.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class FurnaceRecipeTest {

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
    void testExactChoiceFurnaceRecipeMatchesCustomItemAndRejectsVanilla() {
        ItemStack energizedDust = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta meta = energizedDust.getItemMeta();
        assertNotNull(meta);
        meta.setDisplayName("§eEnergized Gold Dust");
        energizedDust.setItemMeta(meta);

        ItemStack energizedIngot = new ItemStack(Material.GOLD_INGOT);
        ItemMeta ingotMeta = energizedIngot.getItemMeta();
        assertNotNull(ingotMeta);
        ingotMeta.setDisplayName("§eEnergized Gold Ingot");
        energizedIngot.setItemMeta(ingotMeta);

        NamespacedKey key = new NamespacedKey("sensibletoolbox", "energizedgolddust_furnacerecipe");
        RecipeChoice.ExactChoice choice = new RecipeChoice.ExactChoice(energizedDust);
        FurnaceRecipe recipe = new FurnaceRecipe(key, energizedIngot, choice, 0.0f, 200);

        server.addRecipe(recipe);

        // Verify the recipe choice
        assertTrue(recipe.getInputChoice() instanceof RecipeChoice.ExactChoice);
        RecipeChoice.ExactChoice registeredChoice = (RecipeChoice.ExactChoice) recipe.getInputChoice();

        // Must match the custom item with meta
        assertTrue(registeredChoice.test(energizedDust));

        // Must match a stacked item of the same custom type
        ItemStack stackedCustom = energizedDust.clone();
        stackedCustom.setAmount(16);
        assertTrue(registeredChoice.test(stackedCustom));

        // Must reject raw vanilla glowstone dust without meta
        ItemStack vanillaDust = new ItemStack(Material.GLOWSTONE_DUST, 1);
        assertFalse(registeredChoice.test(vanillaDust));

        // Output matches energized ingot
        assertEquals(energizedIngot, recipe.getResult());
    }

    @Test
    void testGoldDustAndEnergizedGoldDustDoNotConflict() {
        ItemStack goldDust = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta goldMeta = goldDust.getItemMeta();
        assertNotNull(goldMeta);
        goldMeta.setDisplayName("§eGold Dust");
        goldDust.setItemMeta(goldMeta);

        ItemStack energizedDust = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta energizedMeta = energizedDust.getItemMeta();
        assertNotNull(energizedMeta);
        energizedMeta.setDisplayName("§eEnergized Gold Dust");
        energizedDust.setItemMeta(energizedMeta);

        RecipeChoice.ExactChoice goldChoice = new RecipeChoice.ExactChoice(goldDust);
        RecipeChoice.ExactChoice energizedChoice = new RecipeChoice.ExactChoice(energizedDust);

        // Gold dust choice must test true for gold dust, false for energized dust
        assertTrue(goldChoice.test(goldDust));
        assertFalse(goldChoice.test(energizedDust));

        // Energized dust choice must test true for energized dust, false for gold dust
        assertTrue(energizedChoice.test(energizedDust));
        assertFalse(energizedChoice.test(goldDust));
    }
}
