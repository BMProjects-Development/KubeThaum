package org.bmp.kubethaum;

import com.leclowndu93150.thaumaturge.api.recipe.ResearchGated;
import com.leclowndu93150.thaumaturge.content.infernalfurnace.InfernalBonus;
import com.leclowndu93150.thaumaturge.content.recipe.crucible.CrucibleRecipe;
import com.leclowndu93150.thaumaturge.content.recipe.workbench.ArcaneCraftingRecipe;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionRecipe;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

final class RecipeSmoke {
    static void verify(MinecraftServer server) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        for (var id : new String[]{"gold_in_crucible", "gold_at_arcane_workbench", "gold_from_redstone", "diamond_infusion", "swift_boots", "gilded_iron", "gilded_logs", "infernal_furnace"}) {
            var recipe = get(server, "kubejs:" + id);
            var json = Recipe.CODEC.encodeStart(ops, recipe).getOrThrow();
            Recipe.CODEC.parse(ops, json).getOrThrow();
        }
        var edited = (CrucibleRecipe) get(server, "kubethaum:recipe_edit");
        var json = Recipe.CODEC.encodeStart(ops, edited).getOrThrow().getAsJsonObject();
        require(json.getAsJsonArray("aspects").get(0).getAsJsonObject().get("amount").getAsInt() == 8, "aspect scaling and rounding");
        require(edited.catalyst().test(new ItemStack(Items.COPPER_INGOT)), "replaceInput catalyst");
        require(json.getAsJsonObject("result").get("id").getAsString().equals("minecraft:diamond"), "replaceOutput");
        require(((ResearchGated)edited).researchGate().orElseThrow().entry().toString().equals("kubejs:basic_metallurgy"), "research survives edits");
        var input = new com.leclowndu93150.thaumaturge.content.recipe.crucible.CrucibleRecipeInput(
            new ItemStack(Items.COPPER_INGOT), edited.aspects());
        require(edited.matches(input, server.overworld()) && edited.assemble(input, server.registryAccess()).is(Items.DIAMOND), "native crucible/Thaumatorium matching and output");
        require(!edited.matches(new com.leclowndu93150.thaumaturge.content.recipe.crucible.CrucibleRecipeInput(
            new ItemStack(Items.COPPER_INGOT), com.leclowndu93150.thaumaturge.api.aspect.AspectList.EMPTY), server.overworld()), "native aspect requirement");
        require(server.getRecipeManager().byKey(ResourceLocation.parse("kubethaum:recipe_removed")).isEmpty(), "remove");
        var infusion = (InfusionRecipe) get(server, "kubethaum:infusion_edit");
        var infusionJson = Recipe.CODEC.encodeStart(ops, infusion).getOrThrow().getAsJsonObject();
        require(infusionJson.get("instability").getAsInt() == 2 && infusionJson.getAsJsonArray("components").toString().contains("lapis_lazuli"), "infusion edits");
        var arcane = (ArcaneCraftingRecipe) get(server, "kubejs:gold_from_redstone");
        var arcaneJson = Recipe.CODEC.encodeStart(ops, arcane).getOrThrow().getAsJsonObject();
        require(arcaneJson.getAsJsonArray("crystals").isEmpty(), "default empty crystals");
        var patch = Recipe.CODEC.encodeStart(ops, get(server, "kubethaum:infusion_patch")).getOrThrow().getAsJsonObject();
        require(patch.has("catalyst_patch") && !patch.has("result")
            && patch.getAsJsonArray("aspects").get(0).getAsJsonObject().get("amount").getAsInt() == 4, "native catalyst patch survives editing");
        var shaped = Recipe.CODEC.encodeStart(ops, get(server, "kubethaum:shaped_edit")).getOrThrow().getAsJsonObject();
        require(shaped.getAsJsonObject("key").toString().contains("copper_ingot") && shaped.get("vis").getAsInt() == 30
            && shaped.getAsJsonArray("crystals").get(0).getAsJsonObject().get("amount").getAsInt() == 4, "shaped map replacement, vis and crystals");
        var bonuses = Items.COBBLESTONE.builtInRegistryHolder().getData(InfernalBonus.DATA_MAP);
        require(bonuses != null && bonuses.size() == 2 && bonuses.getFirst().items().contains(Items.GOLD_NUGGET.builtInRegistryHolder())
            && bonuses.getFirst().count().sample(net.minecraft.util.RandomSource.create()) == 3 && bonuses.getFirst().chance() == 0.75f
            && bonuses.get(1).items().contains(Items.IRON_NUGGET.builtInRegistryHolder()), "infernal replacement and repeated additions");
        require(Items.RAW_GOLD.builtInRegistryHolder().getData(InfernalBonus.DATA_MAP) == null, "infernal removal");
        var replaced = Items.RAW_IRON.builtInRegistryHolder().getData(InfernalBonus.DATA_MAP);
        require(replaced != null && replaced.size() == 1 && replaced.getFirst().items().contains(Items.GOLD_NUGGET.builtInRegistryHolder())
            && replaced.getFirst().chance() == 0.5f, "replace existing native furnace bonus");
        var appended = Items.RAW_COPPER.builtInRegistryHolder().getData(InfernalBonus.DATA_MAP);
        require(appended != null && appended.stream().anyMatch(bonus -> bonus.items().contains(Items.IRON_NUGGET.builtInRegistryHolder()) && bonus.chance() == 0.25f)
            && appended.stream().anyMatch(bonus -> bonus.chance() == 0.33f), "tag bonus merges with native furnace data");
        System.out.println("KubeThaum: 1.1 recipes and furnace data verified");
    }
    private static Recipe<?> get(MinecraftServer server, String id) {
        return server.getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow().value();
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
