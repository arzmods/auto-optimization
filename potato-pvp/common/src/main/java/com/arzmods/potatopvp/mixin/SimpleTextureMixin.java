package com.arzmods.potatopvp.mixin;

import com.arzmods.potatopvp.PotatoConfig;
import com.arzmods.potatopvp.PotatoPvP;
import com.arzmods.potatopvp.client.PotatoTextures;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reduces textures that live outside a texture atlas.
 *
 * <p>Block textures are stitched into an atlas and handled elsewhere. Entity
 * textures are not - they are loaded one file at a time through this class, and
 * the end crystal is one of them.
 *
 * <p>Every standalone texture in the game passes through here, including
 * player skins and the interface, so the decision about what is actually worth
 * touching is left to the same filter the atlas path uses. Today that means end
 * crystals and nothing else.
 */
@Mixin(SimpleTexture.class)
public class SimpleTextureMixin {

    @Inject(method = "apply", at = @At("HEAD"), require = 0)
    private void potatopvp$reduceStandalone(TextureContents contents, CallbackInfo ci) {
        try {
            if (contents == null) {
                return;
            }
            NativeImage image = contents.image();
            if (image == null) {
                return;
            }
            Identifier id = ((SimpleTexture) (Object) this).resourceId();
            // A standalone texture is a single frame, so its frame size is just
            // its size.
            PotatoTextures.degrade(id, image.getWidth(), image.getHeight(), image,
                    PotatoConfig.textures(), "standalone");
        } catch (Throwable t) {
            PotatoPvP.LOGGER.warn("[Potato PvP] Skipped reducing a standalone texture", t);
        }
    }
}
