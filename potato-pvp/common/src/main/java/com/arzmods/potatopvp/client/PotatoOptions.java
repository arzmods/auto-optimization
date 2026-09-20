package com.arzmods.potatopvp.client;

import com.arzmods.potatopvp.PotatoConfig;
import com.arzmods.potatopvp.PotatoPvP;
import com.arzmods.potatopvp.QualityLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

/**
 * Pushes the three Potato PvP settings down into Minecraft's own video options.
 *
 * <p>The mixins do the heavy lifting, but a lot of cheap wins are just vanilla
 * options that nobody turns off by hand: view bobbing, entity shadows, the
 * enchantment glint scroll, the nausea warp. This is where those get set.
 *
 * <p>Heads up: these are the same options the vanilla video settings screen
 * writes, so changing a Potato PvP setting really does change your video
 * settings. That is intentional - it is the point of a preset.
 */
public final class PotatoOptions {

    private PotatoOptions() {
    }

    public static void applyAll() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        Options options = minecraft.options;

        applyParticles(options, PotatoConfig.particles());
        applyTextures(options, PotatoConfig.textures());
        applyAnimations(options, PotatoConfig.animations());

        options.save();
        PotatoPvP.LOGGER.info("[Potato PvP] applied - particles: {}, textures: {}, animations: {}",
                PotatoConfig.particles(), PotatoConfig.textures(), PotatoConfig.animations());
    }

    private static void applyParticles(Options options, QualityLevel level) {
        switch (level) {
            case NONE, MINIMUM -> options.particles().set(ParticleStatus.MINIMAL);
            case MEDIUM -> options.particles().set(ParticleStatus.DECREASED);
        }
    }

    private static void applyTextures(Options options, QualityLevel level) {
        switch (level) {
            case NONE, MINIMUM -> {
                options.mipmapLevels().set(0);
                options.ambientOcclusion().set(false);
            }
            case MEDIUM -> {
                options.mipmapLevels().set(1);
                options.ambientOcclusion().set(true);
            }
        }
    }

    private static void applyAnimations(Options options, QualityLevel level) {
        switch (level) {
            case NONE -> {
                options.bobView().set(false);
                options.entityShadows().set(false);
                options.glintSpeed().set(0.0);
                options.glintStrength().set(0.0);
                options.screenEffectScale().set(0.0);
                options.fovEffectScale().set(0.0);
                options.damageTiltStrength().set(0.0);
            }
            case MINIMUM -> {
                options.bobView().set(false);
                options.entityShadows().set(false);
                options.glintSpeed().set(0.0);
                options.glintStrength().set(0.5);
                options.screenEffectScale().set(0.0);
                options.fovEffectScale().set(0.0);
                options.damageTiltStrength().set(0.0);
            }
            case MEDIUM -> {
                options.bobView().set(true);
                options.entityShadows().set(true);
                options.glintSpeed().set(0.5);
                options.glintStrength().set(0.75);
                options.screenEffectScale().set(0.5);
                options.fovEffectScale().set(0.5);
                options.damageTiltStrength().set(0.5);
            }
        }
    }
}
