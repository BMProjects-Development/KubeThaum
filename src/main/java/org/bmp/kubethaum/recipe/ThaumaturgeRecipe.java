package org.bmp.kubethaum.recipe;

import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import org.bmp.kubethaum.ThaumaturgeBinding;
import org.bmp.kubethaum.research.ResearchJson;

public final class ThaumaturgeRecipe extends KubeRecipe {
    public KubeRecipe research(String entry) { return ThaumaturgeBinding.requireResearch(this, entry); }
    public KubeRecipe research(String entry, int stage) { return ThaumaturgeBinding.requireResearch(this, entry, stage); }

    public ThaumaturgeRecipe aspect(String id, int amount) {
        var key = costKey();
        var costs = getValue(key);
        var json = new com.google.gson.JsonObject();
        json.add(key.name, key.codec.encodeStart(type.event.ops.json(), costs == null ? AspectList.EMPTY : costs).getOrThrow());
        ResearchJson.aspect(json, key.name, id, amount);
        setValue(key, key.codec.parse(type.event.ops.json(), json.get(key.name)).getOrThrow());
        return this;
    }

    /** Positive factors, rounded up; native codecs enforce workbench crystal limits. */
    public ThaumaturgeRecipe scaleAspects(double factor) {
        if (!Double.isFinite(factor) || factor <= 0) throw new IllegalArgumentException("Aspect multiplier must be finite and positive");
        var key = costKey();
        var costs = getValue(key);
        if (costs == null) return this;
        var json = key.codec.encodeStart(type.event.ops.json(), costs).getOrThrow().getAsJsonArray();
        for (var element : json) {
            var entry = element.getAsJsonObject();
            double amount = Math.ceil(entry.get("amount").getAsInt() * factor);
            if (amount > Integer.MAX_VALUE) throw new IllegalArgumentException("Scaled aspect amount is too large");
            entry.addProperty("amount", (int) amount);
        }
        setValue(key, key.codec.parse(type.event.ops.json(), json).getOrThrow());
        return this;
    }

    private RecipeKey<AspectList> costKey() {
        var schema = type.schemaType.schema;
        RecipeKey<AspectList> key = schema.getOptionalKey("aspects");
        if (key == null) key = schema.getOptionalKey("crystals");
        if (key == null) throw new IllegalArgumentException("Recipe '" + id + "' has no aspect cost");
        return key;
    }
}
