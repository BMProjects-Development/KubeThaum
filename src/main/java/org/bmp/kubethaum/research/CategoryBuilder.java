package org.bmp.kubethaum.research;

import com.google.gson.JsonObject;
import dev.latvian.mods.rhino.util.HideFromJS;
import java.util.function.Function;

public final class CategoryBuilder {
    private final JsonObject data;
    private final Function<String, JsonObject> categories;

    public CategoryBuilder(JsonObject data, Function<String, JsonObject> categories) {
        this.data = data;
        this.categories = categories;
    }
    public CategoryBuilder style(String category) {
        var source = categories.apply(ResearchJson.id(category));
        for (String field : new String[]{"icon", "background", "overlay_background"}) {
            if (source.has(field)) data.add(field, source.get(field).deepCopy()); else data.remove(field);
        }
        return this;
    }
    public CategoryBuilder icon(String texture) { data.addProperty("icon", ResearchJson.id(texture)); return this; }
    public CategoryBuilder background(String texture) { data.addProperty("background", ResearchJson.id(texture)); return this; }
    public CategoryBuilder overlay(String texture) { data.addProperty("overlay_background", ResearchJson.id(texture)); return this; }
    public CategoryBuilder noOverlay() { data.remove("overlay_background"); return this; }
    public CategoryBuilder index(int index) { data.addProperty("index", ResearchJson.nonNegative(index, "Category index")); return this; }
    public CategoryBuilder requireResearch(String id) { data.addProperty("required_research", ResearchJson.id(id)); return this; }
    public CategoryBuilder noRequiredResearch() { data.remove("required_research"); return this; }
    public CategoryBuilder formula(String aspect, int amount) { ResearchJson.aspect(data, "formula", aspect, amount); return this; }
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
