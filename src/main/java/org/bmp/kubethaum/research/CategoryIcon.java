package org.bmp.kubethaum.research;

import net.minecraft.resources.Identifier;
import org.bmp.kubethaum.KubeThaum;
import org.jetbrains.annotations.Nullable;

/** Item icons travel through Thaumaturge's existing category codec as reserved texture IDs. */
public final class CategoryIcon {
    private static final String ITEM_PREFIX = "category_item/";

    private CategoryIcon() {}

    public static Identifier forItem(Identifier item) {
        return Identifier.fromNamespaceAndPath(KubeThaum.ID, ITEM_PREFIX + item.getNamespace() + "/" + item.getPath());
    }

    public static @Nullable Identifier itemId(Identifier icon) {
        String path = icon.getPath();
        if (!KubeThaum.ID.equals(icon.getNamespace()) || !path.startsWith(ITEM_PREFIX)) return null;
        int separator = path.indexOf('/', ITEM_PREFIX.length());
        return separator < 0 ? null : Identifier.tryBuild(path.substring(ITEM_PREFIX.length(), separator), path.substring(separator + 1));
    }
}
