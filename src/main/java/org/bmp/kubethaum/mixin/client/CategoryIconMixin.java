package org.bmp.kubethaum.mixin.client;

import com.leclowndu93150.thaumaturge.client.render.GuiBlend;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bmp.kubethaum.research.CategoryIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiBlend.class)
public abstract class CategoryIconMixin {
    @Inject(method = {
        "blitTinted(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;IIFFIIIII)V",
        "blitAdditive(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;IIFFIIIII)V"
    }, at = @At("HEAD"), cancellable = true, require = 2)
    private static void kubethaum$drawCategoryItem(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                                                 float u, float v, int width, int height,
                                                 int textureWidth, int textureHeight, int color, CallbackInfo ci) {
        var id = CategoryIcon.itemId(texture);
        if (id == null) return;
        ci.cancel();
        if ((color >>> 24) == 0) return;

        var stack = new ItemStack(BuiltInRegistries.ITEM.getOptional(id).orElse(Items.BARRIER));
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(width / 16f, height / 16f, 1);
        graphics.renderItem(stack, 0, 0);
        pose.popPose();
    }
}
