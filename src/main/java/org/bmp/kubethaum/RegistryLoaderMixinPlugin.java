package org.bmp.kubethaum;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Selects exactly one registry hook across the NeoForge pendingTags API change. */
public final class RegistryLoaderMixinPlugin implements IMixinConfigPlugin {
    private boolean pendingTags;

    @Override public void onLoad(String mixinPackage) {
        try {
            var loader = MixinService.getService().getBytecodeProvider()
                .getClassNode("net.minecraft.resources.RegistryDataLoader");
            pendingTags = loader.methods.stream().anyMatch(method -> method.name.equals("load")
                && method.desc.equals("(Lnet/minecraft/server/packs/resources/ResourceManager;"
                    + "Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/List;)"
                    + "Ljava/util/concurrent/CompletableFuture;"));
        } catch (Exception ex) {
            throw new IllegalStateException("KubeThaum cannot inspect the registry loading API", ex);
        }
    }

    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return mixinClassName.endsWith(".PendingTagsRegistryDataLoaderMixin") == pendingTags;
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
