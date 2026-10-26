package org.bmp.kubethaum.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.SimpleRecipeComponent;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.resources.ResourceLocation;

/** Uses the same registry-aware validation as Thaumaturge's recipe serializers. */
public final class NativeRecipeComponent<T> extends SimpleRecipeComponent<T> {
    private final boolean aspectMap;

    private NativeRecipeComponent(RecipeComponentType<?> type, Codec<T> codec, boolean aspectMap) {
        super(type, codec, TypeInfo.OBJECT);
        this.aspectMap = aspectMap;
    }

    public static <T> RecipeComponentType<T> type(String name, Codec<T> codec, boolean aspectMap) {
        return RecipeComponentType.unit(ResourceLocation.fromNamespaceAndPath("kubethaum", name),
            type -> new NativeRecipeComponent<>(type, codec, aspectMap));
    }

    @Override public T wrap(RecipeScriptContext context, Object from) {
        JsonElement json = JsonUtils.of(context.cx(), from);
        if (aspectMap && json.isJsonObject()) {
            var array = new JsonArray();
            for (var entry : json.getAsJsonObject().entrySet()) {
                var cost = new JsonObject();
                cost.addProperty("aspect", entry.getKey());
                cost.add("amount", entry.getValue());
                array.add(cost);
            }
            json = array;
        }
        return codec.parse(context.ops().json(), json).getOrThrow();
    }
}
