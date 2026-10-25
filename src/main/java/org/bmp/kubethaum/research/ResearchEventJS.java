package org.bmp.kubethaum.research;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.util.HideFromJS;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** One isolated transaction per world load; never stores builders in global state. */
public final class ResearchEventJS implements KubeEvent {
    private final Map<String, JsonObject> originalEntries;
    private final Map<String, JsonObject> originalCategories;
    private final Map<String, ResearchBuilder> entries = new LinkedHashMap<>();
    private final Map<String, CategoryBuilder> categories = new LinkedHashMap<>();
    private final ProgressFlags progressFlags;
    private Throwable scriptError;

    public ResearchEventJS(Map<String, JsonObject> entries, Map<String, JsonObject> categories) {
        this(entries, categories, ProgressFlags.EVENTS_ONLY);
    }
    public ResearchEventJS(Map<String, JsonObject> entries, Map<String, JsonObject> categories, ProgressFlags progressFlags) {
        this.originalEntries = entries;
        this.originalCategories = categories;
        this.progressFlags = progressFlags;
    }

    public ResearchBuilder create(String id) {
        ResearchJson.id(id);
        if (originalEntries.containsKey(id) || entries.containsKey(id)) {
            throw new IllegalArgumentException("Research '" + id + "' already exists; use edit() or replace()");
        }
        return replace(id);
    }
    public ResearchBuilder replace(String id) {
        var builder = new ResearchBuilder(id);
        entries.put(id, builder);
        return builder;
    }
    public ResearchBuilder edit(String id) {
        ResearchJson.id(id);
        var existing = entries.get(id);
        if (existing != null) return existing;
        var builder = new ResearchBuilder(id, require(originalEntries, id, "research").deepCopy());
        entries.put(id, builder);
        return builder;
    }
    public void edit(String id, Consumer<ResearchBuilder> callback) { callback.accept(edit(id)); }
    /** Copies the current effective record, including earlier handlers' edits, without rewriting links. */
    public ResearchBuilder copy(String sourceId, String newId) {
        ResearchJson.id(newId);
        if (exists(newId)) throw new IllegalArgumentException("Research '" + newId + "' already exists; copy() needs a new ID");
        var builder = new ResearchBuilder(newId, get(sourceId));
        entries.put(newId, builder);
        return builder;
    }
    public CategoryBuilder category(String id) {
        ResearchJson.id(id);
        if (originalCategories.containsKey(id) || categories.containsKey(id)) {
            throw new IllegalArgumentException("Category '" + id + "' already exists; use editCategory()");
        }
        var builder = new CategoryBuilder(CategoryBuilder.defaults(), this::getCategory, id);
        categories.put(id, builder);
        return builder;
    }
    public CategoryBuilder editCategory(String id) {
        ResearchJson.id(id);
        return categories.computeIfAbsent(id, key -> new CategoryBuilder(
            require(originalCategories, key, "category").deepCopy(), this::getCategory, key));
    }
    public JsonObject get(String id) {
        return entries.containsKey(id) ? entries.get(id).build() : require(originalEntries, ResearchJson.id(id), "research").deepCopy();
    }
    public JsonObject getCategory(String id) {
        return categories.containsKey(id) ? categories.get(id).build() : require(originalCategories, ResearchJson.id(id), "category").deepCopy();
    }
    public boolean exists(String id) { return entries.containsKey(id) || originalEntries.containsKey(id); }

    private static JsonObject require(Map<String, JsonObject> map, String id, String kind) {
        var data = map.get(id);
        if (data == null) throw new IllegalArgumentException("Unknown " + kind + " '" + id + "'");
        return data;
    }
    @HideFromJS public void scriptFailed(Throwable ex) { scriptError = ex; }
    @HideFromJS public Changes finish() {
        if (scriptError != null) throw new IllegalArgumentException("KubeThaum research script failed; see kubejs/logs/server.log", scriptError);
        var modifiedEntries = new LinkedHashMap<String, JsonObject>();
        var modifiedCategories = new LinkedHashMap<String, JsonObject>();
        entries.forEach((id, builder) -> modifiedEntries.put(id, builder.build()));
        categories.forEach((id, builder) -> modifiedCategories.put(id, builder.build()));
        var allEntries = new LinkedHashMap<>(originalEntries);
        var allCategories = new LinkedHashMap<>(originalCategories);
        allEntries.putAll(modifiedEntries);
        allCategories.putAll(modifiedCategories);
        var usedFlags = ResearchValidator.progressFlags(originalEntries);
        ResearchValidator.validate(allEntries, allCategories, modifiedEntries.keySet(), modifiedCategories.keySet(),
            id -> usedFlags.contains(id) || progressFlags.accepts(id));
        int changedResearch = (int)modifiedEntries.keySet().stream().filter(originalEntries::containsKey).count();
        int changedTabs = (int)modifiedCategories.keySet().stream().filter(originalCategories::containsKey).count();
        var summary = new Summary(modifiedEntries.size() - changedResearch, changedResearch,
            modifiedCategories.size() - changedTabs, changedTabs);
        return new Changes(Map.copyOf(modifiedEntries), Map.copyOf(modifiedCategories), summary);
    }
    public record Changes(Map<String, JsonObject> entries, Map<String, JsonObject> categories, Summary summary) {}
    public record Summary(int addedResearch, int changedResearch, int addedCategories, int changedCategories) {}
}
