package org.bmp.kubethaum;

import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import com.leclowndu93150.thaumaturge.content.research.ResearchCategory;
import com.leclowndu93150.thaumaturge.content.research.ResearchEntry;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.registry.ServerRegistryRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentTypeRegistry;
import dev.latvian.mods.kubejs.recipe.schema.RecipeFactoryRegistry;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import org.bmp.kubethaum.recipe.ThaumaturgeRecipes;

public final class KubeThaumPlugin implements KubeJSPlugin {
    @Override public void registerRecipeComponents(RecipeComponentTypeRegistry registry) {
        registry.register(ThaumaturgeRecipes.ASPECTS);
        registry.register(ThaumaturgeRecipes.CRYSTALS);
        registry.register(ThaumaturgeRecipes.GATE);
        registry.register(ThaumaturgeRecipes.ENCHANTMENT);
    }
    @Override public void registerRecipeFactories(RecipeFactoryRegistry registry) { registry.register(ThaumaturgeRecipes.FACTORY); }
    @Override public void registerRecipeSchemas(RecipeSchemaRegistry registry) { ThaumaturgeRecipes.register(registry); }
    @Override public void registerEvents(EventGroupRegistry registry) { registry.register(ThaumaturgeEvents.GROUP); }
    @Override public void registerBindings(BindingRegistry bindings) { bindings.add("Thaumaturge", ThaumaturgeBinding.class); }
    @Override public void registerServerRegistries(ServerRegistryRegistry registry) {
        registry.register(IResearchCategory.REGISTRY_KEY, ResearchCategory.CODEC, IResearchCategory.class);
        registry.register(IResearchEntry.REGISTRY_KEY, ResearchEntry.CODEC, IResearchEntry.class);
    }
}
