package io.github.thebusybiscuit.sensibletoolbox.core.enderstorage;

import javax.annotation.Nonnull;

import org.bukkit.World;

/**
 * Stable, filesystem-safe namespace for ender storage. World UUIDs prevent an
 * item carried between modalities from opening the same storage channel.
 */
final class EnderStorageScope {

    static final String LEGACY_SCOPE = "legacy";

    private EnderStorageScope() {}

    @Nonnull
    static String forWorld(World world) {
        return world == null ? LEGACY_SCOPE : world.getUID().toString();
    }
}
