package org.bmp.kubethaum.mixin.client;

import com.leclowndu93150.thaumaturge.client.screen.research.EntryDetailScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.bmp.kubethaum.client.FlagDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws scanned items, creatures, effects and scan entries in requirement slots instead of "?". */
@Mixin(EntryDetailScreen.class)
public abstract class EntryDetailScreenMixin {
    // Optional: if Thaumaturge renames this, the book falls back to its own "?" icon.
    @Inject(method = "drawPrereqIcon", at = @At("HEAD"), cancellable = true, require = 0)
    private void kubethaum$flagIcon(GuiGraphics graphics, int x, int y, ResourceLocation prereq, CallbackInfo ci) {
        var subject = FlagDisplay.of(prereq).orElse(null);
        if (subject == null) return;
        if (!subject.icon().isEmpty()) graphics.renderItem(subject.icon(), x, y);
        else if (subject.sprite() != null) graphics.blit(x, y, 0, 16, 16, subject.sprite());
        else return; // A creature without a spawn egg keeps the book's own icon.
        ci.cancel();
    }
}
