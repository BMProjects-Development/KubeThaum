package org.bmp.kubethaum.client;

import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import com.leclowndu93150.thaumaturge.api.research.scan.ScanEntry;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import org.jetbrains.annotations.Nullable;

/**
 * Names and icons for progress flags that the Thaumonomicon shows as "?": its requirement slots only
 * draw research entries, aspects and {@code f_}/{@code m_}/{@code c_} flags. Display only; client side.
 */
public final class FlagDisplay {
    private static final String SCANNED = "scanned/";

    /** @param icon drawn when not empty; otherwise {@code sprite} when set, else the book's own icon */
    public record Subject(Component name, ItemStack icon, @Nullable TextureAtlasSprite sprite) {}

    private FlagDisplay() {}

    public static Optional<Subject> of(ResourceLocation flag) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null || !flag.getNamespace().equals("thaumaturge")) return Optional.empty();
        var registries = level.registryAccess();
        // The book draws real entries itself, including Thaumaturge's own hidden scan entries.
        if (registries.lookup(IResearchEntry.REGISTRY_KEY).flatMap(entries -> entries.get(ResourceKey.create(IResearchEntry.REGISTRY_KEY, flag))).isPresent()) {
            return Optional.empty();
        }
        var scan = registries.lookup(ScanEntry.REGISTRY_KEY)
            .flatMap(scans -> scans.listElements().map(Holder.Reference::value).filter(entry -> entry.key().equals(flag)).findFirst());
        if (scan.isPresent()) return target(scan.get());
        if (!flag.getPath().startsWith(SCANNED)) return Optional.empty();
        String[] parts = flag.getPath().substring(SCANNED.length()).split("/", 3);
        var id = parts.length < 3 ? null : ResourceLocation.tryBuild(parts[1], parts[2]);
        if (id == null) return Optional.empty();
        return switch (parts[0]) {
            case "item" -> BuiltInRegistries.ITEM.getOptional(id).map(item -> item(new ItemStack(item)));
            case "entity" -> BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(FlagDisplay::entity);
            case "effect" -> BuiltInRegistries.MOB_EFFECT.getHolder(id).map(effect -> new Subject(scanned(effect.value().getDisplayName()),
                ItemStack.EMPTY, minecraft.getMobEffectTextures().get(effect)));
            case "enchantment" -> registries.lookup(Registries.ENCHANTMENT).flatMap(enchantments -> enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, id)))
                .map(enchantment -> new Subject(scanned(enchantment.value().description()), new ItemStack(Items.ENCHANTED_BOOK), null));
            default -> Optional.empty(); // Aspects are drawn by the book; other kinds have no single subject.
        };
    }

    /** A scan entry is shown as its first item, block or creature. */
    private static Optional<Subject> target(ScanEntry scan) {
        var item = scan.items().flatMap(items -> items.stream().findFirst()).map(holder -> item(new ItemStack(holder)));
        if (item.isPresent()) return item;
        var block = scan.blocks().flatMap(blocks -> blocks.stream().findFirst())
            .map(holder -> new Subject(scanned(holder.value().getName()), new ItemStack(holder.value().asItem()), null));
        if (block.isPresent()) return block;
        return scan.entities().flatMap(entities -> entities.stream().findFirst()).map(holder -> entity(holder.value()));
    }

    private static Subject item(ItemStack stack) {
        return new Subject(scanned(stack.getHoverName()), stack, null);
    }
    private static Subject entity(EntityType<?> type) {
        var egg = SpawnEggItem.byId(type);
        return new Subject(scanned(type.getDescription()), egg == null ? ItemStack.EMPTY : new ItemStack(egg), null);
    }
    private static Component scanned(Component subject) {
        return Component.translatable("kubethaum.flag.scanned", subject);
    }
}
