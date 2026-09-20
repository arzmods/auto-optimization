package com.arzmods.potatopvp.mixin;

import com.arzmods.potatopvp.PotatoConfig;
import com.arzmods.potatopvp.client.PotatoAnimations;
import com.arzmods.potatopvp.client.PotatoTextures;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where the Textures and Animations settings actually bite.
 *
 * <p>{@code SpriteContents} is one decoded image in a texture atlas - every
 * block face, in other words. Catching it here means the reduced pixels are
 * what gets stitched, mipmapped and uploaded, so the saving is real GPU memory
 * and not just a filter drawn on top.
 */
@Mixin(SpriteContents.class)
public class SpriteContentsMixin {

    /**
     * Deliberately takes no target arguments. Mixin allows a handler that
     * declares only the CallbackInfo, and that makes this bind to either
     * constructor without caring which one was used.
     */
    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void potatopvp$reduceDetail(CallbackInfo ci) {
        SpriteContents self = (SpriteContents) (Object) this;
        NativeImage image = PotatoTextures.findImage(self);
        if (image != null) {
            PotatoTextures.degrade(self.name(), self.width(), self.height(), image, PotatoConfig.textures());
        }
    }

    /**
     * Reporting a sprite as not animated is what freezes it on frame one.
     *
     * <p>The old createTicker hook is gone in 26.3 - animation now runs through
     * createAnimationState - but callers still gate on isAnimated(), and
     * answering false here is far safer than returning a null animation state
     * into rendering code that may not expect one.
     */
    @Inject(method = "isAnimated", at = @At("HEAD"), cancellable = true, require = 0)
    private void potatopvp$freezeAnimation(CallbackInfoReturnable<Boolean> cir) {
        SpriteContents self = (SpriteContents) (Object) this;
        if (!PotatoAnimations.allowsAnimation(self.name())) {
            cir.setReturnValue(false);
        }
    }
}
