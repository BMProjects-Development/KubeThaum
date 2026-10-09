package org.bmp.kubethaum;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import org.bmp.kubethaum.research.CategoryBuilder;
import org.bmp.kubethaum.research.ResearchBuilder;
import org.bmp.kubethaum.research.ResearchScriptCheck;

public final class ScriptCheckVerification {
    private static int checks;
    private static final Map<String, JsonObject> CATEGORIES = Map.of("thaumaturge:basics", CategoryBuilder.defaults());
    private static final String GOOD = """
        const amount = 2 + 2
        ThaumaturgeEvents.research(event => {
          event.create('check:entry')
            .stage(stage => {
              stage.text('Bring iron')
              stage.obtain('minecraft:iron_ingot', amount)
            })
        })
        ServerEvents.recipes(event => { throw new Error('Recipe callback must not execute') })
        """;
    public static void main(String[] args) {
        var good = check(GOOD);
        verify(good.successful(), "normal script: " + good.problems());
        verify(good.handlers() == 1 && good.skippedHandlers() == 1, "only research handler executed");
        verify(good.changes().entries().get("check:entry").getAsJsonArray("stages").get(0).getAsJsonObject()
            .getAsJsonArray("obtain").get(0).getAsJsonObject().get("amount").getAsInt() == 4, "fresh JS values evaluated");
        var negative = check(GOOD.replace("2 + 2", "-1"));
        verify(!negative.successful(), "negative amount rejected");
        verify(negative.problems().getFirst().location().line() == 6, "runtime error points to obtain line: " + negative.problems());
        verify(negative.problems().getFirst().message().contains("stage[0]")
            && negative.problems().getFirst().message().contains("obtain"), "builder context retained");
        var missing = check(GOOD.replace("minecraft:iron_ingot", "minecraft:missing"));
        verify(!missing.successful() && missing.problems().getFirst().location().line() == 6,
            "deferred item validation retains original line: " + missing.problems());
        var multiple = check(GOOD.replace("stage.obtain('minecraft:iron_ingot', amount)",
            "stage.obtain('minecraft:missing', amount)\n      stage.obtain('minecraft:iron_ingot', 1)"));
        verify(!multiple.successful() && multiple.problems().getFirst().location().line() == 6,
            "first invalid requirement keeps its line after later requirements: " + multiple.problems());
        var syntax = check(GOOD.replace("amount = 2 + 2", "amount = )"));
        verify(!syntax.successful() && syntax.problems().getFirst().location().line() == 1, "fresh syntax error located");
        var many = ResearchScriptCheck.check(List.of(new ResearchScriptCheck.Source("a.js", "const x = )"),
            new ResearchScriptCheck.Source("b.js", "const y = )")), Map.of(), CATEGORIES, id -> true);
        verify(many.problems().size() == 2, "syntax errors collected across files");
        verify(!check("Java.loadClass('java.lang.System')").successful(), "Java binding unavailable");
        verify(!check("Packages.java.lang.System.exit(0)").successful(), "Packages unavailable");
        verify(!check("ThaumaturgeEvents.research(event => event.getClass())").successful(), "Object.getClass unavailable");
        verify(!check(GOOD.replace("stage.text('Bring iron')", "stage.build()" )).successful(), "hidden Java helpers unavailable");
        verify(!check("while (true) {}").successful(), "infinite loop stops within instruction budget");
        var category = check("""
            ThaumaturgeEvents.research(event => {
              event.category('check:tab').style('thaumaturge:basics')
              event.create('check:entry').category('check:tab').stage(stage => {
                stage.text('Components').obtain('minecraft:iron_ingot', 1, { 'minecraft:damage': 0 })
                stage.construct(1, 1, 1, ['minecraft:iron_ingot'], [{aspect: 'thaumaturge:ordo', amount: 1}])
              })
              event.copy('check:entry', 'check:copy').editStage(0, stage => stage.clearObtain())
            })
            """);
        verify(category.successful(), "arrays/components/callbacks/copy: " + category.problems());
        var original = new ResearchBuilder("check:base").stage(stage -> stage.text("Original")).build();
        var result = ResearchScriptCheck.check(List.of(new ResearchScriptCheck.Source("edit.js", """
            ThaumaturgeEvents.research(event => event.edit('check:base').title('Changed'))
            """)), Map.of("check:base", original), CATEGORIES, id -> true);
        verify(result.successful() && original.get("name").getAsString().equals("research.check.base"), "source data unchanged");
        verify(check(GOOD).successful(), "repeated checks do not retain registrations or created IDs");
        System.out.println("Script check verification: " + checks + " checks passed");
    }
    private static ResearchScriptCheck.Result check(String code) {
        return ResearchScriptCheck.check(List.of(new ResearchScriptCheck.Source("server_scripts/test.js", code)),
            Map.of(), CATEGORIES, id -> id.toString().equals("minecraft:iron_ingot"));
    }
    private static void verify(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
