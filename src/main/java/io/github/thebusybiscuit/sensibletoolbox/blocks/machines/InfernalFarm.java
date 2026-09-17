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

    private static final int RADIUS = 5;

    private final Set<Block> blocks = new HashSet<>();
    private Material buffer;

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
        return new String[] { "Automatically harvests and replants", "Nether Warts", "in a " + RADIUS + "x" + RADIUS + " Radius 2 Blocks above the Machine" };
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

    private void populateBlocks(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        int range = RADIUS / 2;
        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();
        for (int y = 0; y <= 2; y++) {
            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    blocks.add(location.getWorld().getBlockAt(bx + x, by + y, bz + z));
                }
            }
        }
    }

    @Override
    public void onBlockRegistered(Location location, boolean isPlacing) {
        populateBlocks(location);
        super.onBlockRegistered(location, isPlacing);
    }

    @Override
    public void onServerTick() {
        if (blocks.isEmpty() && getLocation() != null) {
            populateBlocks(getLocation());
        }

        if (!isJammed()) {
            if (getCharge() >= getScuPerCycle()) {
                for (Block crop : blocks) {
                    if (crop.getType() == Material.NETHER_WART) {
                        Ageable ageable = (Ageable) crop.getBlockData();

                        if (ageable.getAge() >= ageable.getMaximumAge()) {
                            setCharge(getCharge() - getScuPerCycle());

                            ageable.setAge(0);
                            crop.setBlockData(ageable);
                            crop.getWorld().playEffect(crop.getLocation(), Effect.STEP_SOUND, crop.getType());
                            if (!output(Material.NETHER_WART)) {
                                buffer = Material.NETHER_WART;
                                setJammed(true);
                            }
                            break;
                        }
                    }
                }
            }
        } else if (buffer != null) {
            if (output(buffer)) {
                buffer = null;
                setJammed(false);
            }
        }

        super.onServerTick();
    }

    @Override
    public double getScuPerCycle() {
        return 50.0;
    }
}
