package io.github.thebusybiscuit.sensibletoolbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Validates the final shaded jar before it is published: metadata, bundled guide and the
 * bytecode level that decides which Java versions (and therefore which Minecraft versions)
 * can load it.
 */
class PackagedPluginDescriptorIT {

    /** Java 21 class file version. 1.21.11 servers run on Java 21, so nothing newer may ship. */
    private static final int JAVA_21 = 65;

    @Test
    void pluginYmlIsFilteredWithTheProjectVersion() throws IOException {
        YamlConfiguration yml = readYaml("plugin.yml");

        assertEquals(System.getProperty("stb.projectVersion"), yml.getString("version"));
        assertEquals("io.github.thebusybiscuit.sensibletoolbox.SensibleToolboxPlugin", yml.getString("main"));
        assertNotNull(yml.getConfigurationSection("commands.stb"), "The /stb command must be declared");
    }

    @Test
    void apiVersionLetsTheJarLoadOnEverySupportedServer() throws IOException {
        String apiVersion = readYaml("plugin.yml").getString("api-version");
        assertNotNull(apiVersion, "api-version is required, otherwise Paper loads the plugin in legacy mode");

        String[] parts = apiVersion.split("\\.");
        int release = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        // A server refuses plugins whose api-version is newer than itself; 1.21.11 is the oldest target.
        assertTrue(release == 1 && minor >= 13 && minor <= 21, "api-version " + apiVersion + " would not load on 1.21.11");
    }

    @Test
    void guidePermissionIsGrantedToEveryone() throws IOException {
        YamlConfiguration yml = readYaml("plugin.yml");

        assertEquals("true", yml.getString("permissions.stb.commands.guide.default"));
    }

    @Test
    void bothGuideLanguagesArePackagedAndParsable() throws IOException {
        for (String name : List.of("guide_en.yml", "guide_es.yml")) {
            String raw = readText(name);
            assertFalse(raw.contains("${"), name + " contains an unresolved Maven placeholder");

            YamlConfiguration yml = new YamlConfiguration();
            try {
                yml.loadFromString(raw);
            } catch (Exception x) {
                throw new AssertionError(name + " is not valid YAML", x);
            }
            assertTrue(yml.getConfigurationSection("topics").getKeys(false).size() >= 10, name + " has too few topics");
        }
    }

    @Test
    void everyPluginClassTargetsJava21() throws IOException {
        List<String> tooNew = new ArrayList<>();

        try (JarFile jar = openJar()) {
            for (JarEntry entry : jar.stream().toList()) {
                if (!entry.getName().endsWith(".class") || entry.getName().startsWith("META-INF/")) {
                    continue;
                }

                try (InputStream in = jar.getInputStream(entry)) {
                    byte[] header = in.readNBytes(8);
                    int major = ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
                    if (major > JAVA_21) {
                        tooNew.add(entry.getName() + " (" + major + ")");
                    }
                }
            }
        }

        assertTrue(tooNew.isEmpty(), "Classes compiled for a Java newer than 21 cannot load on 1.21.11: " + tooNew);
    }

    @Test
    void guideClassesArePackaged() throws IOException {
        try (JarFile jar = openJar()) {
            assertNotNull(jar.getJarEntry("io/github/thebusybiscuit/sensibletoolbox/commands/GuideCommand.class"));
            assertNotNull(jar.getJarEntry("io/github/thebusybiscuit/sensibletoolbox/core/guide/GuideBook.class"));
            assertNotNull(jar.getJarEntry("io/github/thebusybiscuit/sensibletoolbox/api/MinecraftVersion.class"));
        }
    }

    private static Path pluginJar() {
        Path path = Path.of(System.getProperty("stb.packagedJar"));
        assertTrue(Files.isRegularFile(path), "Run mvn verify to test the final shaded JAR: " + path);
        return path;
    }

    private static JarFile openJar() throws IOException {
        return new JarFile(pluginJar().toFile());
    }

    private static String readText(String name) throws IOException {
        try (JarFile jar = openJar()) {
            JarEntry entry = jar.getJarEntry(name);
            assertNotNull(entry, "Missing packaged resource: " + name);
            try (InputStream in = jar.getInputStream(entry)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private static YamlConfiguration readYaml(String name) throws IOException {
        try (JarFile jar = openJar()) {
            JarEntry entry = jar.getJarEntry(name);
            assertNotNull(entry, "Missing packaged resource: " + name);
            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        }
    }
}
