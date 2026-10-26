package org.bmp.kubethaum.recipe;

import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.items.InfusionEnchantment;
import com.leclowndu93150.thaumaturge.api.recipe.ResearchGate;
import com.leclowndu93150.thaumaturge.content.recipe.workbench.ArcaneCraftingRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.*;
import dev.latvian.mods.kubejs.recipe.schema.*;
import net.minecraft.resources.ResourceLocation;

public final class ThaumaturgeRecipes {
    public static final RecipeComponentType<AspectList> ASPECTS = NativeRecipeComponent.type("aspects", AspectList.NON_EMPTY_CODEC, true);
    public static final RecipeComponentType<AspectList> CRYSTALS = NativeRecipeComponent.type("crystals", ArcaneCraftingRecipe.PRIMAL_ASPECTS_CODEC, true);
    public static final RecipeComponentType<ResearchGate> GATE = NativeRecipeComponent.type("research_gate", ResearchGate.CODEC, false);
    public static final RecipeComponentType<InfusionEnchantment> ENCHANTMENT = NativeRecipeComponent.type("infusion_enchantment", InfusionEnchantment.CODEC, false);
    public static final KubeRecipeFactory FACTORY = new KubeRecipeFactory(ResourceLocation.parse("kubethaum:recipe"), ThaumaturgeRecipe.class, ThaumaturgeRecipe::new);
    private ThaumaturgeRecipes() {}

    private static RecipeKey<?> result() { return ItemStackComponent.ITEM_STACK.outputKey("result"); }
    private static RecipeKey<?> catalyst() { return IngredientComponent.INGREDIENT.inputKey("catalyst"); }
    private static RecipeKey<?> components() { return IngredientComponent.INGREDIENT.instance().asList().inputKey("components"); }
    private static RecipeKey<?> aspects() { return ASPECTS.otherKey("aspects"); }
    // No generated builder function: ThaumaturgeRecipe.research(entry[, stage]) validates the gate.
    private static RecipeKey<?> research() { return GATE.otherKey("research").defaultOptional().noFunctions(); }
    private static RecipeKey<?> crystals() { return CRYSTALS.otherKey("crystals").optional(AspectList.EMPTY).alwaysWrite(); }
    private static RecipeKey<?> vis() { return NumberComponent.POSITIVE_INT.otherKey("vis"); }
    private static void register(RecipeSchemaRegistry registry, String name, RecipeKey<?>... keys) {
        registry.register(ResourceLocation.fromNamespaceAndPath("thaumaturge", name), new RecipeSchema(keys).factory(FACTORY));
    }

    public static void register(RecipeSchemaRegistry registry) {
        register(registry, "crucible", result(), catalyst(), aspects(), research());
        // Result is optional for native catalyst-patch recipes; only the fixed-result constructor is exposed.
        var output = ItemStackComponent.ITEM_STACK.outputKey("result").defaultOptional();
        var input = catalyst(); var ingredients = components(); var cost = aspects();
        registry.register(ResourceLocation.parse("thaumaturge:infusion"), new RecipeSchema(input, ingredients, cost,
            output, NumberComponent.intRange(0, 100).otherKey("instability").optional(0), research())
            .constructor(output, input, ingredients, cost).factory(FACTORY));
        register(registry, "infusion_enchantment", ENCHANTMENT.otherKey("enchantment"),
            IngredientComponent.INGREDIENT.inputKey("display_catalyst"), components(), aspects(), research());
        register(registry, "arcane_workbench_shaped", result(), StringComponent.STRING.instance().asList().otherKey("pattern"),
            IngredientComponent.INGREDIENT.instance().asMap(StringComponent.STRING.instance()).inputKey("key"), vis(), crystals(), research());
        register(registry, "arcane_workbench_shapeless", result(), IngredientComponent.INGREDIENT.instance().asList().inputKey("ingredients"),
            vis(), crystals(), research());
        register(registry, "dust_trigger_simple", result(), BlockComponent.BLOCK.inputKey("target"), research());
        register(registry, "dust_trigger_tag", result(), StringComponent.ID.otherKey("target_tag"), research());
        register(registry, "dust_trigger_multiblock", result(), StringComponent.ID.otherKey("blueprint"), research());
    }
}
