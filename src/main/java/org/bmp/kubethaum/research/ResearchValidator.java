package org.bmp.kubethaum.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Checks references the native codecs represent as IDs rather than resolved holders. */
public final class ResearchValidator {
    private ResearchValidator() {}

    public static void validate(Map<String, JsonObject> entries, Map<String, JsonObject> categories,
                                Set<String> changedEntries, Set<String> changedCategories) {
        for (String id : changedEntries) {
            var entry = entries.get(id);
            known(categories, entry.get("category").getAsString(), id + " category");
            ResearchJson.text(entry.get("name").getAsString());
            var stages = entry.getAsJsonArray("stages");
            if (stages == null || stages.isEmpty()) fail(id, "needs at least one stage");
            for (var value : array(entry, "parents")) {
                var parent = ResearchJson.parent(value.getAsString());
                known(entries, parent.id(), id + " parent");
                int stageCount = entries.get(parent.id()).getAsJsonArray("stages").size();
                if (parent.stage() > stageCount) fail(id, "parent " + value + " exceeds its " + stageCount + " stages");
            }
            references(entries, entry, "siblings", id);
            for (var value : stages) {
                var stage = value.getAsJsonObject();
                ResearchJson.text(stage.get("text").getAsString());
                references(entries, stage, "required_research", id);
                for (String field : new String[]{"knowledge", "required_knowledge"}) {
                    for (var reward : array(stage, field)) {
                        var json = reward.getAsJsonObject();
                        known(categories, json.get("category").getAsString(), id + " " + field);
                        ResearchJson.positive(json.get("amount").getAsInt(), "Knowledge amount");
                    }
                }
            }
            for (var page : array(entry, "addenda")) references(entries, page.getAsJsonObject(), "required_research", id);
        }
        for (String id : changedCategories) {
            var category = categories.get(id);
            if (category.has("required_research")) known(entries, category.get("required_research").getAsString(), id + " required research");
        }
        // Visit the complete graph from edited nodes, so a cycle through a vanilla node is detected.
        // Stage-qualified parents can intentionally form reciprocal links and are excluded.
        var done = new HashSet<String>();
        for (String id : changedEntries) visit(id, entries, categories, done, new LinkedHashSet<>());
        for (String id : changedCategories) {
            for (var entry : entries.entrySet()) {
                if (entry.getValue().get("category").getAsString().equals(id)) visit(entry.getKey(), entries, categories, done, new LinkedHashSet<>());
            }
        }
    }

    private static void visit(String id, Map<String, JsonObject> entries, Map<String, JsonObject> categories,
                              Set<String> done, LinkedHashSet<String> path) {
        if (done.contains(id) || !entries.containsKey(id)) return;
        if (!path.add(id)) fail(id, "completion dependency cycle: " + String.join(" -> ", path) + " -> " + id);
        for (String target : dependencies(entries.get(id), categories)) visit(target, entries, categories, done, path);
        path.remove(id);
        done.add(id);
    }
    private static List<String> dependencies(JsonObject entry, Map<String, JsonObject> categories) {
        var refs = new ArrayList<String>();
        for (var parent : array(entry, "parents")) {
            var parsed = ResearchJson.parent(parent.getAsString());
            if (parsed.stage() == 0) refs.add(parsed.id());
        }
        for (var stage : array(entry, "stages")) {
            for (var research : array(stage.getAsJsonObject(), "required_research")) refs.add(research.getAsString());
        }
        var category = categories.get(entry.get("category").getAsString());
        if (category != null && category.has("required_research")) refs.add(category.get("required_research").getAsString());
        return refs;
    }
    private static void references(Map<String, JsonObject> entries, JsonObject json, String field, String owner) {
        for (var ref : array(json, field)) known(entries, ref.getAsString(), owner + " " + field);
    }
    private static void known(Map<String, JsonObject> entries, String id, String context) {
        ResearchJson.id(id);
        if (!entries.containsKey(id)) throw new IllegalArgumentException(context + ": unknown ID '" + id + "'");
    }
    private static JsonArray array(JsonObject json, String field) { return json.has(field) ? json.getAsJsonArray(field) : new JsonArray(); }
    private static void fail(String id, String message) { throw new IllegalArgumentException("Research '" + id + "': " + message); }
}
