package org.bmp.kubethaum.research;

import com.google.gson.JsonObject;
import dev.latvian.mods.rhino.util.HideFromJS;
import java.util.function.Function;

public final class CategoryBuilder {
    private final JsonObject data;
    private final Function<String, JsonObject> categories;
    private final String context;

    public CategoryBuilder(JsonObject data, Function<String, JsonObject> categories) {
        this(data, categories, "<standalone>");
    }
    public CategoryBuilder(JsonObject data, Function<String, JsonObject> categories, String id) {
        this.data = data;
        this.categories = categories;
        this.context = "Category '" + id + "'";
    }
    public CategoryBuilder style(String category) {
        return field("style", () -> copyStyle(category));
    }
    private void copyStyle(String category) {
        var source = categories.apply(ResearchJson.id(category));
        for (String field : new String[]{"icon", "background", "overlay_background"}) {
            if (source.has(field)) data.add(field, source.get(field).deepCopy()); else data.remove(field);
        }
    }
    public CategoryBuilder icon(String texture) { return field("icon", () -> data.addProperty("icon", ResearchJson.id(texture))); }
    public CategoryBuilder background(String texture) { return field("background", () -> data.addProperty("background", ResearchJson.id(texture))); }
    public CategoryBuilder overlay(String texture) { return field("overlay_background", () -> data.addProperty("overlay_background", ResearchJson.id(texture))); }
    public CategoryBuilder noOverlay() { data.remove("overlay_background"); return this; }
    public CategoryBuilder index(int index) { return field("index", () -> data.addProperty("index", ResearchJson.nonNegative(index, "Category index"))); }
    public CategoryBuilder requireResearch(String id) { return field("required_research", () -> data.addProperty("required_research", ResearchJson.id(id))); }
    public CategoryBuilder noRequiredResearch() { data.remove("required_research"); return this; }
    public CategoryBuilder formula(String aspect, int amount) { return field("formula", () -> ResearchJson.aspect(data, "formula", aspect, amount)); }
    private CategoryBuilder field(String field, Runnable action) {
        ResearchDiagnostics.at(context + ", field '" + field + "'", action); return this;
    }
    @HideFromJS public JsonObject build() { return data.deepCopy(); }
    @HideFromJS public static JsonObject defaults() {
        var json = new JsonObject();
        json.add("formula", new com.google.gson.JsonArray());
        json.addProperty("icon", "thaumaturge:textures/item/thaumonomicon_cheat.png");
        json.addProperty("background", "thaumaturge:textures/gui/gui_research_back_1.png");
        json.addProperty("overlay_background", "thaumaturge:textures/gui/gui_research_back_over.png");
        json.addProperty("index", 100);
        return json;
    }
}
