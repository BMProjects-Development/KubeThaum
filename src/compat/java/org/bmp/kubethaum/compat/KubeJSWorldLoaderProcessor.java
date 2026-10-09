package org.bmp.kubethaum.compat;

import java.util.List;
import java.util.Set;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.BytecodeProvider;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.ClassReader;

/** Repairs the released KubeJS 8.0.6 injection descriptor before Mixin reads it. */
public final class KubeJSWorldLoaderProcessor implements ClassProcessor {
    private static final String MIXIN = "dev.latvian.mods.kubejs.core.mixin.WorldLoaderMixin";
    private static final String OLD_TARGET = "Lnet/minecraft/resources/RegistryDataLoader;load("
        + "Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;"
        + "Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;";
    private static final String NEW_TARGET = OLD_TARGET.replace("Executor;)", "Executor;Ljava/util/List;)");
    private BytecodeProvider bytecode;

    @Override
    public void link(LinkContext context) {
        bytecode = context.bytecodeProvider();
    }

    @Override
    public ProcessorName name() {
        return new ProcessorName("kubethaum", "kubejs_world_loader");
    }

    @Override
    public Set<ProcessorName> runsBefore() {
        return Set.of(ClassProcessorIds.MIXIN);
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return !context.empty() && MIXIN.equals(context.type().getClassName());
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        // No game classes may be loaded here: this code runs on the FML plugin layer.
        if (!hasPendingTags()) return ComputeFlags.NO_REWRITE;
        for (var method : context.node().methods) {
            if (!method.name.equals("kjs$load") || method.visibleAnnotations == null) continue;
            for (var injection : method.visibleAnnotations) {
                if (!injection.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) continue;
                if (!(value(injection, "at") instanceof List<?> points)) continue;
                for (var point : points) {
                    if (point instanceof AnnotationNode at
                        && at.desc.equals("Lorg/spongepowered/asm/mixin/injection/At;")
                        && "INVOKE".equals(value(at, "value"))
                        && OLD_TARGET.equals(value(at, "target"))) {
                        at.values.set(at.values.indexOf("target") + 1, NEW_TARGET);
                        context.audit("Updated KubeJS WorldLoader injection for NeoForge pending registry tags");
                        System.getLogger("KubeThaum").log(System.Logger.Level.INFO,
                            "Updated KubeJS WorldLoader hook for the NeoForge pendingTags API");
                        return ComputeFlags.SIMPLE_REWRITE;
                    }
                }
            }
        }
        // An upstream-fixed mixin is left intact; no dependency JAR is edited on disk.
        return ComputeFlags.NO_REWRITE;
    }

    private boolean hasPendingTags() {
        try {
            var loader = new ClassNode();
            new ClassReader(bytecode.getByteCode("net.minecraft.resources.RegistryDataLoader"))
                .accept(loader, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return loader.methods.stream().anyMatch(method -> method.name.equals("load")
                && method.desc.equals(NEW_TARGET.substring(NEW_TARGET.indexOf('('))));
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("KubeThaum cannot inspect the registry loading API", ex);
        }
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values == null) return null;
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        }
        return null;
    }
}
