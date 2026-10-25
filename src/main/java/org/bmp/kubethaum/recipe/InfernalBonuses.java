package org.bmp.kubethaum.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.bmp.kubethaum.research.ResearchJson;

/** Generate native data-map JSON before tags load; NeoForge resolves tags during data-map loading. */
public final class InfernalBonuses {
    private static final ResourceLocation FILE = ResourceLocation.parse("thaumaturge:data_maps/item/infernal_bonus.json");
    private InfernalBonuses() {}

    public static void write(KubeDataGenerator data, String input, String output, int count, float chance, boolean replace) {
        checkItemOrTag(input);
        checkItemOrTag(output);
        if (count < 1 || count > 64) throw new IllegalArgumentException("Infernal bonus count must be between 1 and 64");
        if (!Float.isFinite(chance) || chance < 0 || chance > 1) throw new IllegalArgumentException("Infernal bonus chance must be between 0 and 1");
        var file = read(data);
        var values = file.getAsJsonObject("values");
        var previous = values.get(input);
        var bonuses = new JsonArray();
        boolean replaceEntry = replace;
        if (!replace && previous != null) {
            if (previous.isJsonArray()) bonuses = previous.getAsJsonArray();
            else {
                bonuses = previous.getAsJsonObject().getAsJsonArray("value");
                replaceEntry = previous.getAsJsonObject().has("replace") && previous.getAsJsonObject().get("replace").getAsBoolean();
            }
        }
        if (bonuses.size() >= 64) throw new IllegalArgumentException("Infernal bonus list supports up to 64 entries per input");
        var bonus = new JsonObject();
        bonus.addProperty("items", output);
        bonus.addProperty("count", count);
        bonus.addProperty("chance", chance);
        bonuses.add(bonus);
        if (replaceEntry) {
            var entry = new JsonObject();
            entry.addProperty("replace", true);
            entry.add("value", bonuses);
            values.add(input, entry);
        } else values.add(input, bonuses);
        clearRemoval(file, input);
        data.json(FILE, file);
    }

    public static void remove(KubeDataGenerator data, String input) {
        checkItemOrTag(input);
        var file = read(data);
        file.getAsJsonObject("values").remove(input);
        clearRemoval(file, input);
        ResearchJson.array(file, "remove").add(input);
        data.json(FILE, file);
    }

    private static JsonObject read(KubeDataGenerator data) {
        var previous = data.getGenerated(FILE);
        if (previous != null) return JsonParser.parseString(new String(previous.data().get(), StandardCharsets.UTF_8)).getAsJsonObject();
        var file = new JsonObject();
        file.add("values", new JsonObject());
        return file;
    }

    private static void clearRemoval(JsonObject file, String input) {
        var removals = ResearchJson.array(file, "remove");
        for (int i = removals.size() - 1; i >= 0; i--) {
            var value = removals.get(i);
            String key = value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get("key").getAsString();
            if (key.equals(input)) removals.remove(i);
        }
    }

    private static void checkItemOrTag(String value) {
        if (value == null) throw new IllegalArgumentException("Expected an item ID or tag");
        boolean tag = value.startsWith("#");
        String id = ResearchJson.id(tag ? value.substring(1) : value);
        if (!tag && !BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id))) throw new IllegalArgumentException("Unknown item '" + id + "'");
    }
}
