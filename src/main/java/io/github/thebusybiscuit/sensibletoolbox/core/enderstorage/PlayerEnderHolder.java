package io.github.thebusybiscuit.sensibletoolbox.core.enderstorage;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import io.github.thebusybiscuit.sensibletoolbox.utils.UnicodeSymbol;

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
        File f = new File(root, getPlayer().getUniqueId().toString());
        return new File(f, Integer.toString(getFrequency()));
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
