package org.bmp.kubethaum.research;

import com.google.gson.JsonObject;
import dev.latvian.mods.rhino.util.HideFromJS;

public abstract class PageBuilder<T extends PageBuilder<T>> {
    protected final JsonObject data;

    protected PageBuilder(JsonObject data) { this.data = data; }

    protected abstract T self();

    public T text(String textOrTranslationKey) {
        data.addProperty("text", ResearchJson.text(textOrTranslationKey));
        return self();
    }

    public T recipe(String id) {
        ResearchJson.addUnique(data, "recipes", ResearchJson.id(id));
        return self();
    }

    public T requireResearch(String id) {
        ResearchJson.addUnique(data, "required_research", ResearchJson.id(id));
        return self();
    }

    @HideFromJS
    public JsonObject build() {
        if (!data.has("text")) throw new IllegalArgumentException("A research page needs text()");
        ResearchJson.text(data.get("text").getAsString());
        return data.deepCopy();
    }
}
