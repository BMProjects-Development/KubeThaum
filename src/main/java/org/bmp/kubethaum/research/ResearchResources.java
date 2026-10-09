package org.bmp.kubethaum.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import dev.latvian.mods.kubejs.core.ScriptManagerHolderKJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.bmp.kubethaum.ThaumaturgeEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An immutable resource overlay consumed by the native registry codecs and network sync. */
public final class ResearchResources implements ResourceManager {
    public static final String ENTRY_DIR = "thaumaturge/research_entry";
    public static final String CATEGORY_DIR = "thaumaturge/research_category";
    private static final Logger LOGGER = LoggerFactory.getLogger("KubeThaum");
    private final ResourceManager delegate;
    private final Map<Identifier, Resource> overlay;
    private final ResearchEventJS.Summary summary;

    private ResearchResources(ResourceManager delegate, Map<Identifier, Resource> overlay, ResearchEventJS.Summary summary) {
        this.delegate = delegate;
        this.overlay = Map.copyOf(overlay);
        this.summary = summary;
    }

    public static ResourceManager apply(ResourceManager resources, List<RegistryDataLoader.RegistryData<?>> registries) {
        if (!ThaumaturgeEvents.RESEARCH.hasListeners()) return resources;
        boolean hasResearch = registries.stream().anyMatch(data -> data.key().equals(IResearchEntry.REGISTRY_KEY));
        if (!hasResearch) return resources;
        if (!(resources instanceof ScriptManagerHolderKJS holder) || holder.kjs$getScriptManager() == null) {
            throw new IllegalStateException("KubeThaum: server scripts are unavailable during research registry loading");
        }
        var event = new ResearchEventJS(read(resources, ENTRY_DIR), read(resources, CATEGORY_DIR));
        ThaumaturgeEvents.RESEARCH.post(ScriptType.SERVER, event);
        var changes = event.finish();
        ResearchValidator.validateItems(changes.entries(), BuiltInRegistries.ITEM::containsKey);
        var overlay = new LinkedHashMap<Identifier, Resource>();
        var source = holder.kjs$getScriptManager().internalDataPack;
        add(overlay, changes.entries(), ENTRY_DIR, source);
        add(overlay, changes.categories(), CATEGORY_DIR, source);
        return new ResearchResources(resources, overlay, changes.summary());
    }

    /** Called only after native registry loading succeeds, once for this resource overlay. */
    public static void logSummary(ResourceManager resources) {
        if (!(resources instanceof ResearchResources research)) return;
        var summary = research.summary;
        LOGGER.info("Research loaded: {} added, {} changed; categories: {} added, {} changed. Research script changes require reopening the world.",
            summary.addedResearch(), summary.changedResearch(), summary.addedCategories(), summary.changedCategories());
    }

    static Map<String, JsonObject> read(ResourceManager resources, String directory) {
        var result = new LinkedHashMap<String, JsonObject>();
        resources.listResources(directory, id -> id.getPath().endsWith(".json")).forEach((file, resource) -> {
            String path = file.getPath().substring(directory.length() + 1, file.getPath().length() - 5);
            String id = Identifier.fromNamespaceAndPath(file.getNamespace(), path).toString();
            try (var reader = resource.openAsReader()) {
                result.put(id, JsonParser.parseReader(reader).getAsJsonObject());
            } catch (IOException | RuntimeException ex) {
                throw new IllegalArgumentException("KubeThaum: cannot read '" + id + "' from " + resource.sourcePackId(), ex);
            }
        });
        return result;
    }
    private static void add(Map<Identifier, Resource> overlay, Map<String, JsonObject> entries, String directory, PackResources source) {
        entries.forEach((name, json) -> {
            var id = Identifier.parse(name);
            var file = id.withPath(directory + "/" + id.getPath() + ".json");
            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            overlay.put(file, new Resource(source, () -> new ByteArrayInputStream(bytes)));
        });
    }

    @Override public Set<String> getNamespaces() {
        var namespaces = new HashSet<>(delegate.getNamespaces());
        overlay.keySet().forEach(id -> namespaces.add(id.getNamespace()));
        return Set.copyOf(namespaces);
    }
    @Override public Optional<Resource> getResource(Identifier id) {
        return overlay.containsKey(id) ? Optional.of(overlay.get(id)) : delegate.getResource(id);
    }
    @Override public List<Resource> getResourceStack(Identifier id) {
        if (!overlay.containsKey(id)) return delegate.getResourceStack(id);
        var stack = new ArrayList<>(delegate.getResourceStack(id));
        stack.add(overlay.get(id));
        return List.copyOf(stack);
    }
    @Override public Map<Identifier, Resource> listResources(String path, Predicate<Identifier> filter) {
        var result = new HashMap<>(delegate.listResources(path, filter));
        overlay.forEach((id, resource) -> { if (matches(id, path, filter)) result.put(id, resource); });
        return Map.copyOf(result);
    }
    @Override public Map<Identifier, List<Resource>> listResourceStacks(String path, Predicate<Identifier> filter) {
        var result = new HashMap<>(delegate.listResourceStacks(path, filter));
        overlay.forEach((id, resource) -> { if (matches(id, path, filter)) result.put(id, getResourceStack(id)); });
        return Map.copyOf(result);
    }
    private static boolean matches(Identifier id, String path, Predicate<Identifier> filter) {
        return id.getPath().startsWith(path + "/") && filter.test(id);
    }
    @Override public Stream<PackResources> listPacks() { return delegate.listPacks(); }
}
