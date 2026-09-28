package io.github.thebusybiscuit.sensibletoolbox.core.enderstorage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import io.github.thebusybiscuit.sensibletoolbox.utils.UnicodeSymbol;
import me.desht.dhutils.text.LogUtils;

public class PlayerEnderHolder extends STBEnderStorageHolder {

    private final OfflinePlayer player;
    private final String scope;

    protected PlayerEnderHolder(EnderStorageManager manager, OfflinePlayer player, String scope, int frequency) {
        super(manager, frequency);
        this.player = player;
        this.scope = scope;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    @Override
    public File getSaveFile() {
        File root = EnderStorageScope.LEGACY_SCOPE.equals(scope) ? getManager().getStorageDir() : new File(getManager().getStorageDir(), scope);
        File playerDir = new File(root, getPlayer().getUniqueId().toString());
        File newFile = new File(playerDir, Integer.toString(getFrequency()));

        // Migración: los datos antiguos (formato sin modalidad, en la raíz) se mueven UNA
        // vez a la modalidad principal, sin duplicar, para que nadie pierda sus ítems.
        if (!newFile.isFile() && getManager().isLegacyTarget(scope)) {
            File legacy = new File(new File(getManager().getStorageDir(), getPlayer().getUniqueId().toString()), Integer.toString(getFrequency()));
            if (legacy.isFile() && !legacy.equals(newFile)) {
                try {
                    playerDir.mkdirs();
                    Files.move(legacy.toPath(), newFile.toPath());
                } catch (IOException e) {
                    LogUtils.warning("No se pudo migrar ender storage legacy " + legacy + ": " + e.getMessage());
                }
            }
        }
        return newFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        return player.equals(((PlayerEnderHolder) o).player);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + player.hashCode();
        return result;
    }

    @Override
    public String getInventoryTitle() {
        return ChatColor.DARK_PURPLE + "E-Storage " + ChatColor.DARK_RED + "[Personal " + UnicodeSymbol.NUMBER.toUnicode() + getFrequency() + "]";
    }

    @Override
    public String toString() {
        return "Player Ender Storage " + player.getName() + "#" + getFrequency();
    }

    @Override
    public boolean isGlobal() {
        return false;
    }
}
