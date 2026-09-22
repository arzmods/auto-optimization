package com.arzmods.potatopvp.mixin;

import com.arzmods.potatopvp.PotatoConfig;
import com.arzmods.potatopvp.PotatoPvP;
import com.arzmods.potatopvp.client.PotatoAnimations;
import com.arzmods.potatopvp.client.PotatoTextures;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Where the Textures and Animations settings actually bite.
 *
 * <p>{@code SpriteContents} is one decoded image in a texture atlas - every
 * block face, in other words. Catching it here means the reduced pixels are
 * what gets stitched, mipmapped and uploaded, so the saving is real GPU memory
 * and not just a filter drawn on top.
 *
 * <p>Both settings are applied by rewriting pixels and nothing else. Nothing
 * here changes an image's size, its frame count, or what the sprite reports
 * about itself, because the atlas upload is sized from exactly those things and
 * a wrong answer stops the game booting rather than merely looking odd.
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
        try {
            SpriteContents self = (SpriteContents) (Object) this;
            NativeImage image = PotatoTextures.findImage(self);
            if (image == null) {
                return;
            }

            if (!PotatoAnimations.allowsAnimation(self.name())) {
                PotatoTextures.freezeFrames(self.width(), self.height(), image);
            }

            PotatoTextures.degrade(self.name(), self.width(), self.height(), image, PotatoConfig.textures());
        } catch (Throwable t) {
            // A texture that fails to load takes the whole atlas, and therefore
            // the whole game, down with it. Never let that be this mod's doing.
            PotatoPvP.LOGGER.warn("[Potato PvP] Skipped reducing a sprite", t);
        }
    }
}
