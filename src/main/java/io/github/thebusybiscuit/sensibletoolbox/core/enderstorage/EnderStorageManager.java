package io.github.thebusybiscuit.sensibletoolbox.core.enderstorage;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.Validate;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import io.github.thebusybiscuit.sensibletoolbox.SensibleToolboxPlugin;
import io.github.thebusybiscuit.sensibletoolbox.api.enderstorage.EnderStorageHolder;
import io.github.thebusybiscuit.sensibletoolbox.items.EnderBag;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.text.LogUtils;

public class EnderStorageManager implements Listener {

    public static final int MAX_ENDER_FREQUENCY = 1000;
    public static final int BAG_SIZE = 54;
    private static final String ENDER_STORAGE_DIR = "enderstorage";
    private final File storageDir;

    private final Map<String, Map<Integer, GlobalEnderHolder>> globalInvs = new HashMap<>();
    private final Map<UUID, Map<String, Map<Integer, PlayerEnderHolder>>> playerInvs = new HashMap<>();
    private final Set<EnderStorageHolder> updateNeeded = new HashSet<>();

    private static final FilenameFilter uuidFilter = (dir, name) -> MiscUtil.looksLikeUUID(name);

    // Mapeo prefijo-de-mundo -> modalidad y la modalidad a la que migran los datos antiguos.
    private final Map<String, String> modalityPrefixes = new HashMap<>();
    private final String legacyModality;

    public EnderStorageManager(SensibleToolboxPlugin plugin) {
        storageDir = new File(plugin.getDataFolder(), ENDER_STORAGE_DIR);

        this.legacyModality = plugin.getConfig().getString("enderstorage.legacy_modality", "slimefun");
        ConfigurationSection modalities = plugin.getConfig().getConfigurationSection("enderstorage.modalities");
        if (modalities != null) {
            for (String prefix : modalities.getKeys(false)) {
                modalityPrefixes.put(prefix.toLowerCase(Locale.ROOT), modalities.getString(prefix));
            }
        }

        if (!storageDir.exists()) {
            setupStorageStructure(plugin, storageDir);
        }
    }

    File getStorageDir() {
        return storageDir;
    }

    /** Modalidad (scope) a la que pertenece un mundo. */
    public String scopeFor(World world) {
        return EnderStorageScope.forWorld(world, modalityPrefixes);
    }

    /** True si este scope es la modalidad principal a la que migran los datos antiguos. */
    public boolean isLegacyTarget(String scope) {
        return legacyModality.equals(scope);
    }

    public GlobalEnderHolder getGlobalInventoryHolder(int frequency) {
        return getGlobalInventoryHolder(null, frequency);
    }

    public GlobalEnderHolder getGlobalInventoryHolder(World world, int frequency) {
        Validate.isTrue(frequency > 0 && frequency <= MAX_ENDER_FREQUENCY, "Frequency out of range: " + frequency);
        String scope = scopeFor(world);
        Map<Integer, GlobalEnderHolder> inventories = globalInvs.computeIfAbsent(scope, ignored -> new HashMap<>());
        GlobalEnderHolder h = inventories.get(frequency);

        if (h == null) {
            h = new GlobalEnderHolder(this, scope, frequency);

            try {
                h.loadInventory();
                inventories.put(frequency, h);
            } catch (IOException e) {
                LogUtils.severe("Can't load global ender storage: " + h.getSaveFile());
                return null;
            }
        }

        return h;
    }

    public PlayerEnderHolder getPlayerInventoryHolder(OfflinePlayer player, Integer frequency) {
        return getPlayerInventoryHolder(player, null, frequency);
    }

    public PlayerEnderHolder getPlayerInventoryHolder(OfflinePlayer player, World world, Integer frequency) {
        Validate.isTrue(frequency > 0 && frequency <= MAX_ENDER_FREQUENCY, "Frequency out of range: " + frequency);
        String scope = scopeFor(world);
        Map<String, Map<Integer, PlayerEnderHolder>> scopedInventories = playerInvs.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        Map<Integer, PlayerEnderHolder> map = scopedInventories.computeIfAbsent(scope, ignored -> new HashMap<>());

        PlayerEnderHolder h = map.get(frequency);

        if (h == null) {
            h = new PlayerEnderHolder(this, player, scope, frequency);

            try {
                h.loadInventory();
                map.put(frequency, h);
            } catch (IOException e) {
                LogUtils.severe("Can't load player ender storage: " + h.getSaveFile());
                return null;
            }
        }

        return h;
    }

    private void setupStorageStructure(SensibleToolboxPlugin plugin, File storageDir) {
        mkdir(storageDir);
        File globalDir = new File(storageDir, "global");
        mkdir(globalDir);

        // migrate any old data in the bagofholding folder to personal ender channel "1"
        File oldDir = new File(plugin.getDataFolder(), EnderBag.BAG_SAVE_DIR);

        if (oldDir.exists()) {
            for (File f : oldDir.listFiles(uuidFilter)) {
                File newDir = new File(storageDir, f.getName());
                mkdir(newDir);
                File newFile = new File(newDir, "1");
                Validate.isTrue(f.renameTo(newFile), "can't move " + f + " to " + newFile);
            }

            for (File f : oldDir.listFiles()) {
                try {
                    Files.delete(f.toPath());
                } catch (IOException e) {
                    LogUtils.warning("can't delete unwanted file: " + f, e);
                }
            }

            try {
                Files.delete(oldDir.toPath());
            } catch (IOException e) {
                LogUtils.warning("can't delete old bagofholding directory", e);
            }
        }
    }

    void mkdir(File dir) {
        Validate.isTrue(dir.isDirectory() || dir.mkdirs(), "can't create directory: " + dir);
    }

    void setChanged(EnderStorageHolder holder) {
        updateNeeded.add(holder);
    }

    public void tick() {
        if (!updateNeeded.isEmpty()) {
            for (EnderStorageHolder holder : updateNeeded) {
                ((STBEnderStorageHolder) holder).saveInventory();
            }

            updateNeeded.clear();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof STBEnderStorageHolder) {
            EnderStorageHolder h = (EnderStorageHolder) event.getInventory().getHolder();
            setChanged(h);
        }
    }
}
