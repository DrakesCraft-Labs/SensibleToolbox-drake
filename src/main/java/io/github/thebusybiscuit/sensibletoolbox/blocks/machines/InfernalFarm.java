package io.github.thebusybiscuit.sensibletoolbox.blocks.machines;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import io.github.thebusybiscuit.sensibletoolbox.items.GoldCombineHoe;
import io.github.thebusybiscuit.sensibletoolbox.items.components.MachineFrame;

public class InfernalFarm extends AutoFarm {

    private static final int RADIUS = 11;

    public InfernalFarm() {
        super();
    }

    public InfernalFarm(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public Material getMaterial() {
        return Material.NETHER_BRICKS;
    }

    @Override
    public String getItemName() {
        return "Infernal Farm";
    }

    @Override
    public String[] getLore() {
        return new String[] { "Automatically harvests and replants", "Nether Warts", "in an 11x11 Area (5 Block Radius) 2 Blocks above the Machine" };
    }

    @Override
    public Recipe getMainRecipe() {
        MachineFrame frame = new MachineFrame();
        GoldCombineHoe hoe = new GoldCombineHoe();
        registerCustomIngredients(frame, hoe);
        ShapedRecipe res = new ShapedRecipe(getKey(), toItemStack());
        res.shape("NHN", "IFI", "RGR");
        res.setIngredient('R', Material.REDSTONE);
        res.setIngredient('G', Material.GOLD_INGOT);
        res.setIngredient('I', Material.IRON_INGOT);
        res.setIngredient('H', hoe.getMaterial());
        res.setIngredient('F', frame.getMaterial());
        res.setIngredient('N', Material.NETHER_BRICK);
        return res;
    }

    @Override
    protected int getRadius() {
        return RADIUS;
    }

    @Override
    protected void harvestCrops() {
        if (getCharge() < getScuPerCycle()) {
            return;
        }

        for (Block crop : blocks) {
            if (crop.getType() == Material.NETHER_WART) {
                Ageable ageable = (Ageable) crop.getBlockData();

                if (ageable.getAge() >= ageable.getMaximumAge()) {
                    setCharge(getCharge() - getScuPerCycle());

                    ageable.setAge(0);
                    crop.setBlockData(ageable);
                    crop.getWorld().playEffect(crop.getLocation(), Effect.STEP_SOUND, crop.getBlockData());
                    if (!output(Material.NETHER_WART)) {
                        buffer = Material.NETHER_WART;
                        setJammed(true);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public double getScuPerCycle() {
        return 50.0;
    }
}
