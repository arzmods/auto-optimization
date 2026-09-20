package com.arzmods.potatopvp.mixin;

import com.arzmods.potatopvp.PotatoConfig;
import com.arzmods.potatopvp.QualityLevel;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Kills the giant spinning totem that covers the screen when one pops.
 *
 * <p>The particles are only half of a totem pop; the other half is a full
 * screen item animation that runs for two seconds. Dropping it is the single
 * biggest thing you can do to make a pop low profile, and it is the half you
 * least want in your face while you are still mid fight.
 *
 * <p>Only the animation goes. You still get the sound, and on MINIMUM you still
 * get a couple of particles, so you always know a pop happened.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    /**
     * Declares no target arguments on purpose, so it binds regardless of what
     * this method's parameter list looks like on a given Minecraft version.
     */
    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true, require = 0)
    private void potatopvp$skipTotemOverlay(CallbackInfo ci) {
        if (PotatoConfig.particles() != QualityLevel.MEDIUM) {
            ci.cancel();
        }
    }
}
