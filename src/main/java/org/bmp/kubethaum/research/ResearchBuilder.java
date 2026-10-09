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
    public ResearchBuilder category(String id) { return field("category", () -> data.addProperty("category", ResearchJson.id(id))); }
    public ResearchBuilder title(String value) { return field("name", () -> data.addProperty("name", ResearchJson.text(value))); }
    public ResearchBuilder position(int column, int row) {
        data.addProperty("column", column); data.addProperty("row", row); return this;
    }
    public ResearchBuilder icon(String value) {
        return field("icons", () -> {
            ResearchJson.id(value.startsWith("focus:") ? value.substring(6) : value);
            ResearchJson.addUnique(data, "icons", value);
        });
    }
    public ResearchBuilder parent(String value) {
        return field("parents", () -> { ResearchJson.parent(value); ResearchJson.addUnique(data, "parents", value); });
    }
    public ResearchBuilder parent(String id, int stage) {
        return field("parents", () -> parent(ResearchJson.id(id) + "@" + ResearchJson.positive(stage, "Parent stage (starts at 1)")));
    }
    public ResearchBuilder hiddenParent(String id) { return field("parents", () -> parent("~" + ResearchJson.id(id))); }
    public ResearchBuilder sibling(String id) {
        return field("siblings", () -> ResearchJson.addUnique(data, "siblings", ResearchJson.id(id)));
    }
    public ResearchBuilder flag(String flag) {
        return field("meta", () -> {
            if (!FLAGS.contains(flag)) throw new IllegalArgumentException("Unknown research flag: " + flag);
            ResearchJson.addUnique(data, "meta", flag);
        });
    }
    public ResearchBuilder autoUnlock() { return flag("autounlock"); }
    public ResearchBuilder noteAspect(String id, int amount) {
        return field("note_aspects", () -> ResearchJson.aspect(data, "note_aspects", id, amount));
    }
    public ResearchBuilder complexity(int value) {
        return field("complexity", () -> {
            if (value < 1 || value > 3) throw new IllegalArgumentException("Complexity must be 1, 2 or 3");
            data.addProperty("complexity", value);
        });
    }
    public ResearchBuilder stage(Consumer<StageBuilder> callback) {
        var stages = ResearchJson.array(data, "stages");
        stages.add(stageData(stages.size(), new JsonObject(), callback)); return this;
    }
    public ResearchBuilder editStage(int index, Consumer<StageBuilder> callback) {
        var stages = ResearchJson.array(data, "stages");
        field("stages", () -> checkIndex(index, stages.size()));
        stages.set(index, stageData(index, stages.get(index).getAsJsonObject().deepCopy(), callback)); return this;
    }
    public ResearchBuilder insertStage(int index, Consumer<StageBuilder> callback) {
        var stages = ResearchJson.array(data, "stages");
        field("stages", () -> {
            if (index < 0 || index > stages.size()) throw new IllegalArgumentException("Stage insertion index out of range: " + index);
        });
        stages.asList().add(index, stageData(index, new JsonObject(), callback)); return this;
    }
    public ResearchBuilder removeStage(int index) {
        var stages = ResearchJson.array(data, "stages"); field("stages", () -> checkIndex(index, stages.size()));
        stages.remove(index); return this;
    }
    public ResearchBuilder addendum(Consumer<AddendumBuilder> callback) {
        var pages = ResearchJson.array(data, "addenda");
        String context = "Research '" + id + "', addendum[" + pages.size() + "]";
        pages.add(ResearchDiagnostics.at(context, () -> {
            var page = new AddendumBuilder(context); callback.accept(page); return page.build();
        })); return this;
    }
    public ResearchBuilder clearStages() { data.remove("stages"); return this; }
    public ResearchBuilder clearParents() { data.remove("parents"); return this; }
    public ResearchBuilder clearIcons() { data.remove("icons"); return this; }
    public ResearchBuilder clearAddenda() { data.remove("addenda"); return this; }
    private ResearchBuilder field(String field, Runnable action) {
        ResearchDiagnostics.at("Research '" + id + "', field '" + field + "'", action); return this;
    }
    private JsonObject stageData(int index, JsonObject source, Consumer<StageBuilder> callback) {
        String context = "Research '" + id + "', stage[" + index + "]";
        return ResearchDiagnostics.at(context, () -> {
            var stage = new StageBuilder(source, context); callback.accept(stage); return stage.build();
        });
    }
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
