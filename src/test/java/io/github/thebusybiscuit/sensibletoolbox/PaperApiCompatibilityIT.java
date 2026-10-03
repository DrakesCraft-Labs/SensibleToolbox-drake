package io.github.thebusybiscuit.sensibletoolbox;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Binary compatibility check of the packaged jar against every supported Paper API.
 * <p>
 * The jar is compiled once (against 1.21.11) and must link on 1.21.11, 26.1.x and 26.2.x.
 * Source compatibility is not enough: a method whose return type changed, a removed field or
 * a class that became an interface compiles fine but throws {@link LinkageError} at runtime.
 * This test reads every Bukkit/Paper reference in the plugin bytecode and resolves it against
 * each API jar, exactly like the JVM linker would.
 */
class PaperApiCompatibilityIT {

    private static final List<String> API_PACKAGES = List.of("org/bukkit/", "io/papermc/paper/", "com/destroystokyo/paper/");
    private static final List<String> PLUGIN_PACKAGES = List.of("io/github/thebusybiscuit/sensibletoolbox/", "me/desht/dhutils/", "cl/drakescraft/");

    static Stream<Arguments> targetApis() {
        return Stream.of(
            Arguments.of("Paper 1.21.11", "paper-api-1.21.11.jar"),
            Arguments.of("Paper 26.1.x", "paper-api-26.1.jar"),
            Arguments.of("Paper 26.2.x", "paper-api-26.2.jar")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("targetApis")
    void packagedJarLinksAgainstPaperApi(String target, String apiJarName) throws IOException {
        Path apiJar = Path.of(System.getProperty("stb.compatApiDir")).resolve(apiJarName);
        assertTrue(Files.isRegularFile(apiJar), "Missing " + apiJar + " (copied by maven-dependency-plugin in pre-integration-test)");

        Map<String, ApiClass> api = indexApi(apiJar);
        assertFalse(api.isEmpty(), "Could not index " + apiJar);

        Set<String> problems = new TreeSet<>();
        for (Reference ref : collectReferences(Path.of(System.getProperty("stb.packagedJar")))) {
            String problem = ref.check(api);
            if (problem != null) {
                problems.add(problem + "   <- " + ref.from);
            }
        }

        assertTrue(problems.isEmpty(), "Plugin bytecode does not link against " + target + ":\n  " + String.join("\n  ", problems));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("targetApis")
    void checkerDetectsBrokenReferences(String target, String apiJarName) throws IOException {
        Map<String, ApiClass> api = indexApi(Path.of(System.getProperty("stb.compatApiDir")).resolve(apiJarName));

        // Sanity checks so this test can never pass by accident with an empty index.
        assertTrue(new Reference(Kind.INTERFACE_METHOD, "org/bukkit/entity/Player", "getName", "()Ljava/lang/String;", "self-test").check(api) == null, "Inherited interface method not resolved on " + target);
        assertTrue(new Reference(Kind.METHOD, "org/bukkit/inventory/ItemStack", "getAmount", "()I", "self-test").check(api) == null);
        assertTrue(new Reference(Kind.METHOD, "org/bukkit/Bukkit", "doesNotExist", "()V", "self-test").check(api) != null, "Missing method not detected");
        assertTrue(new Reference(Kind.METHOD, "org/bukkit/entity/Player", "getName", "()Ljava/lang/String;", "self-test").check(api) != null, "Class/interface mismatch not detected");
        assertTrue(new Reference(Kind.CLASS, "org/bukkit/DoesNotExist", null, null, "self-test").check(api) != null, "Missing class not detected");
        assertTrue(collectReferences(Path.of(System.getProperty("stb.packagedJar"))).size() > 1000, "Suspiciously few API references collected");
    }

    private static boolean isApi(String internalName) {
        for (String pkg : API_PACKAGES) {
            if (internalName.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPluginClass(String entryName) {
        for (String pkg : PLUGIN_PACKAGES) {
            if (entryName.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ API index

    private record ApiClass(String name, boolean isInterface, String superName, List<String> interfaces, Set<String> methods, Set<String> fields) {}

    private static Map<String, ApiClass> indexApi(Path apiJar) throws IOException {
        Map<String, ApiClass> index = new HashMap<>();

        try (JarFile jar = new JarFile(apiJar.toFile())) {
            for (JarEntry entry : jar.stream().toList()) {
                if (!entry.getName().endsWith(".class") || entry.getName().startsWith("META-INF/")) {
                    continue;
                }

                try (InputStream in = jar.getInputStream(entry)) {
                    new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                        private String name;
                        private boolean iface;
                        private String superName;
                        private List<String> interfaces;
                        private final Set<String> methods = new HashSet<>();
                        private final Set<String> fields = new HashSet<>();

                        @Override
                        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                            this.name = name;
                            this.iface = (access & Opcodes.ACC_INTERFACE) != 0;
                            this.superName = superName;
                            this.interfaces = interfaces == null ? List.of() : List.of(interfaces);
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            methods.add(name + descriptor);
                            return null;
                        }

                        @Override
                        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                            fields.add(name + ":" + descriptor);
                            return null;
                        }

                        @Override
                        public void visitEnd() {
                            index.put(name, new ApiClass(name, iface, superName, interfaces, methods, fields));
                        }
                    }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }

        return index;
    }

    // ------------------------------------------------------------------ plugin references

    private enum Kind {
        CLASS,
        METHOD,
        INTERFACE_METHOD,
        FIELD
    }

    private record Reference(Kind kind, String owner, String name, String descriptor, String from) {

        /**
         * @return null if the reference links, otherwise a description of the problem
         */
        String check(Map<String, ApiClass> api) {
            ApiClass ownerClass = api.get(owner);
            if (ownerClass == null) {
                return "missing class " + owner;
            }

            switch (kind) {
                case CLASS:
                    return null;
                case METHOD:
                    if (ownerClass.isInterface()) {
                        return "class became interface: " + owner + " (invoked as class)";
                    }
                    return resolve(api, true) ? null : "missing method " + owner + "." + name + descriptor;
                case INTERFACE_METHOD:
                    if (!ownerClass.isInterface()) {
                        return "interface became class: " + owner + " (invoked as interface)";
                    }
                    return resolve(api, true) ? null : "missing method " + owner + "." + name + descriptor;
                case FIELD:
                    return resolve(api, false) ? null : "missing field " + owner + "." + name + ":" + descriptor;
                default:
                    return null;
            }
        }

        /**
         * Walks the type hierarchy like the JVM does. JDK classes (java.lang.Object, Enum, ...)
         * are inspected through reflection. If the walk reaches a third-party library that is not
         * part of the API jar (e.g. Adventure) the member cannot be disproven and is accepted.
         */
        private boolean resolve(Map<String, ApiClass> api, boolean method) {
            String key = method ? name + descriptor : name + ":" + descriptor;
            Deque<String> queue = new ArrayDeque<>();
            Set<String> seen = new HashSet<>();
            queue.add(owner);

            while (!queue.isEmpty()) {
                String current = queue.poll();
                if (current == null || !seen.add(current)) {
                    continue;
                }

                ApiClass c = api.get(current);
                if (c == null) {
                    if (!current.startsWith("java/")) {
                        return true;
                    }
                    if (jdkHasMember(current, key, method)) {
                        return true;
                    }
                    continue;
                }

                if ((method ? c.methods() : c.fields()).contains(key)) {
                    return true;
                }

                if (c.superName() != null) {
                    queue.add(c.superName());
                }
                queue.addAll(c.interfaces());
            }

            return false;
        }

        private static boolean jdkHasMember(String internalName, String key, boolean method) {
            Class<?> type;
            try {
                type = Class.forName(internalName.replace('/', '.'), false, ClassLoader.getPlatformClassLoader());
            } catch (ClassNotFoundException | LinkageError x) {
                return true;
            }

            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                if (declares(c, key, method)) {
                    return true;
                }
                for (Class<?> i : c.getInterfaces()) {
                    if (declaresInHierarchy(i, key, method)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static boolean declaresInHierarchy(Class<?> iface, String key, boolean method) {
            if (declares(iface, key, method)) {
                return true;
            }
            for (Class<?> parent : iface.getInterfaces()) {
                if (declaresInHierarchy(parent, key, method)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean declares(Class<?> c, String key, boolean method) {
            if (method) {
                for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                    if (key.equals(m.getName() + Type.getMethodDescriptor(m))) {
                        return true;
                    }
                }
                for (java.lang.reflect.Constructor<?> ctor : c.getDeclaredConstructors()) {
                    if (key.equals("<init>" + Type.getConstructorDescriptor(ctor))) {
                        return true;
                    }
                }
            } else {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    if (key.equals(f.getName() + ":" + Type.getDescriptor(f.getType()))) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static List<Reference> collectReferences(Path pluginJar) throws IOException {
        List<Reference> refs = new ArrayList<>();

        try (JarFile jar = new JarFile(pluginJar.toFile())) {
            for (JarEntry entry : jar.stream().toList()) {
                if (!entry.getName().endsWith(".class") || !isPluginClass(entry.getName())) {
                    continue;
                }

                try (InputStream in = jar.getInputStream(entry)) {
                    new ClassReader(in).accept(new ReferenceCollector(refs), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }

        assertFalse(refs.isEmpty(), "No Bukkit references found; is " + pluginJar + " the plugin jar?");
        return refs;
    }

    private static final class ReferenceCollector extends ClassVisitor {

        private final List<Reference> refs;
        private String className;

        ReferenceCollector(List<Reference> refs) {
            super(Opcodes.ASM9);
            this.refs = refs;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            className = name;
            addType(superName, name);
            if (interfaces != null) {
                for (String i : interfaces) {
                    addType(i, name);
                }
            }
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            addDescriptor(descriptor, className + "." + name);
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String methodDescriptor, String signature, String[] exceptions) {
            String from = className + "." + methodName + methodDescriptor;
            addDescriptor(methodDescriptor, from);

            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    addMember(isInterface ? Kind.INTERFACE_METHOD : Kind.METHOD, owner, name, descriptor, from);
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    addMember(Kind.FIELD, owner, name, descriptor, from);
                }

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    addType(type, from);
                }

                @Override
                public void visitLdcInsn(Object value) {
                    if (value instanceof Type type && type.getSort() == Type.OBJECT) {
                        addType(type.getInternalName(), from);
                    }
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrap, Object... args) {
                    addDescriptor(descriptor, from);
                    for (Object arg : args) {
                        if (arg instanceof Handle handle) {
                            boolean field = handle.getTag() <= Opcodes.H_PUTSTATIC;
                            Kind kind = field ? Kind.FIELD : handle.isInterface() ? Kind.INTERFACE_METHOD : Kind.METHOD;
                            addMember(kind, handle.getOwner(), handle.getName(), handle.getDesc(), from);
                        }
                    }
                }
            };
        }

        private void addMember(Kind kind, String owner, String name, String descriptor, String from) {
            if (owner.startsWith("[")) {
                return;
            }
            if (isApi(owner)) {
                refs.add(new Reference(kind, owner, name, descriptor, from));
            }
            addDescriptor(descriptor, from);
        }

        private void addDescriptor(String descriptor, String from) {
            Type type = Type.getType(descriptor);
            if (type.getSort() == Type.METHOD) {
                addTypeOf(type.getReturnType(), from);
                for (Type arg : type.getArgumentTypes()) {
                    addTypeOf(arg, from);
                }
            } else {
                addTypeOf(type, from);
            }
        }

        private void addTypeOf(Type type, String from) {
            if (type.getSort() == Type.ARRAY) {
                type = type.getElementType();
            }
            if (type.getSort() == Type.OBJECT) {
                addType(type.getInternalName(), from);
            }
        }

        private void addType(String internalName, String from) {
            if (internalName == null) {
                return;
            }
            if (internalName.startsWith("[")) {
                addDescriptor(internalName, from);
                return;
            }
            if (isApi(internalName)) {
                refs.add(new Reference(Kind.CLASS, internalName, null, null, from));
            }
        }
    }
}
