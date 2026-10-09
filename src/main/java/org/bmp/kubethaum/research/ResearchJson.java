package org.bmp.kubethaum.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public final class ResearchJson {
    private ResearchJson() {}

    public static String id(String value) {
        if (value == null || !value.contains(":") || Identifier.tryParse(value) == null) {
            throw new IllegalArgumentException("Expected a namespaced identifier, got '" + value + "'");
        }
        return value;
    }

    public static String text(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Text must not be blank");
        return value;
    }

    public static int positive(int value, String field) {
        if (value < 1) throw new IllegalArgumentException(field + " must be positive");
        return value;
    }

    public static int nonNegative(int value, String field) {
        if (value < 0) throw new IllegalArgumentException(field + " must not be negative");
        return value;
    }

    public static JsonArray array(JsonObject json, String field) {
        if (!json.has(field)) json.add(field, new JsonArray());
        return json.getAsJsonArray(field);
    }

    public static void addUnique(JsonObject json, String field, String value) {
        var array = array(json, field);
        for (var old : array) if (old.getAsString().equals(value)) return;
        array.add(value);
    }

    public static void aspect(JsonObject json, String field, String aspect, int amount) {
        id(aspect);
        positive(amount, "Aspect amount");
        var array = array(json, field);
        for (var value : array) {
            var entry = value.getAsJsonObject();
            if (entry.get("aspect").getAsString().equals(aspect)) {
                entry.addProperty("amount", amount);
                return;
            }
        }
        var entry = new JsonObject();
        entry.addProperty("aspect", aspect);
        entry.addProperty("amount", amount);
        array.add(entry);
    }

    public static JsonObject requirement(String[] items, int amount, JsonObject components) {
        if (items.length == 0) throw new IllegalArgumentException("A requirement needs at least one item or tag");
        var json = new JsonObject();
        if (items.length == 1 && items[0].startsWith("#")) {
            json.addProperty("items", "#" + id(items[0].substring(1)));
        } else {
            var ids = new JsonArray();
            for (String item : items) ids.add(id(item));
            json.add("items", ids);
        }
        json.addProperty("amount", positive(amount, "Item amount"));
        if (components != null) json.add("components", components.deepCopy());
        return json;
    }

    public static JsonObject knowledge(String type, String category, int amount) {
        if (!type.equals("theory") && !type.equals("observation")) {
            throw new IllegalArgumentException("Knowledge type must be 'theory' or 'observation'");
        }
        var json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("category", id(category));
        json.addProperty("amount", positive(amount, "Knowledge amount"));
        return json;
    }

    public static JsonObject gate(String entry, Integer stage, boolean negate) {
        var json = new JsonObject();
        json.addProperty("entry", id(entry));
        if (stage != null) json.addProperty("stage", nonNegative(stage, "Gate stage"));
        if (negate) json.addProperty("negate", true);
        return json;
    }

    public static Parent parent(String value) {
        String body = value.startsWith("~") ? value.substring(1) : value;
        int at = body.lastIndexOf('@');
        int stage = 0;
        if (at >= 0) {
            try { stage = positive(Integer.parseInt(body.substring(at + 1)), "Parent stage"); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid parent stage: " + value, ex); }
            body = body.substring(0, at);
        }
        return new Parent(id(body), stage);
    }

    public record Parent(String id, int stage) {}
}
