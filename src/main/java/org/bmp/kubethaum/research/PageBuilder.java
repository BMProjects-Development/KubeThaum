package org.bmp.kubethaum.research;

import com.google.gson.JsonObject;
import dev.latvian.mods.rhino.util.HideFromJS;

public abstract class PageBuilder<T extends PageBuilder<T>> {
    protected final JsonObject data;
    protected final String context;

    protected PageBuilder(JsonObject data) { this(data, "Research page"); }
    protected PageBuilder(JsonObject data, String context) { this.data = data; this.context = context; }
    protected T field(String field, Runnable action) {
        ResearchDiagnostics.at(context + ", field '" + field + "'", action); return self();
    }

    protected abstract T self();

    public T text(String textOrTranslationKey) {
        return field("text", () -> data.addProperty("text", ResearchJson.text(textOrTranslationKey)));
    }

    public T recipe(String id) {
        return field("recipes", () -> ResearchJson.addUnique(data, "recipes", ResearchJson.id(id)));
    }

    public T requireResearch(String id) {
        return field("required_research", () -> ResearchJson.addUnique(data, "required_research", ResearchJson.id(id)));
    }

    @HideFromJS
    public JsonObject build() {
        field("text", () -> {
            if (!data.has("text")) throw new IllegalArgumentException("A research page needs text()");
            ResearchJson.text(data.get("text").getAsString());
        });
        return data.deepCopy();
    }
}
