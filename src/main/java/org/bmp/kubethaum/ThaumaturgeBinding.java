package org.bmp.kubethaum;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import java.util.Set;
import org.bmp.kubethaum.research.ResearchJson;

public final class ThaumaturgeBinding {
    private static final Set<String> GATED_RECIPES = Set.of("thaumaturge:arcane_workbench_shaped", "thaumaturge:arcane_workbench_shapeless",
        "thaumaturge:crucible", "thaumaturge:infusion", "thaumaturge:infusion_enchantment",
        "thaumaturge:dust_trigger_simple", "thaumaturge:dust_trigger_tag", "thaumaturge:dust_trigger_multiblock");
    private ThaumaturgeBinding() {}

    public static JsonObject gate(String entry) { return ResearchJson.gate(entry, null, false); }
    public static JsonObject gate(String entry, int stage) { return ResearchJson.gate(entry, stage, false); }
    public static JsonObject gate(String entry, int stage, boolean negate) { return ResearchJson.gate(entry, stage, negate); }

    public static KubeRecipe requireResearch(KubeRecipe recipe, String entry) { return requireResearch(recipe, gate(entry)); }
    public static KubeRecipe requireResearch(KubeRecipe recipe, String entry, int stage) { return requireResearch(recipe, gate(entry, stage)); }
    private static KubeRecipe requireResearch(KubeRecipe recipe, JsonObject gate) {
        String type = recipe.type.schemaType.serializerType;
        if (!GATED_RECIPES.contains(type)) throw new IllegalArgumentException("Recipe type '" + type + "' does not support Thaumaturge ResearchGate");
        var entryId = Identifier.parse(gate.get("entry").getAsString());
        var entries = recipe.type.event.ops.json().getter(IResearchEntry.REGISTRY_KEY)
            .orElseThrow(() -> new IllegalArgumentException("ResearchGate must be configured inside ServerEvents.recipes"));
        var entry = entries.get(ResourceKey.create(IResearchEntry.REGISTRY_KEY, entryId))
            .orElseThrow(() -> new IllegalArgumentException("Unknown research '" + entryId + "' in recipe gate")).value();
        if (gate.has("stage") && gate.get("stage").getAsInt() > entry.stages().size()) {
            throw new IllegalArgumentException("Recipe gate stage exceeds " + entry.stages().size() + " stages of '" + entryId + "'");
        }
        recipe.json.add("research", gate);
        recipe.save();
        return recipe;
    }
}
