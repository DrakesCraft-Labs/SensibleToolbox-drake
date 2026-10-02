package io.github.thebusybiscuit.sensibletoolbox.blocks.machines;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import com.github.drakescraft_labs.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.sensibletoolbox.api.items.AutoFarmingMachine;
import io.github.thebusybiscuit.sensibletoolbox.items.IronCombineHoe;
import io.github.thebusybiscuit.sensibletoolbox.items.components.MachineFrame;

public class AutoFarm extends AutoFarmingMachine {

    private static final Map<Material, Material> crops = new EnumMap<>(Material.class);
    private static final int RADIUS = 3;

    static {
        crops.put(Material.WHEAT, Material.WHEAT);
        crops.put(Material.POTATOES, Material.POTATO);
        crops.put(Material.CARROTS, Material.CARROT);
        crops.put(Material.BEETROOTS, Material.BEETROOT);
    }

    protected final Set<Block> blocks = new HashSet<>();
    protected Material buffer;

    public AutoFarm() {
        super();
    }

    public AutoFarm(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public Material getMaterial() {
        return Material.BROWN_TERRACOTTA;
    }

    @Override
    public String getItemName() {
        return "Auto Farm";
    }

    @Override
    public String[] getLore() {
        return new String[] { "Automatically harvests and replants", "Wheat/Potato/Carrot Crops", "in a " + RADIUS + "x" + RADIUS + " Radius 2 Blocks above the Machine" };
    }

    @Override
    public Recipe getMainRecipe() {
        MachineFrame frame = new MachineFrame();
        IronCombineHoe hoe = new IronCombineHoe();
        registerCustomIngredients(frame, hoe);
        ShapedRecipe res = new ShapedRecipe(getKey(), toItemStack());
        res.shape(" H ", "IFI", "RGR");
        res.setIngredient('R', Material.REDSTONE);
        res.setIngredient('G', Material.GOLD_INGOT);
        res.setIngredient('I', Material.IRON_INGOT);
        res.setIngredient('H', hoe.getMaterial());
        res.setIngredient('F', frame.getMaterial());
        return res;
    }

    protected int getRadius() {
        return RADIUS;
    }

    protected void populateBlocks(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        int range = getRadius() / 2;
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

    protected void harvestCrops() {
        if (getCharge() < getScuPerCycle()) {
            return;
        }

        for (Block crop : blocks) {
            if (crops.containsKey(crop.getType())) {
                if (crop.getBlockData() instanceof Ageable) {
                    Ageable ageable = (Ageable) crop.getBlockData();

                    if (ageable.getAge() >= ageable.getMaximumAge()) {
                        setCharge(getCharge() - getScuPerCycle());

                        ageable.setAge(0);
                        crop.setBlockData(ageable);
                        crop.getWorld().playEffect(crop.getLocation(), Effect.STEP_SOUND, crop.getType());
                        Material out = crops.get(crop.getType());
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

    @Override
    public void onServerTick() {
        if (blocks.isEmpty() && getLocation() != null) {
            populateBlocks(getLocation());
        }

        if (!isJammed()) {
            harvestCrops();
        } else if (buffer != null) {
            if (output(buffer)) {
                buffer = null;
                setJammed(false);
            }
        }

        super.onServerTick();
    }

    protected boolean output(@Nonnull Material m) {
        for (int slot : getOutputSlots()) {
            ItemStack stack = getInventoryItem(slot);

            if (stack == null || (stack.getType() == m && stack.getAmount() < stack.getMaxStackSize())) {
                if (stack == null) {
                    stack = new ItemStack(m);
                }

                int amount = 1;

                if (!m.isBlock()) {
                    amount = (stack.getMaxStackSize() - stack.getAmount()) > 3 ? (ThreadLocalRandom.current().nextInt(2) + 1) : (stack.getMaxStackSize() - stack.getAmount());
                }

                setInventoryItem(slot, new CustomItemStack(stack, stack.getAmount() + amount));
                buffer = null;
                return true;
            }
        }
        return false;
    }

    @Override
    public double getScuPerCycle() {
        return 25.0;
    }
}

