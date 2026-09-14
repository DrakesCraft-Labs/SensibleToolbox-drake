package io.github.thebusybiscuit.sensibletoolbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class PackagedMetricsIT {

    private static final String PLUGIN = "io/github/thebusybiscuit/sensibletoolbox/SensibleToolboxPlugin";
    private static final String BSTATS = "com/github/drakescraft_labs/sensibletoolbox/bstats/";
    private static final String METRICS = BSTATS + "bukkit/Metrics";

    @Test
    void compileClasspathUsesTheBStatsArtifactsInsteadOfTheCopyInsideSlimefun() throws Exception {
        assertArtifactOrigin(org.bstats.bukkit.Metrics.class, "bstats-bukkit-");
        assertArtifactOrigin(org.bstats.MetricsBase.class, "bstats-base-");
    }

    @Test
    void onEnableInvokesAConstructorPresentInThePackagedMetrics() throws Exception {
        try (JarFile jar = openPluginJar(); URLClassLoader loader = metricsLoader()) {
            List<String> descriptors = new ArrayList<>();
            readClass(jar, PLUGIN).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                        String signature, String[] exceptions) {
                    if (!name.equals("onEnable") || !descriptor.equals("()V")) {
                        return null;
                    }
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name,
                                String descriptor, boolean isInterface) {
                            if (owner.endsWith("/bstats/bukkit/Metrics") && name.equals("<init>")) {
                                assertEquals(METRICS, owner, "STB must use its private bStats package");
                                descriptors.add(descriptor);
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            assertEquals(1, descriptors.size(), "Expected one metrics constructor call in onEnable");
            Class<?> metrics = loader.loadClass(METRICS.replace('/', '.'));
            assertSame(loader, metrics.getClassLoader(), "Inspect the packaged class, not the test classpath");
            Class<?>[] parameters = MethodType.fromMethodDescriptorString(descriptors.getFirst(), loader)
                .parameterArray();
            assertNotNull(metrics.getConstructor(parameters),
                "The constructor invoked by onEnable must exist in the shaded JAR");
        }
    }

    @Test
    void metricsInitializesAndShutsDownFromThePackagedJarWithTelemetryDisabled() throws Exception {
        MockBukkit.mock();
        try (URLClassLoader loader = metricsLoader()) {
            JavaPlugin plugin = MockBukkit.createMockPlugin("STBMetricsTest");
            Path config = plugin.getDataFolder().toPath().getParent().resolve("bStats/config.yml");
            Files.createDirectories(config.getParent());
            Files.writeString(config, "enabled: false\nserverUuid: 00000000-0000-0000-0000-000000000001\n");

            Class<?> metrics = loader.loadClass(METRICS.replace('/', '.'));
            Object instance = metrics.getConstructor(JavaPlugin.class, int.class).newInstance(plugin, 6354);
            metrics.getMethod("shutdown").invoke(instance);
        } finally {
            MockBukkit.unmock();
        }
    }

    @Test
    void bStatsIsRelocatedAndContainsItsRuntimeDependencies() throws Exception {
        try (JarFile jar = openPluginJar(); URLClassLoader loader = metricsLoader()) {
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().startsWith("org/bstats/")),
                "Unrelocated bStats classes can collide with other plugins");
            for (String name : List.of("bukkit/Metrics", "MetricsBase", "charts/CustomChart", "json/JsonObjectBuilder")) {
                assertNotNull(jar.getJarEntry(BSTATS + name + ".class"), "Missing bStats dependency: " + name);
                assertSame(loader, loader.loadClass((BSTATS + name).replace('/', '.')).getClassLoader());
            }
        }
    }

    private static Path pluginJar() {
        Path path = Path.of(System.getProperty("stb.packagedJar"));
        assertTrue(Files.isRegularFile(path), "Run mvn verify to test the final shaded JAR: " + path);
        return path;
    }

    private static void assertArtifactOrigin(Class<?> type, String artifactPrefix) throws Exception {
        Path origin = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertTrue(origin.getFileName().toString().startsWith(artifactPrefix),
            type.getName() + " is shadowed by another dependency: " + origin);
    }

    private static JarFile openPluginJar() throws IOException {
        return new JarFile(pluginJar().toFile());
    }

    private static URLClassLoader metricsLoader() throws IOException {
        return new URLClassLoader(new URL[] { pluginJar().toUri().toURL() }, PackagedMetricsIT.class.getClassLoader());
    }

    private static ClassReader readClass(JarFile jar, String name) throws IOException {
        var entry = jar.getJarEntry(name + ".class");
        assertNotNull(entry, "Missing packaged class: " + name);
        try (InputStream input = jar.getInputStream(entry)) {
            return new ClassReader(input);
        }
    }
}
