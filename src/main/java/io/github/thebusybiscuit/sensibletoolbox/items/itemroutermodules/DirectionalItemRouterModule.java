package io.github.thebusybiscuit.sensibletoolbox.items.itemroutermodules;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Directional;

import com.github.drakescraft_labs.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import com.github.drakescraft_labs.slimefun4.libraries.dough.items.ItemUtils;
import io.github.thebusybiscuit.sensibletoolbox.SensibleToolboxPlugin;
import io.github.thebusybiscuit.sensibletoolbox.api.STBInventoryHolder;
import io.github.thebusybiscuit.sensibletoolbox.api.SensibleToolbox;
import io.github.thebusybiscuit.sensibletoolbox.api.filters.Filter;
import io.github.thebusybiscuit.sensibletoolbox.api.filters.FilterType;
import io.github.thebusybiscuit.sensibletoolbox.api.filters.Filtering;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.GUIUtil;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.InventoryGUI;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.SlotType;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.gadgets.DirectionGadget;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.gadgets.FilterTypeGadget;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.gadgets.ToggleButton;
import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBBlock;
import io.github.thebusybiscuit.sensibletoolbox.blocks.router.ItemRouter;
import io.github.thebusybiscuit.sensibletoolbox.utils.UnicodeSymbol;
import io.github.thebusybiscuit.sensibletoolbox.utils.VanillaInventoryUtils;

public abstract class DirectionalItemRouterModule extends ItemRouterModule implements Filtering, Directional {

    private static final String LIST_ITEM = ChatColor.LIGHT_PURPLE + UnicodeSymbol.CENTERED_POINT.toUnicode() + " " + ChatColor.AQUA;

    private static final ItemStack WHITE_BUTTON = GUIUtil.makeTexture(Material.WHITE_WOOL, ChatColor.WHITE.toString() + ChatColor.UNDERLINE + "Whitelist", "Module will only process", "items which match the filter.");
    private static final ItemStack BLACK_BUTTON = GUIUtil.makeTexture(Material.BLACK_WOOL, ChatColor.WHITE.toString() + ChatColor.UNDERLINE + "Blacklist", "Module will NOT process", "items which match the filter.");
    private static final ItemStack OFF_BUTTON = GUIUtil.makeTexture(Material.LIGHT_BLUE_STAINED_GLASS, ChatColor.WHITE.toString() + ChatColor.UNDERLINE + "Termination OFF", "Subsequent modules in the", "Item Router will process items", "as normal.");
    private static final ItemStack ON_BUTTON = GUIUtil.makeTexture(Material.ORANGE_WOOL, ChatColor.WHITE.toString() + ChatColor.UNDERLINE + "Termination ON", "If this module processes an", "item, the Item Router will", "not process any more items", "on this tick.");

    public static final int FILTER_LABEL_SLOT = 0;
    public static final int DIRECTION_LABEL_SLOT = 5;
    private final Filter filter;
    private BlockFace direction;
    private boolean terminator;
    private InventoryGUI gui;
    private final int[] filterSlots = { 1, 2, 3, 10, 11, 12, 19, 20, 21 };

    /**
     * Run this module's action.
     *
     * @param loc
     *            the location of the module's owning item router
     * @return true if the module did some work on this tick
     */
    public abstract boolean execute(Location loc);

    public DirectionalItemRouterModule() {
        // default filter: blacklist, no items
        filter = new Filter();
        setFacingDirection(BlockFace.SELF);
    }

    public DirectionalItemRouterModule(ConfigurationSection conf) {
        super(conf);
        String dirStr = conf.getString("direction");
        try {
            setFacingDirection(dirStr != null ? BlockFace.valueOf(dirStr) : BlockFace.SELF);
        } catch (IllegalArgumentException e) {
            setFacingDirection(BlockFace.SELF);
        }
        setTerminator(conf.getBoolean("terminator", false));

        if (conf.contains("filtered")) {
            boolean isWhite = conf.getBoolean("filterWhitelist", true);
            FilterType filterType;
            try {
                filterType = FilterType.valueOf(conf.getString("filterType", "MATERIAL"));
            } catch (IllegalArgumentException e) {
                filterType = FilterType.MATERIAL;
            }
            @SuppressWarnings("unchecked")
            List<ItemStack> l = (List<ItemStack>) conf.getList("filtered");
            List<ItemStack> sanitized = new ArrayList<>();
            if (l != null) {
                for (ItemStack s : l) {
                    if (s != null && !s.getType().isAir()) {
                        ItemStack item = sanitizeFilterItem(s, filterType);
                        if (item != null) {
                            sanitized.add(item);
                        }
                    }
                }
            }
            filter = Filter.fromItemList(isWhite, sanitized, filterType);
        } else {
            filter = new Filter();
        }
    }

    public static ItemStack sanitizeFilterItem(ItemStack original, FilterType filterType) {
        if (original == null || original.getType().isAir()) {
            return null;
        }
        if (filterType == FilterType.MATERIAL) {
            return new ItemStack(original.getType());
        }
        ItemStack clean = original.clone();
        clean.setAmount(1);
        ItemMeta meta = clean.getItemMeta();
        if (meta != null) {
            boolean modified = false;
            if (meta instanceof BlockStateMeta bsm && bsm.hasBlockState()) {
                if (bsm.getBlockState() instanceof Container c) {
                    c.getInventory().clear();
                    bsm.setBlockState(c);
                    modified = true;
                }
            }
            if (meta instanceof BundleMeta bm) {
                if (!bm.getItems().isEmpty()) {
                    bm.setItems(Collections.emptyList());
                    modified = true;
                }
            }
            try {
                NamespacedKey stbKey = SensibleToolboxPlugin.getInstance().getItemRegistry().getKey();
                if (PersistentDataAPI.hasString(meta, stbKey)) {
                    String existing = PersistentDataAPI.getString(meta, stbKey);
                    if (existing != null && existing.length() > 200) {
                        YamlConfiguration itemConf = YamlConfiguration.loadConfiguration(new java.io.StringReader(existing));
                        String typeId = itemConf.getString("*TYPE");
                        YamlConfiguration minimalConf = new YamlConfiguration();
                        if (typeId != null) {
                            minimalConf.set("*TYPE", typeId);
                        }
                        PersistentDataAPI.setString(meta, stbKey, minimalConf.saveToString());
                        modified = true;
                    }
                }
            } catch (Exception ignored) {
            }
            if (modified) {
                clean.setItemMeta(meta);
            }
        }
        return clean;
    }

    @Override
    public YamlConfiguration freeze() {
        YamlConfiguration conf = super.freeze();
        conf.set("direction", getFacing().toString());
        conf.set("terminator", isTerminator());

        if (filter != null) {
            List<ItemStack> sanitizedList = new ArrayList<>();
            for (ItemStack stack : filter.getFilterList()) {
                ItemStack sanitized = sanitizeFilterItem(stack, filter.getFilterType());
                if (sanitized != null) {
                    sanitizedList.add(sanitized);
                }
            }
            conf.set("filtered", sanitizedList);
            conf.set("filterWhitelist", filter.isWhiteList());
            conf.set("filterType", filter.getFilterType().toString());

            String saved = conf.saveToString();
            if (saved.getBytes(StandardCharsets.UTF_8).length > 32000) {
                List<ItemStack> materialOnly = new ArrayList<>();
                for (ItemStack stack : sanitizedList) {
                    materialOnly.add(new ItemStack(stack.getType()));
                }
                conf.set("filtered", materialOnly);
                conf.set("filterType", FilterType.MATERIAL.toString());
                if (SensibleToolboxPlugin.getInstance() != null) {
                    SensibleToolboxPlugin.getInstance().getLogger().warning(
                        "DirectionalItemRouterModule filter serialized size exceeded 32KB; degraded to MATERIAL only to prevent packet crash."
                    );
                }
            }
        }
        return conf;
    }

    @Override
    public String[] getExtraLore() {
        if (filter == null) {
            return new String[0];
        } else {
            String[] lore = new String[(filter.size() + 1) / 2 + 2];
            String what = filter.isWhiteList() ? "white-listed" : "black-listed";
            String s = filter.size() == 1 ? "" : "s";
            lore[0] = ChatColor.GOLD.toString() + filter.size() + " item" + s + " " + what;

            if (isTerminator()) {
                lore[0] += ", " + ChatColor.BOLD + "Terminating";
            }

            lore[1] = ChatColor.GOLD + filter.getFilterType().getLabel();
            int i = 2;

            for (ItemStack stack : filter.getFilterList()) {
                int n = i / 2 + 1;
                String name = ItemUtils.getItemName(stack);
                lore[n] = lore[n] == null ? LIST_ITEM + name : lore[n] + " " + LIST_ITEM + name;
                i++;
            }

            return lore;
        }
    }

    @Override
    public String getDisplaySuffix() {
        return direction != BlockFace.SELF ? direction.toString() : null;
    }

    @Override
    public void setFacingDirection(BlockFace blockFace) {
        direction = blockFace;
    }

    @Override
    public BlockFace getFacing() {
        return direction;
    }

    public Filter getFilter() {
        return filter;
    }

    public boolean isTerminator() {
        return terminator;
    }

    public void setTerminator(boolean terminator) {
        this.terminator = terminator;
    }

    @Override
    public void onInteractItem(PlayerInteractEvent event) {
        org.bukkit.inventory.EquipmentSlot hand = event.getHand();
        if (hand == null) hand = org.bukkit.inventory.EquipmentSlot.HAND;
        ItemStack held = event.getPlayer().getInventory().getItem(hand);
        int amount = held == null ? 1 : held.getAmount();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // set module direction based on clicked block face
            setFacingDirection(event.getBlockFace().getOppositeFace());
            event.getPlayer().getInventory().setItem(hand, toItemStack(amount));
            event.setCancelled(true);
        } else if (event.getAction() == Action.LEFT_CLICK_AIR && event.getPlayer().isSneaking()) {
            // unset module direction
            setFacingDirection(BlockFace.SELF);
            event.getPlayer().getInventory().setItem(hand, toItemStack(amount));
            event.setCancelled(true);
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemRouter rtr = event.getClickedBlock() == null ? null : SensibleToolbox.getBlockAt(event.getClickedBlock().getLocation(), ItemRouter.class, true);
            if (event.getClickedBlock() == null || (rtr == null && !event.getClickedBlock().getType().isInteractable())) {
                // open module configuration GUI
                gui = createGUI(event.getPlayer());
                gui.show(event.getPlayer());
                event.setCancelled(true);
            }
        }
    }

    private InventoryGUI createGUI(Player player) {
        InventoryGUI inventory = GUIUtil.createGUI(player, this, 36, ChatColor.DARK_RED + "Module Configuration");

        inventory.addGadget(new ToggleButton(inventory, 28, getFilter().isWhiteList(), WHITE_BUTTON, BLACK_BUTTON, newValue -> {
            if (getFilter() != null) {
                getFilter().setWhiteList(newValue);
                return true;
            } else {
                return false;
            }
        }));

        inventory.addGadget(new FilterTypeGadget(inventory, 29));
        inventory.addGadget(new ToggleButton(inventory, 30, isTerminator(), ON_BUTTON, OFF_BUTTON, newValue -> {
            setTerminator(newValue);
            return true;
        }));

        inventory.addLabel("Filtered Items", FILTER_LABEL_SLOT, null, "Place up to 9 items", "in the filter " + UnicodeSymbol.ARROW_RIGHT.toUnicode());
        for (int slot : filterSlots) {
            inventory.setSlotType(slot, SlotType.ITEM);
        }
        populateFilterInventory(inventory.getInventory());

        inventory.addLabel("Module Direction", DIRECTION_LABEL_SLOT, null, "Set the direction that", "the module works in", "once installed in an", "Item Router");
        ItemStack texture = new ItemStack(new ItemRouter().getMaterial());
        GUIUtil.setDisplayName(texture, "No Direction");
        inventory.addGadget(new DirectionGadget(inventory, 16, texture));

        return inventory;
    }

    private void populateFilterInventory(Inventory inv) {
        int n = 0;
        for (ItemStack stack : filter.getFilterList()) {
            inv.setItem(filterSlots[n], stack);
            n++;
            if (n >= filterSlots.length) {
                break;
            }
        }
    }

    protected String[] makeDirectionalLore(String... lore) {
        String[] newLore = Arrays.copyOf(lore, lore.length + 2);
        newLore[lore.length] = "L-click Block: " + ChatColor.WHITE + " Set direction";
        newLore[lore.length + 1] = UnicodeSymbol.ARROW_UP.toUnicode() + " + L-click Air: " + ChatColor.WHITE + " Unset direction";
        return newLore;
    }

    @Override
    public boolean onSlotClick(HumanEntity player, int slot, ClickType click, ItemStack inSlot, ItemStack onCursor) {
        if (onCursor.getType() == Material.AIR) {
            gui.getInventory().setItem(slot, null);
        } else {
            FilterType ft = getFilter() != null ? getFilter().getFilterType() : FilterType.MATERIAL;
            ItemStack sanitized = sanitizeFilterItem(onCursor, ft);
            gui.getInventory().setItem(slot, sanitized);
        }
        return false;
    }

    @Override
    public boolean onPlayerInventoryClick(HumanEntity player, int slot, ClickType click, ItemStack inSlot, ItemStack onCursor) {
        return true;
    }

    @Override
    public int onShiftClickInsert(HumanEntity player, int slot, ItemStack toInsert) {
        return 0;
    }

    @Override
    public boolean onShiftClickExtract(HumanEntity player, int slot, ItemStack toExtract) {
        return false;
    }

    @Override
    public boolean onClickOutside(HumanEntity player) {
        return false;
    }

    @Override
    public void onGUIClosed(HumanEntity player) {
        filter.clear();

        for (int slot : filterSlots) {
            ItemStack stack = gui.getInventory().getItem(slot);

            if (stack != null) {
                ItemStack sanitized = sanitizeFilterItem(stack, filter.getFilterType());
                if (sanitized != null) {
                    filter.addItem(sanitized);
                }
            }
        }

        if (player instanceof Player p) {
            ItemStack main = p.getInventory().getItemInMainHand();
            ItemStack off = p.getInventory().getItemInOffHand();
            if (SensibleToolbox.getItemRegistry().isSTBItem(main, this.getClass())) {
                p.getInventory().setItemInMainHand(toItemStack(main.getAmount()));
            } else if (SensibleToolbox.getItemRegistry().isSTBItem(off, this.getClass())) {
                p.getInventory().setItemInOffHand(toItemStack(off.getAmount()));
            }
        }
    }

    protected boolean doPull(BlockFace from, Location loc) {
        ItemStack inBuffer = getItemRouter().getBufferItem();

        if (inBuffer != null && inBuffer.getAmount() >= inBuffer.getType().getMaxStackSize()) {
            return false;
        }

        int nToPull = getItemRouter().getStackSize();
        Location targetLoc = getTargetLocation(loc);
        ItemStack pulled;
        BaseSTBBlock stb = SensibleToolbox.getBlockAt(targetLoc, true);

        if (stb instanceof STBInventoryHolder) {
            pulled = ((STBInventoryHolder) stb).extractItems(from.getOppositeFace(), inBuffer, nToPull, getItemRouter().getOwner());
        } else {
            // possible vanilla inventory holder
            pulled = VanillaInventoryUtils.pullFromInventory(targetLoc.getBlock(), nToPull, inBuffer, getFilter(), getItemRouter().getOwner());
        }

        if (pulled != null) {
            if (stb != null) {
                stb.update(false);
            }

            getItemRouter().setBufferItem(inBuffer == null ? pulled : inBuffer);
            return true;
        }

        return false;
    }

    protected boolean vanillaInsertion(Block target, int amount, BlockFace side) {
        ItemStack buffer = getItemRouter().getBufferItem();
        int nInserted = VanillaInventoryUtils.vanillaInsertion(target, buffer, amount, side, false, getItemRouter().getOwner());

        if (nInserted == 0) {
            // no insertion happened
            return false;
        } else {
            // some or all items were inserted, buffer size has been adjusted accordingly
            getItemRouter().setBufferItem(buffer.getAmount() == 0 ? null : buffer);
            return true;
        }
    }

    protected Location getTargetLocation(Location loc) {
        BlockFace face = getFacing();
        return loc.clone().add(face.getModX(), face.getModY(), face.getModZ());
    }
}

