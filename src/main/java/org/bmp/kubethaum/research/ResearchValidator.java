package org.bmp.kubethaum.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;

/** Checks references the native codecs represent as IDs rather than resolved holders. */
public final class ResearchValidator {
    private ResearchValidator() {}

    public static void validate(Map<String, JsonObject> entries, Map<String, JsonObject> categories,
                                Set<String> changedEntries, Set<String> changedCategories) {
        validate(entries, categories, changedEntries, changedCategories, Set.of());
    }

    /**
     * Thaumaturge also uses {@code required_research} and {@code parents} for progress flags that
     * are not entries ({@code f_onfire}, {@code m_deepdown}, {@code scanned/...}). Flags already
     * used by the loaded data are accepted there; new unknown IDs still fail, so script typos are caught.
     */
    public static Set<String> progressFlags(Map<String, JsonObject> entries) {
        var flags = new HashSet<String>();
        for (var entry : entries.values()) {
            for (var value : array(entry, "parents")) {
                String parent;
                try {
                    parent = ResearchJson.parent(value.getAsString()).id();
                } catch (IllegalArgumentException ex) {
                    continue; // Malformed in someone else's data: only edited entries are validated.
                }
                if (!entries.containsKey(parent)) flags.add(parent);
            }
            var holders = new ArrayList<JsonObject>();
            for (var stage : array(entry, "stages")) holders.add(stage.getAsJsonObject());
            for (var page : array(entry, "addenda")) holders.add(page.getAsJsonObject());
            for (var holder : holders) {
                for (var ref : array(holder, "required_research")) {
                    if (!entries.containsKey(ref.getAsString())) flags.add(ref.getAsString());
                }
            }
        }
        return flags;
    }

    public static void validate(Map<String, JsonObject> entries, Map<String, JsonObject> categories,
                                Set<String> changedEntries, Set<String> changedCategories, Set<String> progressFlags) {
        for (String id : changedEntries) {
            var entry = entries.get(id);
            known(categories, entry.get("category").getAsString(), "Research '" + id + "', field 'category'");
            ResearchJson.text(entry.get("name").getAsString());
            var stages = entry.getAsJsonArray("stages");
            if (stages == null || stages.isEmpty()) fail(id, "needs at least one stage");
            for (var value : array(entry, "parents")) {
                var parent = ResearchJson.parent(value.getAsString());
                if (progressFlags.contains(parent.id())) continue;
                known(entries, parent.id(), "Research '" + id + "', field 'parents'");
                int stageCount = entries.get(parent.id()).getAsJsonArray("stages").size();
                if (parent.stage() > stageCount) fail(id, "parent " + value + " exceeds its " + stageCount + " stages");
            }
            references(entries, entry, "siblings", id);
            for (int index = 0; index < stages.size(); index++) {
                var stage = stages.get(index).getAsJsonObject();
                String context = "Research '" + id + "', stage[" + index + "]";
                ResearchJson.text(stage.get("text").getAsString());
                requiredResearch(entries, stage, progressFlags, context);
                for (String field : new String[]{"knowledge", "required_knowledge"}) {
                    for (var reward : array(stage, field)) {
                        var json = reward.getAsJsonObject();
                        known(categories, json.get("category").getAsString(), context + ", field '" + field + ".category'");
                        ResearchDiagnostics.at(context + ", field '" + field + ".amount'",
                            () -> ResearchJson.positive(json.get("amount").getAsInt(), "Knowledge amount"));
                    }
                }
            }
            var pages = array(entry, "addenda");
            for (int index = 0; index < pages.size(); index++) requiredResearch(entries, pages.get(index).getAsJsonObject(),
                progressFlags, "Research '" + id + "', addendum[" + index + "]");
        }
        for (String id : changedCategories) {
            var category = categories.get(id);
            if (category.has("required_research")) known(entries, category.get("required_research").getAsString(), "Category '" + id + "', field 'required_research'");
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

    /** Static items are available now; tags and dynamic holders are resolved by native codecs. */
    public static void validateItems(Map<String, JsonObject> entries, Predicate<Identifier> knownItem) {
        entries.forEach((id, entry) -> {
            String owner = "Research '" + id + "'";
            var stages = array(entry, "stages");
            for (int index = 0; index < stages.size(); index++) {
                var stage = stages.get(index).getAsJsonObject();
                String context = owner + ", stage[" + index + "]";
                for (String field : new String[]{"obtain", "craft"}) {
                    var requirements = array(stage, field);
                    for (int r = 0; r < requirements.size(); r++) {
                        var items = requirements.get(r).getAsJsonObject().get("items");
                        String path = context + ", field '" + field + "[" + r + "].items'";
                        if (items.isJsonArray()) {
                            for (var item : items.getAsJsonArray()) item(item.getAsString(), path, knownItem);
                        } else item(items.getAsString(), path, knownItem);
                    }
                }
                if (stage.has("construct")) {
                    for (var cell : array(stage.getAsJsonObject("construct"), "cells")) {
                        if (!cell.getAsString().isEmpty()) item(cell.getAsString(), context + ", field 'construct.cells'", knownItem);
                    }
                }
            }
        });
    }

    private static void item(String item, String context, Predicate<Identifier> knownItem) {
        if (item.startsWith("#")) return;
        ResearchDiagnostics.at(context, () -> {
            if (!knownItem.test(Identifier.parse(ResearchJson.id(item)))) {
                throw new IllegalArgumentException("Unknown item '" + item + "'");
            }
        });
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
        for (var ref : array(json, field)) known(entries, ref.getAsString(), owner + ", field '" + field + "'");
    }
    private static void requiredResearch(Map<String, JsonObject> entries, JsonObject json, Set<String> progressFlags, String owner) {
        for (var ref : array(json, "required_research")) {
            String id = ref.getAsString();
            if (!progressFlags.contains(id)) known(entries, id, owner + ", field 'required_research'");
        }
    }
    private static void known(Map<String, JsonObject> entries, String id, String context) {
        ResearchJson.id(id);
        if (!entries.containsKey(id)) throw new IllegalArgumentException(context + ": unknown ID '" + id + "'");
    }
    private static JsonArray array(JsonObject json, String field) { return json.has(field) ? json.getAsJsonArray(field) : new JsonArray(); }
    private static void fail(String id, String message) { throw new IllegalArgumentException("Research '" + id + "': " + message); }
}
