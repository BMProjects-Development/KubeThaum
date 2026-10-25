package org.bmp.kubethaum.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import org.bmp.kubethaum.research.ResearchResources;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RegistryDataLoader.class)
public abstract class RegistryDataLoaderMixin {
    @WrapMethod(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;")
    private static RegistryAccess.Frozen kubethaum$research(
        ResourceManager resources, RegistryAccess context,
        List<RegistryDataLoader.RegistryData<?>> registries,
        Operation<RegistryAccess.Frozen> original) {
        var prepared = ResearchResources.apply(resources, registries);
        var access = original.call(prepared, context, registries);
        ResearchResources.logSummary(prepared);
        return access;
    }
}
