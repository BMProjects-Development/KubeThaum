package org.bmp.kubethaum.recipe;

import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.items.InfusionEnchantment;
import com.leclowndu93150.thaumaturge.api.recipe.ResearchGate;
import com.leclowndu93150.thaumaturge.content.recipe.workbench.ArcaneCraftingRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.*;
import dev.latvian.mods.kubejs.recipe.schema.*;
import net.minecraft.resources.Identifier;

public final class ThaumaturgeRecipes {
    public static final NativeRecipeComponent<AspectList> ASPECTS = new NativeRecipeComponent<>("aspects", AspectList.NON_EMPTY_CODEC, true);
    public static final NativeRecipeComponent<AspectList> CRYSTALS = new NativeRecipeComponent<>("crystals", ArcaneCraftingRecipe.PRIMAL_ASPECTS_CODEC, true);
    public static final NativeRecipeComponent<ResearchGate> GATE = new NativeRecipeComponent<>("research_gate", ResearchGate.CODEC, false);
    public static final NativeRecipeComponent<InfusionEnchantment> ENCHANTMENT = new NativeRecipeComponent<>("infusion_enchantment", InfusionEnchantment.CODEC, false);
    public static final KubeRecipeFactory FACTORY = new KubeRecipeFactory(Identifier.parse("kubethaum:recipe"), ThaumaturgeRecipe.class, ThaumaturgeRecipe::new);
    private ThaumaturgeRecipes() {}

    private static RecipeKey<?> result() { return ItemStackComponent.ITEM_STACK.outputKey("result"); }
    private static RecipeKey<?> catalyst() { return IngredientComponent.INGREDIENT.inputKey("catalyst"); }
    private static RecipeKey<?> components() { return IngredientComponent.INGREDIENT.asList().inputKey("components"); }
    private static RecipeKey<?> aspects() { return ASPECTS.otherKey("aspects"); }
    private static RecipeKey<?> research() { return GATE.otherKey("research").defaultOptional(); }
    private static RecipeKey<?> crystals() { return CRYSTALS.otherKey("crystals").optional(AspectList.EMPTY).alwaysWrite(); }
    private static RecipeKey<?> vis() { return NumberComponent.POSITIVE_INT.otherKey("vis"); }
    private static void register(RecipeSchemaRegistry registry, String name, RecipeKey<?>... keys) {
        registry.register(Identifier.fromNamespaceAndPath("thaumaturge", name), new RecipeSchema(keys).factory(FACTORY));
    }

    public static void register(RecipeSchemaRegistry registry) {
        register(registry, "crucible", result(), catalyst(), aspects(), research());
        // Result is optional for native catalyst-patch recipes; only the fixed-result constructor is exposed.
        var output = ItemStackComponent.ITEM_STACK.outputKey("result").defaultOptional();
        var input = catalyst(); var ingredients = components(); var cost = aspects();
        registry.register(Identifier.parse("thaumaturge:infusion"), new RecipeSchema(input, ingredients, cost,
            output, NumberComponent.intRange(0, 100).otherKey("instability").optional(0), research())
            .constructor(output, input, ingredients, cost).factory(FACTORY));
        register(registry, "infusion_enchantment", ENCHANTMENT.otherKey("enchantment"),
            IngredientComponent.INGREDIENT.inputKey("display_catalyst"), components(), aspects(), research());
        register(registry, "arcane_workbench_shaped", result(), StringComponent.STRING.asList().otherKey("pattern"),
            IngredientComponent.INGREDIENT.asMap(StringComponent.STRING).inputKey("key"), vis(), crystals(), research());
        register(registry, "arcane_workbench_shapeless", result(), IngredientComponent.INGREDIENT.asList().inputKey("ingredients"),
            vis(), crystals(), research());
        register(registry, "dust_trigger_simple", result(), BlockComponent.BLOCK.inputKey("target"), research());
        register(registry, "dust_trigger_tag", result(), StringComponent.ID.otherKey("target_tag"), research());
        register(registry, "dust_trigger_multiblock", result(), StringComponent.ID.otherKey("blueprint"), research());
    }
}
