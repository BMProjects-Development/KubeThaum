package org.bmp.kubethaum;

import dev.latvian.mods.kubejs.KubeJSPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import org.bmp.kubethaum.research.ResearchCheckCommand;

/** Fresh files must be checked without changing world registries or live script handlers. */
final class ScriptCheckSmoke {
    static void verify(MinecraftServer server) throws Exception {
        var registry = server.registryAccess().lookupOrThrow(IResearchEntry.REGISTRY_KEY);
        var original = registry.getValue(Identifier.parse("pack:basic_metallurgy"));
        long count = registry.listElements().count();
        Path file = KubeJSPaths.SERVER_SCRIPTS.resolve("zz_check_verification.js");
        Path ignored = KubeJSPaths.SERVER_SCRIPTS.resolve("zz_check_ignored.js");
        String good = """
            ThaumaturgeEvents.research(event => {
              event.category('check:temporary').style('thaumaturge:alchemy')
              event.create('check:dry_run').category('check:temporary')
                .stage(stage => {
                  stage.text('Not applied')
                  stage.obtain('#minecraft:planks', 2)
                })
              event.edit('pack:basic_metallurgy').title('Must never reach the live registry')
            })
            ServerEvents.recipes(event => { throw new Error('Must not execute recipes') })
            """;
        try {
            Files.writeString(file, good);
            Files.writeString(ignored, "// ignored: true\nconst syntaxError = )\n");
            var result = ResearchCheckCommand.check(server);
            require(result.successful(), "fresh good script and ignored file: " + result.problems());
            require(result.changes().entries().containsKey("check:dry_run"), "new disk script was not evaluated");
            require(registry.getValue(Identifier.parse("check:dry_run")) == null, "dry run added a live entry");
            require(registry.getValue(Identifier.parse("pack:basic_metallurgy")) == original
                && registry.listElements().count() == count, "dry run replaced live holders");
            require(server.getCommands().getDispatcher().execute("kubethaum check", server.createCommandSourceStack()) == 1,
                "operator command did not succeed");

            Files.writeString(file, good.replace("'#minecraft:planks', 2", "'minecraft:missing_item', 2"));
            result = ResearchCheckCommand.check(server);
            require(!result.successful() && result.problems().stream().anyMatch(problem ->
                problem.location().file().endsWith("zz_check_verification.js") && problem.location().line() == 6
                    && problem.message().contains("minecraft:missing_item")), "fresh item error location: " + result.problems());
            require(server.getCommands().getDispatcher().execute("kubethaum check", server.createCommandSourceStack()) == 0,
                "bad command must return failure");

            Files.writeString(file, good.replace("event.create('check:dry_run').category('check:temporary')", "event.create('check:dry_run').category('check:temporary').noteAspect('thaumaturge:missing_aspect', 1)"));
            result = ResearchCheckCommand.check(server);
            require(!result.successful() && result.problems().stream().anyMatch(problem -> problem.message().contains("missing_aspect")),
                "native codec did not reject missing aspect: " + result.problems());

            Files.writeString(file, good.replace("stage.text('Not applied')", "stage.text('Not applied'"));
            result = ResearchCheckCommand.check(server);
            require(!result.successful() && result.problems().getFirst().location().file().endsWith("zz_check_verification.js"),
                "fresh syntax error was not found");
            Files.writeString(file, good);
            require(ResearchCheckCommand.check(server).successful(), "fixed file did not pass on repeated check");
            require(registry.getValue(Identifier.parse("pack:basic_metallurgy")) == original
                && registry.listElements().count() == count, "failed checks changed live registry");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(ignored);
        }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("Research check: " + message);
    }
}
