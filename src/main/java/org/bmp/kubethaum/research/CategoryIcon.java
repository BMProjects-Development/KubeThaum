package org.bmp.kubethaum.research;

import net.minecraft.resources.ResourceLocation;
import org.bmp.kubethaum.KubeThaum;
import org.jetbrains.annotations.Nullable;

/** Item icons travel through Thaumaturge's existing category codec as reserved texture IDs. */
public final class CategoryIcon {
    private static final String ITEM_PREFIX = "category_item/";

    private CategoryIcon() {}

    public static ResourceLocation forItem(ResourceLocation item) {
        return ResourceLocation.fromNamespaceAndPath(KubeThaum.ID, ITEM_PREFIX + item.getNamespace() + "/" + item.getPath());
    }

    public static @Nullable ResourceLocation itemId(ResourceLocation icon) {
        String path = icon.getPath();
        if (!KubeThaum.ID.equals(icon.getNamespace()) || !path.startsWith(ITEM_PREFIX)) return null;
        int separator = path.indexOf('/', ITEM_PREFIX.length());
        return separator < 0 ? null : ResourceLocation.tryBuild(path.substring(ITEM_PREFIX.length(), separator), path.substring(separator + 1));
    }
}
