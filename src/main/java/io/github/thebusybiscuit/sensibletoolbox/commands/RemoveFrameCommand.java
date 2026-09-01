package io.github.thebusybiscuit.sensibletoolbox.commands;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import io.github.thebusybiscuit.sensibletoolbox.api.SensibleToolbox;
import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBItem;
import io.github.thebusybiscuit.sensibletoolbox.core.storage.LocationManager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

/**
 * Removes an END_PORTAL_FRAME left behind by an Ender Bag whose item data was stripped.
 * That block is unbreakable in survival, so a player who ends up with one in their base has
 * no way of getting rid of it; this gives staff a way to clear it and hand the bag back.
 */
public class RemoveFrameCommand extends AbstractCommand {

    private static final int TARGET_RANGE = 8;

    public RemoveFrameCommand() {
        super("stb rmframe", 0, 0);
        setPermissionNode("stb.commands.rmframe");
        setUsage("/<command> rmframe");
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MiscUtil.errorMessage(sender, "This command can't be run from the console.");
            return true;
        }

        Player player = (Player) sender;
        Block block = player.getTargetBlockExact(TARGET_RANGE);

        if (block == null || block.getType() != Material.END_PORTAL_FRAME) {
            MiscUtil.errorMessage(sender, "You must be looking at an End Portal Frame (within " + TARGET_RANGE + " blocks).");
            return true;
        }

        if (LocationManager.getManager().get(block.getLocation(), true) != null) {
            MiscUtil.errorMessage(sender, "That block is a live STB block; break it normally instead.");
            return true;
        }

        block.setType(Material.AIR);
        BaseSTBItem bag = SensibleToolbox.getItemRegistry().getItemById("enderbag");

        if (bag != null) {
            player.getInventory().addItem(bag.toItemStack());
            MiscUtil.statusMessage(sender, "Orphaned End Portal Frame removed; an &6Ender Bag&- was returned to you.");
        } else {
            MiscUtil.statusMessage(sender, "Orphaned End Portal Frame removed.");
        }

        return true;
    }
}
