package com.arzmods.potatopvp.mixin;

import com.arzmods.potatopvp.PotatoConfig;
import com.arzmods.potatopvp.QualityLevel;
import com.arzmods.potatopvp.client.ParticleFilter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops particles before they are ever built.
 *
 * <p>Two hooks on purpose. The first one knows <i>which</i> particle is being
 * asked for, so it can keep the handful that matter in a fight. The second is
 * a catch-all for the "off" setting that sits on a much simpler method
 * signature, so the default preset still works even if the first hook fails to
 * bind on a future Minecraft version.
 */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    /** Type aware: this is the one that implements None / Minimum / Medium. */
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void potatopvp$filterByType(ParticleOptions options, double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed,
                                        CallbackInfoReturnable<Particle> cir) {
        if (!ParticleFilter.allows(options)) {
            cir.setReturnValue(null);
        }
    }

    /** Catch-all: every particle in the game ends up here before it is drawn. */
    @Inject(method = "add", at = @At("HEAD"), cancellable = true, require = 0)
    private void potatopvp$blockAll(Particle particle, CallbackInfo ci) {
        if (PotatoConfig.particles() == QualityLevel.NONE) {
            ci.cancel();
        }
    }
}
