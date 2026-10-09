package org.bmp.kubethaum;

import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import com.leclowndu93150.thaumaturge.content.research.ResearchCategory;
import com.leclowndu93150.thaumaturge.content.research.ResearchEntry;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.registry.ServerRegistryRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public final class KubeThaumPlugin implements KubeJSPlugin {
    @Override public void registerEvents(EventGroupRegistry registry) { registry.register(ThaumaturgeEvents.GROUP); }
    @Override public void registerBindings(BindingRegistry bindings) { bindings.add("Thaumaturge", ThaumaturgeBinding.class); }
    @Override public void registerServerRegistries(ServerRegistryRegistry registry) {
        registry.register(IResearchCategory.REGISTRY_KEY, ResearchCategory.CODEC, IResearchCategory.class);
        registry.register(IResearchEntry.REGISTRY_KEY, ResearchEntry.CODEC, IResearchEntry.class);
    }
}
