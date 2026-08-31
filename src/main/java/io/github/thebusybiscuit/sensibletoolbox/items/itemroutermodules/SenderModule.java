package io.github.thebusybiscuit.sensibletoolbox.items.itemroutermodules;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;

import io.github.thebusybiscuit.sensibletoolbox.api.STBInventoryHolder;
import io.github.thebusybiscuit.sensibletoolbox.api.SensibleToolbox;
import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBBlock;
import io.github.thebusybiscuit.sensibletoolbox.blocks.router.ItemRouter;
import io.github.thebusybiscuit.sensibletoolbox.utils.VanillaInventoryUtils;
import me.desht.dhutils.Debugger;

public class SenderModule extends DirectionalItemRouterModule {

    private static final int MAX_SENDER_DISTANCE = 10;

    public SenderModule() {}

    public SenderModule(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public String getItemName() {
        return "I.R. Mod: Sender";
    }

    @Override
    public String[] getLore() {
        return makeDirectionalLore("Insert into an Item Router", "Sends items elsewhere:", " - An adjacent inventory OR", " - Item Router with Receiver Module:", "   within 10 blocks, with line of sight");
    }

    @Override
    public Recipe getMainRecipe() {
        BlankModule bm = new BlankModule();
        registerCustomIngredients(bm);
        ShapelessRecipe recipe = new ShapelessRecipe(getKey(), toItemStack());
        recipe.addIngredient(bm.getMaterial());
        recipe.addIngredient(Material.ARROW);
        return recipe;
    }

    @Override
    public Material getMaterial() {
        return Material.BLUE_DYE;
    }

    @Override
    public boolean execute(Location loc) {
        if (getItemRouter() != null && getItemRouter().getBufferItem() != null) {
            if (getFilter() != null && !getFilter().shouldPass(getItemRouter().getBufferItem())) {
                return false;
            }
            if (getFacing() == null || getFacing() == BlockFace.SELF) {
                return false;
            }
            Debugger.getInstance().debug(2, "sender in " + getItemRouter() + " has: " + getItemRouter().getBufferItem());
            Block b = loc.getBlock();
            Block target = b.getRelative(getFacing());
            int nToInsert = getItemRouter().getStackSize();

            BaseSTBBlock stb = SensibleToolbox.getBlockAt(target.getLocation(), true);
            if (stb instanceof ItemRouter targetRouter) {
                ReceiverModule receiver = targetRouter.getReceiver();
                if (receiver != null) {
                    ItemStack toSend = getItemRouter().getBufferItem().clone();
                    toSend.setAmount(Math.min(nToInsert, toSend.getAmount()));
                    int nReceived = receiver.receiveItem(toSend, getItemRouter().getOwner());
                    getItemRouter().reduceBuffer(nReceived);
                    if (nReceived > 0 && SensibleToolbox.getPluginInstance().getConfigCache().getParticleLevel() >= 2) {
                        playSenderParticles(getItemRouter(), targetRouter);
                    }
                    return nReceived > 0;
                }
            } else if (stb instanceof STBInventoryHolder holder) {
                ItemStack toInsert = getItemRouter().getBufferItem().clone();
                toInsert.setAmount(Math.min(nToInsert, toInsert.getAmount()));
                int nInserted = holder.insertItems(toInsert, getFacing().getOppositeFace(), false, getItemRouter().getOwner());
                getItemRouter().reduceBuffer(nInserted);
                return nInserted > 0;
            } else if (VanillaInventoryUtils.isVanillaInventory(target)) {
                return vanillaInsertion(target, nToInsert, getFacing().getOppositeFace());
            } else if (allowsItemsThrough(target.getType())) {
                ReceiverModule receiver = findReceiver(b);
                if (receiver != null) {
                    Debugger.getInstance().debug(2, "sender found receiver module in " + receiver.getItemRouter());
                    ItemStack toSend = getItemRouter().getBufferItem().clone();
                    toSend.setAmount(Math.min(nToInsert, toSend.getAmount()));
                    int nReceived = receiver.receiveItem(toSend, getItemRouter().getOwner());
                    getItemRouter().reduceBuffer(nReceived);

                    if (nReceived > 0 && SensibleToolbox.getPluginInstance().getConfigCache().getParticleLevel() >= 2) {
                        playSenderParticles(getItemRouter(), receiver.getItemRouter());
                    }

                    return nReceived > 0;
                }
            }
        }
        return false;
    }

    private void playSenderParticles(ItemRouter src, ItemRouter dest) {
        Location s = src.getLocation().clone();
        Location d = dest.getLocation().clone();
        double xOff = (d.getX() - s.getX()) / 2.0;
        double yOff = (d.getY() - s.getY()) / 2.0;
        double zOff = (d.getZ() - s.getZ()) / 2.0;
        Location mid = s.add(xOff + 0.5, yOff + 0.5, zOff + 0.5);
        if (mid.getWorld() != null) {
            mid.getWorld().spawnParticle(Particle.DUST, mid.getX(), mid.getY(), mid.getZ(), 15, Math.abs((float) xOff / 4.0f), Math.abs((float) yOff / 4.0f), Math.abs((float) zOff / 4.0f), 0, new DustOptions(Color.GREEN, 1.5F));
        }
    }

    private ReceiverModule findReceiver(Block b) {
        for (int i = 0; i < MAX_SENDER_DISTANCE; i++) {
            b = b.getRelative(getFacing());
            if (!allowsItemsThrough(b.getType())) {
                break;
            }
        }

        ItemRouter rtr = SensibleToolbox.getBlockAt(b.getLocation(), ItemRouter.class, false);
        return rtr == null ? null : rtr.getReceiver();
    }

    private boolean allowsItemsThrough(Material mat) {
        return !mat.isOccluding();
    }

}

