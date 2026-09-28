package io.github.thebusybiscuit.sensibletoolbox.core.enderstorage;

import org.bukkit.ChatColor;

import io.github.thebusybiscuit.sensibletoolbox.utils.UnicodeSymbol;
import me.desht.dhutils.text.LogUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class GlobalEnderHolder extends STBEnderStorageHolder {

    private final String scope;

    public GlobalEnderHolder(EnderStorageManager manager, String scope, int frequency) {
        super(manager, frequency);
        this.scope = scope;
    }

    @Override
    public String getInventoryTitle() {
        return ChatColor.DARK_PURPLE + "E-Storage " + ChatColor.DARK_RED + "[Global " + UnicodeSymbol.NUMBER.toUnicode() + getFrequency() + "]";
    }

    @Override
    public File getSaveFile() {
        File root = EnderStorageScope.LEGACY_SCOPE.equals(scope) ? getManager().getStorageDir() : new File(getManager().getStorageDir(), scope);
        File global = new File(root, "global");
        File newFile = new File(global, Integer.toString(getFrequency()));

        // Migración del "global" antiguo (<storageDir>/global/<freq>) a la modalidad principal.
        if (!newFile.isFile() && getManager().isLegacyTarget(scope)) {
            File legacy = new File(new File(getManager().getStorageDir(), "global"), Integer.toString(getFrequency()));
            if (legacy.isFile() && !legacy.equals(newFile)) {
                try {
                    global.mkdirs();
                    Files.move(legacy.toPath(), newFile.toPath());
                } catch (IOException e) {
                    LogUtils.warning("No se pudo migrar ender storage global legacy " + legacy + ": " + e.getMessage());
                }
            }
        }
        return newFile;
    }

    @Override
    public String toString() {
        return "Global Ender Storage #" + getFrequency();
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

}
