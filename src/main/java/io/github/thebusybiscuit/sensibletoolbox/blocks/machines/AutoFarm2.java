package io.github.thebusybiscuit.sensibletoolbox.blocks.machines;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.inventory.ShapedRecipe;

import io.github.thebusybiscuit.sensibletoolbox.items.GoldCombineHoe;
import io.github.thebusybiscuit.sensibletoolbox.items.components.MachineFrame;

public class AutoFarm2 extends AutoFarm {

    private static final Map<Material, Material> crops = new EnumMap<>(Material.class);
    private static final int RADIUS = 5;

    static {
        crops.put(Material.COCOA, Material.COCOA_BEANS);
        crops.put(Material.SWEET_BERRY_BUSH, Material.SWEET_BERRIES);
        crops.put(Material.SUGAR_CANE, Material.SUGAR_CANE);
        crops.put(Material.CACTUS, Material.CACTUS);
    }

    public AutoFarm2() {
        super();
    }

    public AutoFarm2(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public String getItemName() {
        return "Auto Farm MkII";
    }

    @Override
    public String[] getLore() {
        return new String[] { "Automatically harvests and replants", "Cocoa Beans/Sugar Cane/Cactus", "in a " + RADIUS + "x" + RADIUS + " Radius 2 Blocks above the Machine" };
    }

    @Override
    protected int getRadius() {
        return RADIUS;
    }

    @Override
    protected void populateBlocks(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        int range = getRadius() / 2;
        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();

        for (int y = 0; y <= 3; y++) {
            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    blocks.add(location.getWorld().getBlockAt(bx + x, by + y, bz + z));
                }
            }
        }
    }

    @Override
    public Recipe getMainRecipe() {
        MachineFrame frame = new MachineFrame();
        GoldCombineHoe hoe = new GoldCombineHoe();
        registerCustomIngredients(frame, hoe);
        ShapedRecipe res = new ShapedRecipe(getKey(), toItemStack());
        res.shape("LHL", "IFI", "RGR");
        res.setIngredient('R', Material.REDSTONE);
        res.setIngredient('G', Material.GOLD_INGOT);
        res.setIngredient('I', Material.IRON_INGOT);
        res.setIngredient('L', new MaterialChoice(Tag.LOGS));
        res.setIngredient('H', hoe.getMaterial());
        res.setIngredient('F', frame.getMaterial());
        return res;
    }

    @Override
    protected void harvestCrops() {
        for (Block crop : blocks) {
            if (crops.containsKey(crop.getType())) {
                if (crop.getType() == Material.COCOA || crop.getType() == Material.SWEET_BERRY_BUSH) {
                    if (crop.getBlockData() instanceof Ageable) {
                        Ageable ageable = (Ageable) crop.getBlockData();

                        if (ageable.getAge() >= ageable.getMaximumAge()) {
                            if (getCharge() >= getScuPerCycle()) {
                                setCharge(getCharge() - getScuPerCycle());
                            } else {
                                break;
                            }

                            if (crop.getType() == Material.SWEET_BERRY_BUSH) {
                                ageable.setAge(1);
                            } else {
                                ageable.setAge(0);
                            }
                            crop.setBlockData(ageable);
                            crop.getWorld().playEffect(crop.getLocation(), Effect.STEP_SOUND, crop.getBlockData());
                            Material out = crops.get(crop.getType());
                            if (!output(out)) {
                                buffer = out;
                                setJammed(true);
                            }
                            break;
                        }
                    }
                } else {
                    // Sugar Cane & Cactus
                    // Only harvest if this is the base of the plant column
                    if (crop.getRelative(BlockFace.DOWN).getType() != crop.getType()) {
                        Block above = crop.getRelative(BlockFace.UP);
                        if (above.getType() == crop.getType()) {
                            // Find highest grown segment above base to harvest from top down
                            Block highest = above;
                            while (highest.getRelative(BlockFace.UP).getType() == crop.getType()) {
                                highest = highest.getRelative(BlockFace.UP);
                            }

                            if (getCharge() >= getScuPerCycle()) {
                                setCharge(getCharge() - getScuPerCycle());
                            } else {
                                break;
                            }

                            highest.getWorld().playEffect(highest.getLocation(), Effect.STEP_SOUND, highest.getBlockData());
                            Material out = crops.get(highest.getType());
                            highest.setType(Material.AIR);
                            if (!output(out)) {
                                buffer = out;
                                setJammed(true);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public double getScuPerCycle() {
        return 30.0;
    }
}

