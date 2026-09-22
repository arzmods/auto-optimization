package com.arzmods.potatopvp.mixin;

import com.arzmods.potatopvp.PotatoPvP;
import com.arzmods.potatopvp.client.PotatoAnimations;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Slows or stops the end crystal spin, for crystal PvP.
 *
 * <p>A crystal's spin and bob are both driven by the age its render state is
 * handed each frame, so scaling that one number is enough. Nothing about the
 * model, the renderer or the hitbox changes - a frozen crystal still sits
 * exactly where you placed it and still blows up the same way.
 *
 * <p>This deliberately hooks the base renderer rather than the crystal's own.
 * Both {@link Entity} and {@link EntityRenderState} are stable, widely used
 * types, whereas naming the crystal's entity class would be one more guess.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
    private void potatopvp$slowCrystals(Entity entity, EntityRenderState state, float partialTick,
                                        CallbackInfo ci) {
        try {
            if (state instanceof EndCrystalRenderState) {
                state.ageInTicks = PotatoAnimations.crystalAge(state.ageInTicks);
            }
        } catch (Throwable t) {
            PotatoPvP.LOGGER.warn("[Potato PvP] Could not slow a crystal", t);
        }
    }
}
