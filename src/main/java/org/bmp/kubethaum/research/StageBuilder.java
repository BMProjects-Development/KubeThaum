package org.bmp.kubethaum.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class StageBuilder extends PageBuilder<StageBuilder> {
    public StageBuilder() { this(new JsonObject()); }
    public StageBuilder(JsonObject data) { super(data); }
    StageBuilder(JsonObject data, String context) { super(data, context); }
    @Override protected StageBuilder self() { return this; }

    public StageBuilder obtain(String item) { return obtain(item, 1); }
    public StageBuilder obtain(String item, int amount) { return obtain(item, amount, null); }
    public StageBuilder obtain(String item, int amount, JsonObject components) {
        return field("obtain", () -> ResearchJson.array(data, "obtain").add(ResearchJson.requirement(new String[]{item}, amount, components)));
    }
    public StageBuilder obtainAny(String[] items, int amount) {
        return field("obtain", () -> ResearchJson.array(data, "obtain").add(ResearchJson.requirement(items, amount, null)));
    }
    public StageBuilder craft(String item) { return craft(item, 1); }
    public StageBuilder craft(String item, int amount) { return craft(item, amount, null); }
    public StageBuilder craft(String item, int amount, JsonObject components) {
        return field("craft", () -> ResearchJson.array(data, "craft").add(ResearchJson.requirement(new String[]{item}, amount, components)));
    }
    public StageBuilder craftAny(String[] items, int amount) {
        return field("craft", () -> ResearchJson.array(data, "craft").add(ResearchJson.requirement(items, amount, null)));
    }
    public StageBuilder knowledge(String type, String category, int amount) {
        return field("knowledge", () -> ResearchJson.array(data, "knowledge").add(ResearchJson.knowledge(type, category, amount)));
    }
    public StageBuilder requireKnowledge(String type, String category, int amount) {
        return field("required_knowledge", () -> ResearchJson.array(data, "required_knowledge").add(ResearchJson.knowledge(type, category, amount)));
    }
    public StageBuilder warp(int amount) {
        return field("warp", () -> data.addProperty("warp", ResearchJson.nonNegative(amount, "Warp")));
    }
    public StageBuilder construct(int x, int y, int z, String[] cells) {
        return construct(x, y, z, cells, new JsonArray());
    }
    public StageBuilder construct(int x, int y, int z, String[] cells, JsonArray cost) {
        return field("construct", () -> setConstruct(x, y, z, cells, cost));
    }
    private void setConstruct(int x, int y, int z, String[] cells, JsonArray cost) {
        ResearchJson.positive(x, "x size");
        ResearchJson.positive(y, "y size");
        ResearchJson.positive(z, "z size");
        if ((long)x * y * z != cells.length) throw new IllegalArgumentException("Construct needs exactly x*y*z cells");
        var json = new JsonObject();
        json.addProperty("x_size", x);
        json.addProperty("y_size", y);
        json.addProperty("z_size", z);
        var array = new JsonArray();
        for (String cell : cells) {
            if (!cell.isEmpty()) ResearchJson.id(cell.startsWith("#") ? cell.substring(1) : cell);
            array.add(cell);
        }
        json.add("cells", array);
        json.add("cost", cost.deepCopy());
        data.add("construct", json);
    }
    public StageBuilder clearObtain() { data.remove("obtain"); return this; }
    public StageBuilder clearCraft() { data.remove("craft"); return this; }
    public StageBuilder clearRequiredResearch() { data.remove("required_research"); return this; }
    public StageBuilder clearRequiredKnowledge() { data.remove("required_knowledge"); return this; }
    public StageBuilder clearRecipes() { data.remove("recipes"); return this; }
}
