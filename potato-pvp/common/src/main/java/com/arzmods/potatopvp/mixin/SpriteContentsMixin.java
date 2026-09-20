package com.arzmods.potatopvp.mixin;

import com.arzmods.potatopvp.PotatoConfig;
import com.arzmods.potatopvp.client.PotatoAnimations;
import com.arzmods.potatopvp.client.PotatoTextures;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;
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
     * declares only the CallbackInfo, and that makes this bind to the
     * constructor whatever its parameter list happens to look like.
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
     * Returning no ticker leaves the sprite parked on frame one forever, which
     * is exactly what we want from water, portals and sea lanterns on a
     * machine that cannot afford to re-upload them every tick.
     */
    @Inject(method = "createTicker", at = @At("HEAD"), cancellable = true, require = 0)
    private void potatopvp$freezeAnimation(CallbackInfoReturnable<SpriteTicker> cir) {
        SpriteContents self = (SpriteContents) (Object) this;
        if (!PotatoAnimations.allowsAnimation(self.name())) {
            cir.setReturnValue(null);
        }
    }
}
