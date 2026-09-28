package io.github.thebusybiscuit.sensibletoolbox.core.enderstorage;

import org.bukkit.ChatColor;

import io.github.thebusybiscuit.sensibletoolbox.utils.UnicodeSymbol;

import java.io.File;

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
        return new File(global, Integer.toString(getFrequency()));
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
