package org.bmp.kubethaum;

import com.leclowndu93150.thaumaturge.api.recipe.ResearchGated;
import com.leclowndu93150.thaumaturge.content.infernalfurnace.InfernalBonus;
import com.leclowndu93150.thaumaturge.content.recipe.crucible.CrucibleRecipe;
import com.leclowndu93150.thaumaturge.content.recipe.workbench.ArcaneCraftingRecipe;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionRecipe;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionInput;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionRunicAugmentRecipe;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

final class RecipeSmoke {
    static void verify(MinecraftServer server) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        for (var id : new String[]{"gold_in_crucible", "gold_at_arcane_workbench", "gold_from_redstone", "diamond_infusion", "swift_boots", "runic_shielding", "gilded_iron", "gilded_logs", "infernal_furnace"}) {
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
        require(edited.matches(input, server.overworld()) && edited.assemble(input).is(Items.DIAMOND), "native crucible/Thaumatorium matching and output");
        require(!edited.matches(new com.leclowndu93150.thaumaturge.content.recipe.crucible.CrucibleRecipeInput(
            new ItemStack(Items.COPPER_INGOT), com.leclowndu93150.thaumaturge.api.aspect.AspectList.EMPTY), server.overworld()), "native aspect requirement");
        require(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.parse("kubethaum:recipe_removed"))).isEmpty(), "remove");
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
        var runic = (InfusionRunicAugmentRecipe) get(server, "kubejs:runic_shielding");
        require(runic.researchGate().orElseThrow().entry().toString().equals("thaumaturge:runic_shielding"), "runic augmentation research gate");
        var chestplate = new ItemStack(Items.IRON_CHESTPLATE);
        var first = new InfusionInput(chestplate, List.of(new ItemStack(Items.DIAMOND)));
        require(runic.matches(first, server.overworld()), "native runic augmentation matches uncharged armor");
        var charged = runic.assemble(first);
        require(InfusionRunicAugmentRecipe.charge(charged) == 1 && InfusionRunicAugmentRecipe.charge(chestplate) == 0,
            "native runic augmentation preserves input and adds charge");
        var amber = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.parse("thaumaturge:amber"));
        require(!runic.matches(new InfusionInput(charged, first.components()), server.overworld())
            && runic.matches(new InfusionInput(charged, List.of(new ItemStack(Items.DIAMOND), new ItemStack(amber))), server.overworld()),
            "native runic per-level ingredient scaling");
        require(runic.scaledAspects(charged).entries().getFirst().amount() == 12, "native runic essentia cost scaling");
        var runicEdit = (InfusionRunicAugmentRecipe) get(server, "kubethaum:runic_edit");
        require(runicEdit.matches(new InfusionInput(charged, List.of(new ItemStack(Items.LAPIS_LAZULI), new ItemStack(Items.REDSTONE))), server.overworld())
            && !runicEdit.matches(new InfusionInput(charged, List.of(new ItemStack(Items.DIAMOND), new ItemStack(amber))), server.overworld()),
            "runic base and per-level ingredient edits affect native matching");
        require(runicEdit.scaledAspects(chestplate).entries().getFirst().amount() == 6
            && runicEdit.researchGate().orElseThrow().entry().toString().equals("kubejs:basic_metallurgy"), "runic aspect and research edits");
        Recipe.CODEC.parse(ops, Recipe.CODEC.encodeStart(ops, runicEdit).getOrThrow()).getOrThrow();
        System.out.println("KubeThaum: recipes and furnace data verified against Thaumaturge 1.1");
    }
    private static Recipe<?> get(MinecraftServer server, String id) {
        return server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.parse(id))).orElseThrow().value();
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
