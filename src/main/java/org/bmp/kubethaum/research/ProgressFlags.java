package org.bmp.kubethaum.research;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * IDs Thaumaturge records as research progress without a research entry, so they are valid in
 * {@code parents} and {@code required_research} although no entry has that ID.
 */
public final class ProgressFlags {
    /**
     * Granted by Thaumaturge's own event code ({@code ResearchProgressionEvents}), not by data.
     * Keep in sync with Thaumaturge: these are private constants there.
     */
    private static final Set<String> EVENT_FLAGS = Set.of(
        "thaumaturge:gotcrystals", "thaumaturge:gotdream", "thaumaturge:gotthaumonomicon", "thaumaturge:f_onfire",
        "thaumaturge:m_deepdown", "thaumaturge:m_uphigh", "thaumaturge:m_hellandback", "thaumaturge:m_endoftheworld",
        "thaumaturge:m_walker", "thaumaturge:m_runner", "thaumaturge:m_jumper", "thaumaturge:m_swimmer");
    /** Only the event flags; for tests and callers without loaded data. */
    public static final ProgressFlags EVENTS_ONLY = new ProgressFlags(Set.of(), Map.of());
    private static final String SCAN_DIR = "thaumaturge/scan";
    private static final String ASPECT_DIR = "thaumaturge/aspect";
    private static final String SCANNED = "thaumaturge:scanned/";

    private final Set<String> scanKeys;
    private final Map<String, Predicate<String>> scannedKinds;

    /**
     * @param scanKeys the {@code key} of every scan entry
     * @param scannedKinds for each {@code ScanKeys} kind ({@code item}, {@code entity}, ...), whether
     *                     a {@code namespace:path} ID of that kind exists
     */
    public ProgressFlags(Set<String> scanKeys, Map<String, Predicate<String>> scannedKinds) {
        this.scanKeys = Set.copyOf(scanKeys);
        this.scannedKinds = Map.copyOf(scannedKinds);
    }

    /** Reads scan entries and data-driven IDs from the resources the research registry is loaded from. */
    public static ProgressFlags of(ResourceManager resources) {
        var keys = new HashSet<String>();
        ResearchResources.read(resources, SCAN_DIR).forEach((id, scan) -> {
            if (scan.has("key")) keys.add(scan.get("key").getAsString());
        });
        var aspects = ids(resources, ASPECT_DIR);
        var enchantments = ids(resources, "enchantment");
        return new ProgressFlags(keys, Map.of(
            "item", registered(BuiltInRegistries.ITEM::containsKey),
            "entity", registered(BuiltInRegistries.ENTITY_TYPE::containsKey),
            "effect", registered(BuiltInRegistries.MOB_EFFECT::containsKey),
            "aspect", aspects::contains,
            "enchantment", enchantments::contains));
    }

    public boolean accepts(String id) {
        return EVENT_FLAGS.contains(id) || scanKeys.contains(id) || scanned(id);
    }

    /** {@code thaumaturge:scanned/<kind>/<namespace>/<path>}, as built by Thaumaturge's {@code ScanKeys}. */
    private boolean scanned(String id) {
        if (!id.startsWith(SCANNED)) return false;
        String[] parts = id.substring(SCANNED.length()).split("/", 3);
        if (parts.length < 3 || parts[1].isEmpty() || parts[2].isEmpty()) return false;
        var kind = scannedKinds.get(parts[0]);
        return kind != null && kind.test(parts[1] + ":" + parts[2]);
    }

    private static Predicate<String> registered(Predicate<ResourceLocation> registry) {
        return id -> {
            var parsed = ResourceLocation.tryParse(id);
            return parsed != null && registry.test(parsed);
        };
    }
    private static Set<String> ids(ResourceManager resources, String directory) {
        var result = new HashSet<String>();
        resources.listResources(directory, id -> id.getPath().endsWith(".json")).keySet().forEach(file -> {
            String path = file.getPath().substring(directory.length() + 1, file.getPath().length() - 5);
            result.add(file.getNamespace() + ":" + path);
        });
        return result;
    }
}
