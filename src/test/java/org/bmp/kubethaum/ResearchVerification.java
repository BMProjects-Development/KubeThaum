package org.bmp.kubethaum;

import com.google.gson.JsonObject;
import java.util.Map;
import org.bmp.kubethaum.research.*;

/** Fast checks for progression invariants; integration and native codecs run in GameTests. */
public final class ResearchVerification {
    private static int checks;
    public static void main(String[] args) {
        var initial = research("pack:base").build();
        initial.addProperty("extra_datapack_field", "preserved");
        var categories = Map.of("thaumaturge:basics", CategoryBuilder.defaults());
        var event = new ResearchEventJS(Map.of("pack:base", initial), categories);
        event.edit("pack:base").editStage(0, stage -> stage.obtain("#minecraft:planks", 3));
        event.category("pack:custom").style("thaumaturge:basics").formula("thaumaturge:ordo", 2);
        event.create("pack:new").category("pack:custom").parent("pack:base").icon("minecraft:iron_ingot")
            .stage(stage -> stage.text("Test").craftAny(new String[]{"minecraft:stone", "minecraft:cobblestone"}, 2))
            .addendum(page -> page.text("Extra").requireResearch("pack:base"));
        var result = event.finish();
        check(result.entries().get("pack:base").get("extra_datapack_field").getAsString().equals("preserved"), "edit retains unknown fields");
        check(!initial.getAsJsonArray("stages").get(0).getAsJsonObject().has("obtain"), "edit does not mutate source pack");
        check(result.entries().get("pack:base").getAsJsonArray("stages").get(0).getAsJsonObject()
            .getAsJsonArray("obtain").get(0).getAsJsonObject().get("items").getAsString().equals("#minecraft:planks"), "tag requirement encoding");
        check(result.categories().get("pack:custom").getAsJsonArray("formula").get(0).getAsJsonObject()
            .get("aspect").getAsString().equals("thaumaturge:ordo"), "native aspect list encoding");
        var fresh = new ResearchEventJS(Map.of("pack:base", initial), categories);
        check(fresh.finish().entries().isEmpty(), "new load has no stale builders");
        rejects(() -> event.create("pack:new"), "already exists");
        rejects(() -> event.edit("pack:missing"), "Unknown research");
        rejects(() -> new ResearchBuilder("pack:empty").build(), "at least one stage");
        rejects(() -> research("pack:bad").complexity(4), "Complexity");
        rejects(() -> new StageBuilder().obtain("minecraft:iron_ingot", 0), "positive");
        rejects(() -> new StageBuilder().warp(-1), "negative");
        rejects(() -> ResearchJson.id("Missing Namespace"), "identifier");
        rejects(() -> ResearchJson.parent("pack:base@0"), "positive");
        rejects(() -> new StageBuilder().construct(2, 1, 1, new String[]{"minecraft:stone"}), "x*y*z");
        rejects(() -> research("pack:bad").editStage(5, page -> page.text("No")), "index");
        var missing = new ResearchEventJS(Map.of(), categories);
        missing.create("pack:a").parent("pack:missing").stage(page -> page.text("A"));
        rejects(missing::finish, "unknown ID");
        var cycle = new ResearchEventJS(Map.of(), categories);
        cycle.create("pack:a").parent("pack:b").stage(page -> page.text("A"));
        cycle.create("pack:b").stage(page -> page.text("B").requireResearch("pack:a"));
        rejects(cycle::finish, "cycle");
        var tabCycle = new ResearchEventJS(Map.of(), categories);
        tabCycle.category("pack:tab").requireResearch("pack:a");
        tabCycle.create("pack:a").category("pack:tab").stage(page -> page.text("A"));
        rejects(tabCycle::finish, "cycle");
        var partial = new ResearchEventJS(Map.of(), categories);
        partial.create("pack:a").parent("pack:b@1").stage(page -> page.text("A"));
        partial.create("pack:b").parent("pack:a@1").stage(page -> page.text("B"));
        check(partial.finish().entries().size() == 2, "stage-qualified reciprocal links allowed");
        var stage = new ResearchEventJS(Map.of("pack:base", initial), categories);
        stage.create("pack:new").parent("pack:base@2").stage(page -> page.text("New"));
        rejects(stage::finish, "exceeds");
        var brokenScript = new ResearchEventJS(Map.of(), categories);
        brokenScript.scriptFailed(new IllegalStateException("Test exception"));
        rejects(brokenScript::finish, "script failed");
        check(ThaumaturgeBinding.gate("pack:a", 0).get("stage").getAsInt() == 0, "gate supports native zero-based stages");
        check(result.summary().equals(new ResearchEventJS.Summary(1, 1, 1, 0)), "summary counts unique records rather than method calls");
        var copies = new ResearchEventJS(Map.of("pack:base", initial), categories);
        copies.edit("pack:base").title("Before copy");
        copies.copy("pack:base", "pack:copy").title("Copy").editStage(0, page -> page.obtain("minecraft:gold_ingot", 2));
        copies.edit("pack:base").title("After copy");
        var copied = copies.get("pack:copy");
        check(copied.has("extra_datapack_field"), "copy preserves unknown datapack fields");
        check(copied.get("name").getAsString().equals("Copy"), "copy can be edited independently");
        check(!copies.get("pack:base").getAsJsonArray("stages").get(0).getAsJsonObject().has("obtain"), "copy has independent nested stages");
        copies.copy("pack:copy", "pack:second");
        copies.edit("pack:copy").clearStages().stage(page -> page.text("Replaced"));
        check(copies.get("pack:second").getAsJsonArray("stages").get(0).getAsJsonObject().has("obtain"), "copy captures earlier script edits as a snapshot");
        rejects(() -> copies.copy("pack:base", "pack:base"), "already exists");
        rejects(() -> copies.copy("pack:missing", "pack:absent"), "Unknown research");
        check(!copies.exists("pack:absent"), "failed copy does not reserve destination ID");
        copies.editCategory("thaumaturge:basics").index(2);
        check(copies.finish().summary().equals(new ResearchEventJS.Summary(2, 1, 0, 1)), "summary distinguishes category edits");
        rejects(() -> research("pack:context").stage(page -> page.text("Test").obtain("minecraft:stone", -1)), "Research 'pack:context', stage[1], field 'obtain'");
        rejects(() -> research("pack:context").editStage(0, page -> page.text("")), "Research 'pack:context', stage[0], field 'text'");
        rejects(() -> research("pack:context").addendum(page -> page.recipe("minecraft:stone")), "field 'text'");
        var invalidReference = new ResearchEventJS(Map.of(), categories);
        invalidReference.create("pack:context").stage(page -> page.text("Test").requireResearch("pack:missing"));
        rejects(invalidReference::finish, "Research 'pack:context', stage[0], field 'required_research'");
        var invalidItem = new ResearchEventJS(Map.of(), categories);
        invalidItem.create("pack:context").stage(page -> page.text("Test").obtain("minecraft:missing_item"));
        var invalidItems = invalidItem.finish().entries();
        rejects(() -> ResearchValidator.validateItems(invalidItems, id -> false), "Research 'pack:context', stage[0], field 'obtain[0].items': Unknown item 'minecraft:missing_item'");
        var validTags = new ResearchEventJS(Map.of(), categories);
        validTags.create("pack:tags").stage(page -> page.text("Test").obtain("#minecraft:planks"));
        ResearchValidator.validateItems(validTags.finish().entries(), id -> { throw new AssertionError("Tags must use the native registry loader"); });
        check(true, "tags are left for native resolution");
        System.out.println("KubeThaum: " + checks + " progression checks passed");
    }
    private static ResearchBuilder research(String id) { return new ResearchBuilder(id).stage(page -> page.text("Base text")); }
    private static void check(boolean value, String description) {
        if (!value) throw new AssertionError(description);
        checks++;
    }
    private static void rejects(Runnable action, String message) {
        try { action.run(); } catch (IllegalArgumentException ex) {
            check(ex.getMessage().contains(message), "Expected '" + message + "', got '" + ex.getMessage() + "'");
            return;
        }
        throw new AssertionError("Expected rejection: " + message);
    }
}
