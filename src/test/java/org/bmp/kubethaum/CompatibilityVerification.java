package org.bmp.kubethaum;

import java.util.Arrays;
import java.util.LinkedHashMap;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import org.bmp.kubethaum.compat.KubeJSWorldLoaderProcessor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

/** Checks the actual released KubeJS bytecode without loading Minecraft classes. */
public final class CompatibilityVerification {
    private static final String MIXIN = "dev.latvian.mods.kubejs.core.mixin.WorldLoaderMixin";
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
        byte[] original = bytes(mixin);
        link(processor, false);
        check(process(processor, mixin) == ClassProcessor.ComputeFlags.NO_REWRITE, "Leaves old NeoForge hook intact");
        check(Arrays.equals(original, bytes(mixin)), "Old loader leaves bytecode unchanged");
        link(processor, true);
        check(process(processor, mixin) == ClassProcessor.ComputeFlags.SIMPLE_REWRITE, "Repairs released KubeJS for pendingTags API");
        byte[] patched = bytes(mixin);
        check(!Arrays.equals(original, patched), "Repair reaches output bytecode");
        check(process(processor, mixin) == ClassProcessor.ComputeFlags.NO_REWRITE, "Already fixed KubeJS is a no-op");
        check(Arrays.equals(patched, bytes(mixin)), "Repeat processing preserves repaired bytecode");
        System.out.println("KubeThaum: " + checks + " compatibility checks passed");
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
