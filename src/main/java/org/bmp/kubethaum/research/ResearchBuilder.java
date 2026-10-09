package org.bmp.kubethaum.research;

import com.google.gson.JsonObject;
import dev.latvian.mods.rhino.util.HideFromJS;
import java.util.Set;
import java.util.function.Consumer;

public final class ResearchBuilder {
    private static final Set<String> FLAGS = Set.of("round", "spiky", "reverse", "hidden", "autounlock", "hex");
    private final String id;
    private final JsonObject data;

    public ResearchBuilder(String id) {
        this(id, new JsonObject());
        category("thaumaturge:basics");
        title("research." + id.replace(':', '.').replace('/', '.'));
        position(0, 0);
    }
    public ResearchBuilder(String id, JsonObject data) {
        this.id = ResearchJson.id(id);
        this.data = data;
    }
    public ResearchBuilder category(String id) { data.addProperty("category", ResearchJson.id(id)); return this; }
    public ResearchBuilder title(String value) { data.addProperty("name", ResearchJson.text(value)); return this; }
    public ResearchBuilder position(int column, int row) {
        data.addProperty("column", column); data.addProperty("row", row); return this;
    }
    public ResearchBuilder icon(String value) {
        ResearchJson.id(value.startsWith("focus:") ? value.substring(6) : value);
        ResearchJson.addUnique(data, "icons", value); return this;
    }
    public ResearchBuilder parent(String value) {
        ResearchJson.parent(value); ResearchJson.addUnique(data, "parents", value); return this;
    }
    public ResearchBuilder parent(String id, int stage) {
        return parent(ResearchJson.id(id) + "@" + ResearchJson.positive(stage, "Parent stage"));
    }
    public ResearchBuilder hiddenParent(String id) { return parent("~" + ResearchJson.id(id)); }
    public ResearchBuilder sibling(String id) {
        ResearchJson.addUnique(data, "siblings", ResearchJson.id(id)); return this;
    }
    public ResearchBuilder flag(String flag) {
        if (!FLAGS.contains(flag)) throw new IllegalArgumentException("Unknown research flag: " + flag);
        ResearchJson.addUnique(data, "meta", flag); return this;
    }
    public ResearchBuilder autoUnlock() { return flag("autounlock"); }
    public ResearchBuilder noteAspect(String id, int amount) {
        ResearchJson.aspect(data, "note_aspects", id, amount); return this;
    }
    public ResearchBuilder complexity(int value) {
        if (value < 1 || value > 3) throw new IllegalArgumentException("Complexity must be 1, 2 or 3");
        data.addProperty("complexity", value); return this;
    }
    public ResearchBuilder stage(Consumer<StageBuilder> callback) {
        var stage = new StageBuilder(); callback.accept(stage);
        ResearchJson.array(data, "stages").add(stage.build()); return this;
    }
    public ResearchBuilder editStage(int index, Consumer<StageBuilder> callback) {
        var stages = ResearchJson.array(data, "stages");
        checkIndex(index, stages.size());
        var stage = new StageBuilder(stages.get(index).getAsJsonObject().deepCopy());
        callback.accept(stage); stages.set(index, stage.build()); return this;
    }
    public ResearchBuilder insertStage(int index, Consumer<StageBuilder> callback) {
        var stages = ResearchJson.array(data, "stages");
        if (index < 0 || index > stages.size()) throw new IllegalArgumentException("Stage insertion index out of range: " + index);
        var stage = new StageBuilder(); callback.accept(stage);
        var list = stages.asList(); list.add(index, stage.build()); return this;
    }
    public ResearchBuilder removeStage(int index) {
        var stages = ResearchJson.array(data, "stages"); checkIndex(index, stages.size());
        stages.remove(index); return this;
    }
    public ResearchBuilder addendum(Consumer<AddendumBuilder> callback) {
        var page = new AddendumBuilder(); callback.accept(page);
        ResearchJson.array(data, "addenda").add(page.build()); return this;
    }
    public ResearchBuilder clearStages() { data.remove("stages"); return this; }
    public ResearchBuilder clearParents() { data.remove("parents"); return this; }
    public ResearchBuilder clearIcons() { data.remove("icons"); return this; }
    public ResearchBuilder clearAddenda() { data.remove("addenda"); return this; }
    private static void checkIndex(int index, int size) {
        if (index < 0 || index >= size) throw new IllegalArgumentException("Stage index out of range: " + index + " (count " + size + ")");
    }
    @HideFromJS public JsonObject build() {
        if (!data.has("stages") || data.getAsJsonArray("stages").isEmpty()) {
            throw new IllegalArgumentException("Research '" + id + "' needs at least one stage");
        }
        return data.deepCopy();
    }
}
