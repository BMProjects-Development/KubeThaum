package org.bmp.kubethaum;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.bmp.kubethaum.compat.KubeJSWorldLoaderProcessor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/** Checks the actual released KubeJS bytecode without loading Minecraft classes. */
public final class CompatibilityVerification {
    private static final String MIXIN = "dev.latvian.mods.kubejs.core.mixin.WorldLoaderMixin";
    private static final String OLD_TARGET = "Lnet/minecraft/resources/RegistryDataLoader;load("
        + "Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;"
        + "Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;";
    private static final String NEW_TARGET = OLD_TARGET.replace("Executor;)", "Executor;Ljava/util/List;)");
    private static int checks;

    public static void main(String[] args) throws Exception {
        var processor = new KubeJSWorldLoaderProcessor();
        check(processor.handlesClass(new ClassProcessor.SelectionContext(Type.getObjectType(MIXIN.replace('.', '/')), false)), "Selects KubeJS mixin");
        check(!processor.handlesClass(new ClassProcessor.SelectionContext(Type.getObjectType("example/OtherMixin"), false)), "Ignores unrelated classes");
        check(!processor.handlesClass(new ClassProcessor.SelectionContext(Type.getObjectType(MIXIN.replace('.', '/')), true)), "Ignores missing classes");
        var mixin = new ClassNode();
        try (var input = CompatibilityVerification.class.getClassLoader().getResourceAsStream(MIXIN.replace('.', '/') + ".class")) {
            if (input == null) throw new AssertionError("Released KubeJS mixin missing from test classpath");
            new ClassReader(input).accept(mixin, 0);
        }
        String releasedTarget = (String)value(injectionPoint(mixin), "target");
        check(OLD_TARGET.equals(releasedTarget) || NEW_TARGET.equals(releasedTarget), "Recognizes released KubeJS hook");
        verifyHook(processor, mixin);
        // Exercise both released descriptor shapes regardless of the selected dependency.
        var alternate = copy(mixin);
        setTarget(alternate, OLD_TARGET.equals(releasedTarget) ? NEW_TARGET : OLD_TARGET);
        verifyHook(processor, alternate);
        var unknown = copy(mixin);
        setTarget(unknown, "Lexample/FutureLoader;load()V");
        byte[] unknownBytes = bytes(unknown);
        for (boolean pendingTags : new boolean[]{false, true}) {
            link(processor, pendingTags);
            check(process(processor, unknown) == ClassProcessor.ComputeFlags.NO_REWRITE, "Ignores unknown upstream hooks");
            check(Arrays.equals(unknownBytes, bytes(unknown)), "Unknown hook remains byte-for-byte intact");
        }
        verifyMetadata();
        System.out.println("KubeThaum: " + checks + " compatibility checks passed");
    }

    private static void verifyHook(KubeJSWorldLoaderProcessor processor, ClassNode released) {
        for (boolean pendingTags : new boolean[]{false, true}) {
            var actual = copy(released);
            var expected = copy(released);
            String target = pendingTags ? NEW_TARGET : (String)value(injectionPoint(actual), "target");
            boolean needsRewrite = !target.equals(value(injectionPoint(actual), "target"));
            setTarget(expected, target);
            link(processor, pendingTags);
            check(process(processor, actual) == (needsRewrite ? ClassProcessor.ComputeFlags.SIMPLE_REWRITE
                : ClassProcessor.ComputeFlags.NO_REWRITE), "Repairs legacy hooks and preserves upstream-fixed hooks");
            check(Arrays.equals(bytes(expected), bytes(actual)), "Only the registry injection target changes");
            byte[] patched = bytes(actual);
            check(process(processor, actual) == ClassProcessor.ComputeFlags.NO_REWRITE, "Matching hook is a no-op");
            check(Arrays.equals(patched, bytes(actual)), "Repeat processing preserves bytecode");
        }
    }

    private static void verifyMetadata() throws Exception {
        try (var input = CompatibilityVerification.class.getResourceAsStream("/META-INF/neoforge.mods.toml")) {
            if (input == null) throw new AssertionError("Generated mod metadata missing");
            var metadata = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            var dependency = Pattern.compile("(?s)modId=\"kubejs\"\\s+type=\"required\"\\s+versionRange=\"([^\"]+)\"").matcher(metadata);
            check(dependency.find(), "Generated metadata declares required KubeJS dependency");
            var range = VersionRange.createFromVersionSpec(dependency.group(1));
            for (String version : new String[]{"26.1.2-8.0.6", "26.1.2-8.0.7"}) {
                check(range.containsVersion(new DefaultArtifactVersion(version)), "Metadata accepts KubeJS " + version);
            }
            for (String version : new String[]{"26.1.2-8.0.5", "26.1.2-8.0.8", "26.1.2-8.1.0"}) {
                check(!range.containsVersion(new DefaultArtifactVersion(version)), "Metadata excludes untested KubeJS " + version);
            }
        }
    }

    private static ClassNode copy(ClassNode node) {
        var copy = new ClassNode();
        new ClassReader(bytes(node)).accept(copy, 0);
        return copy;
    }

    private static AnnotationNode injectionPoint(ClassNode node) {
        for (var method : node.methods) {
            if (!method.name.equals("kjs$load") || method.visibleAnnotations == null) continue;
            for (var annotation : method.visibleAnnotations) {
                if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) continue;
                if (value(annotation, "at") instanceof List<?> points) {
                    for (var point : points) {
                        if (point instanceof AnnotationNode at && "INVOKE".equals(value(at, "value"))) return at;
                    }
                }
            }
        }
        throw new AssertionError("Released KubeJS registry injection missing");
    }

    private static void setTarget(ClassNode node, String target) {
        var at = injectionPoint(node);
        at.values.set(at.values.indexOf("target") + 1, target);
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values != null) {
            for (int i = 0; i < annotation.values.size(); i += 2) {
                if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
            }
        }
        return null;
    }

    private static void link(KubeJSWorldLoaderProcessor processor, boolean pendingTags) {
        var loader = new ClassWriter(0);
        loader.visit(Opcodes.V25, Opcodes.ACC_PUBLIC, "net/minecraft/resources/RegistryDataLoader", null, "java/lang/Object", null);
        loader.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "load",
            "(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;"
                + (pendingTags ? "Ljava/util/List;" : "") + ")Ljava/util/concurrent/CompletableFuture;", null, null).visitEnd();
        loader.visitEnd();
        processor.link(new ClassProcessor.LinkContext(new LinkedHashMap<>(), name -> loader.toByteArray()));
    }

    private static ClassProcessor.ComputeFlags process(KubeJSWorldLoaderProcessor processor, ClassNode mixin) {
        return processor.processClass(new ClassProcessor.TransformationContext(Type.getObjectType(mixin.name), mixin,
            false, (activity, context) -> {}, () -> new byte[32]));
    }

    private static byte[] bytes(ClassNode node) {
        var writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
}
