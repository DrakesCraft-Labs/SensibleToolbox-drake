package io.github.thebusybiscuit.sensibletoolbox.core.enderstorage;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import org.bukkit.World;

/**
 * Espacio de nombres estable y seguro para el sistema de ficheros del Ender Storage.
 * <p/>
 * El almacenamiento se aísla por <b>modalidad</b> (mundo lógico), no por mundo físico:
 * todas las dimensiones y sub-mundos de una modalidad (p. ej. {@code world},
 * {@code world_nether}, {@code world_the_end} y los planetas {@code world_galactifun_*}
 * de survival) comparten el mismo Ender Bag, pero survival y skyblock quedan separados.
 * Así un objeto no puede pasar de una modalidad a otra abriendo el mismo canal.
 * <p/>
 * La modalidad se resuelve por el prefijo del nombre del mundo, según el mapeo de la
 * config ({@code enderstorage.modalities}); si ningún prefijo coincide, se cae de vuelta
 * a quitar el sufijo dimensional ({@code _nether}/{@code _the_end}).
 */
final class EnderStorageScope {

    static final String LEGACY_SCOPE = "legacy";

    private EnderStorageScope() {}

    @Nonnull
    static String forWorld(World world, @Nonnull Map<String, String> modalityPrefixes) {
        if (world == null) {
            return LEGACY_SCOPE;
        }

        String name = world.getName().toLowerCase(Locale.ROOT);

        // Prefijo configurado más específico (el más largo) que coincida con el mundo.
        String bestPrefix = null;
        String bestModality = null;
        for (Map.Entry<String, String> entry : modalityPrefixes.entrySet()) {
            String prefix = entry.getKey();
            if ((name.equals(prefix) || name.startsWith(prefix)) && (bestPrefix == null || prefix.length() > bestPrefix.length())) {
                bestPrefix = prefix;
                bestModality = entry.getValue();
            }
        }
        if (bestModality != null && !bestModality.isEmpty()) {
            return bestModality;
        }

        // Sin mapeo: agrupa las dimensiones bajo su mundo base.
        for (String suffix : new String[] { "_the_end", "_the_nether", "_nether", "_end" }) {
            if (name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return name;
    }
}
