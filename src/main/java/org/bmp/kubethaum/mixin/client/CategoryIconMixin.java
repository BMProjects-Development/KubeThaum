package org.bmp.kubethaum.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bmp.kubethaum.research.CategoryIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class CategoryIconMixin {
    @Inject(method = "blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V",
        at = @At("HEAD"), cancellable = true, require = 1)
    private void kubethaum$drawCategoryItem(RenderPipeline pipeline, Identifier texture, int x, int y,
                                          float u, float v, int width, int height, int regionWidth, int regionHeight,
                                          int textureWidth, int textureHeight, int color, CallbackInfo ci) {
        var id = CategoryIcon.itemId(texture);
        if (id == null) return;
        ci.cancel();
        if ((color >>> 24) == 0) return;

        var stack = new ItemStack(BuiltInRegistries.ITEM.getOptional(id).orElse(Items.BARRIER));
        var graphics = (GuiGraphicsExtractor) (Object) this;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(width / 16f, height / 16f);
        graphics.item(stack, 0, 0);
        pose.popMatrix();
    }
}
