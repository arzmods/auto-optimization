package com.arzmods.potatopvp.mixin;

import com.arzmods.potatopvp.PotatoConfig;
import com.arzmods.potatopvp.PotatoPvP;
import com.arzmods.potatopvp.client.PotatoAnimations;
import com.arzmods.potatopvp.client.PotatoTextures;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Where the Textures and Animations settings actually bite.
 *
 * <p>Both settings are applied by rewriting pixels and nothing else. Nothing
 * here changes an image's size, its frame count, or what the sprite reports
 * about itself - the atlas upload is sized from exactly those things, and a
 * wrong answer stops the game booting rather than merely looking odd.
 *
 * <p>There are two entry points on purpose. An injector that fails to find its
 * target is silently skipped rather than reported, so relying on a single one
 * means a miss looks identical to "the feature does nothing". Whichever fires
 * first wins and the other no-ops, and the log says which it was.
 */
@Mixin(SpriteContents.class)
public class SpriteContentsMixin {

    @Unique
    private boolean potatopvp$reduced;

    /** Declares no target arguments, so it binds to either constructor. */
    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void potatopvp$onConstructed(CallbackInfo ci) {
        potatopvp$reduce("constructor");
    }

    /**
     * Backup. Runs before the mipmap pyramid is built from the original image,
     * and has a far simpler signature than the constructors, so it is the more
     * likely of the two to bind.
     */
    @Inject(method = "increaseMipLevel", at = @At("HEAD"), require = 0)
    private void potatopvp$onMipLevel(int mipLevel, CallbackInfo ci) {
        potatopvp$reduce("increaseMipLevel");
    }

    @Unique
    private void potatopvp$reduce(String via) {
        if (this.potatopvp$reduced) {
            return;
        }
        this.potatopvp$reduced = true;
        try {
            SpriteContents self = (SpriteContents) (Object) this;
            NativeImage image = PotatoTextures.findImage(self);

            if (!PotatoAnimations.allowsAnimation(self.name())) {
                PotatoTextures.freezeFrames(self.width(), self.height(), image);
            }
            PotatoTextures.degrade(self.name(), self.width(), self.height(), image,
                    PotatoConfig.textures(), via);
        } catch (Throwable t) {
            // A texture that fails to load takes the whole atlas, and therefore
            // the whole game, down with it. Never let that be this mod's doing.
            PotatoPvP.LOGGER.warn("[Potato PvP] Skipped reducing a sprite", t);
        }
    }
}
