package org.bmp.kubethaum.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import org.bmp.kubethaum.research.ResearchResources;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RegistryDataLoader.class)
public abstract class PendingTagsRegistryDataLoaderMixin {
    @WrapMethod(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/List;)Ljava/util/concurrent/CompletableFuture;")
    private static CompletableFuture<RegistryAccess.Frozen> kubethaum$researchWithTags(
        ResourceManager resources, List<HolderLookup.RegistryLookup<?>> context,
        List<RegistryDataLoader.RegistryData<?>> registries, Executor executor,
        List<Registry.PendingTags<?>> pendingTags,
        Operation<CompletableFuture<RegistryAccess.Frozen>> original) {
        var prepared = ResearchResources.apply(resources, registries);
        return original.call(prepared, context, registries, executor, pendingTags).thenApply(access -> {
            ResearchResources.logSummary(prepared); return access;
        });
    }
}
