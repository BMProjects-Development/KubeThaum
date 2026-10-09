package org.bmp.kubethaum.research;

import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import com.leclowndu93150.thaumaturge.content.research.ResearchCategory;
import com.leclowndu93150.thaumaturge.content.research.ResearchEntry;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import dev.latvian.mods.kubejs.KubeJSPaths;
import dev.latvian.mods.kubejs.script.ScriptFile;
import dev.latvian.mods.kubejs.script.ScriptFileInfo;
import dev.latvian.mods.kubejs.script.ScriptPack;
import dev.latvian.mods.kubejs.script.ScriptPackInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.bmp.kubethaum.KubeThaum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = KubeThaum.ID)
public final class ResearchCheckCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("KubeThaum");
    private ResearchCheckCommand() {}

    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("kubethaum")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("check").executes(context -> execute(context.getSource()))));
    }

    private static int execute(CommandSourceStack source) {
        ResearchScriptCheck.Result result;
        try { result = check(source.getServer()); }
        catch (IOException | RuntimeException error) {
            LOGGER.warn("Research check could not complete", error);
            source.sendFailure(Component.translatable("commands.kubethaum.check.unavailable", error.getMessage()));
            return 0;
        }
        if (result.successful()) {
            source.sendSuccess(() -> Component.translatable("commands.kubethaum.check.success", result.files(), result.handlers())
                .withStyle(ChatFormatting.GREEN), false);
        } else {
            source.sendFailure(Component.translatable("commands.kubethaum.check.failed", result.problems().size()));
            for (var problem : result.problems().stream().limit(8).toList()) {
                source.sendFailure(Component.literal(problem.toString()));
            }
            result.problems().forEach(problem -> LOGGER.warn("Research check: {}", problem));
            if (result.problems().size() > 8) source.sendFailure(Component.translatable("commands.kubethaum.check.more"));
        }
        source.sendSuccess(() -> Component.translatable("commands.kubethaum.check.scope").withStyle(ChatFormatting.GRAY), false);
        return result.successful() ? 1 : 0;
    }

    /** Fresh files and detached native decoding; never reloads KubeJS or writes to live registries. */
    public static ResearchScriptCheck.Result check(MinecraftServer server) throws IOException {
        var sources = readScripts();
        var resources = server.getResourceManager();
        var entries = ResearchResources.read(resources, ResearchResources.ENTRY_DIR);
        var categories = ResearchResources.read(resources, ResearchResources.CATEGORY_DIR);
        var result = ResearchScriptCheck.check(sources, entries, categories, BuiltInRegistries.ITEM::containsKey);
        if (!result.successful()) return result;
        var problems = new ArrayList<>(result.problems());
        var changed = result.changes();
        var allCategories = new LinkedHashMap<>(categories);
        allCategories.putAll(changed.categories());
        // Decode new categories in a private registry, so newly created entries can refer
        // to them while the actual world and its existing holders remain untouched.
        var temporary = new MappedRegistry<IResearchCategory>(IResearchCategory.REGISTRY_KEY, Lifecycle.stable());
        var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        for (var entry : allCategories.entrySet()) {
            try {
                var category = ResearchCategory.CODEC.parse(ops, entry.getValue()).getOrThrow();
                temporary.register(ResourceKey.create(IResearchCategory.REGISTRY_KEY, Identifier.parse(entry.getKey())),
                    category, RegistrationInfo.BUILT_IN);
            } catch (RuntimeException error) {
                String message = "Category '" + entry.getKey() + "': " + error.getMessage();
                problems.add(new ResearchScriptCheck.Problem(ResearchScriptCheck.locate(message, result.locations()), message));
            }
        }
        if (problems.isEmpty()) {
            temporary.freeze();
            var registries = HolderLookup.Provider.create(Stream.concat(server.registryAccess().listRegistries()
                .filter(registry -> !registry.key().equals(IResearchCategory.REGISTRY_KEY)), Stream.of(temporary)));
            var entryOps = RegistryOps.create(JsonOps.INSTANCE, registries);
            for (var entry : changed.entries().entrySet()) {
                try { ResearchEntry.CODEC.parse(entryOps, entry.getValue()).getOrThrow(); }
                catch (RuntimeException error) {
                    String message = "Research '" + entry.getKey() + "': " + error.getMessage();
                    problems.add(new ResearchScriptCheck.Problem(ResearchScriptCheck.locate(message, result.locations()), message));
                }
            }
        }
        return new ResearchScriptCheck.Result(result.files(), result.handlers(), result.skippedHandlers(),
            List.copyOf(problems), changed, result.locations());
    }

    private static List<ResearchScriptCheck.Source> readScripts() throws IOException {
        var root = KubeJSPaths.SERVER_SCRIPTS;
        if (!Files.isDirectory(root)) return List.of();
        var packInfo = new ScriptPackInfo("server_scripts", "");
        var pack = new ScriptPack(null, packInfo);
        var scripts = new ArrayList<ScriptFile>();
        long total = 0;
        try (var paths = Files.walk(root, 10)) {
            var files = paths.filter(Files::isRegularFile).filter(path -> {
                String name = path.getFileName().toString();
                return name.endsWith(".js") || name.endsWith(".ts") && !name.endsWith(".d.ts");
            }).limit(257).toList();
            if (files.size() > 256) throw new IOException("Check supports up to 256 server script files.");
            for (var file : files) {
                long size = Files.size(file);
                total += size;
                if (size > 256 * 1024 || total > 4 * 1024 * 1024) {
                    throw new IOException("Check limit exceeded at " + root.relativize(file) + ": 256 KiB per file, 4 MiB total.");
                }
                try {
                    var info = new ScriptFileInfo(packInfo, file, root.relativize(file).toString().replace('\\', '/'));
                    var script = new ScriptFile(pack, info);
                    if (script.skipLoading().isEmpty()) scripts.add(script);
                } catch (Exception error) { throw new IOException(file + ": " + error.getMessage(), error); }
            }
        }
        scripts.sort(null); // Same priorities, ignored/requires/packmode rules and line numbers as KubeJS.
        return scripts.stream().map(script -> new ResearchScriptCheck.Source(
            "kubejs/server_scripts/" + script.info.file, String.join("\n", script.lines))).toList();
    }
}
