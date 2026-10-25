package org.bmp.kubethaum.mixin.client;

import com.leclowndu93150.thaumaturge.client.screen.TTTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.bmp.kubethaum.client.FlagDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Names scanned items, creatures, effects, enchantments and scan entries in requirement tooltips instead of "?". */
@Mixin(TTTooltips.class)
public abstract class TTTooltipsMixin {
    // Optional: if Thaumaturge renames this, the book falls back to its own "?" name.
    @Inject(method = "prereqEntryName", at = @At("HEAD"), cancellable = true, require = 0)
    private static void kubethaum$flagName(ResourceLocation id, CallbackInfoReturnable<Component> cir) {
        FlagDisplay.of(id).ifPresent(subject -> cir.setReturnValue(subject.name()));
    }
}
