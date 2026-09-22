package com.arzmods.potatopvp.client;

import com.arzmods.potatopvp.PotatoConfig;
import com.arzmods.potatopvp.QualityLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * Decides whether a single particle is allowed to spawn.
 *
 * <p>Particles are the cheapest thing to throw away on a weak machine, but a
 * few of them are how you read a fight - a crit puff, a totem pop, a wind
 * burst. So MINIMUM keeps that short list and bins everything else.
 */
public final class ParticleFilter {

    /**
     * Particles that carry information you fight with. Kept on MINIMUM.
     * Names are the path of the particle's registry id, e.g. "minecraft:crit".
     */
    private static final Set<String> COMBAT_CRITICAL = Set.of(
            // hit feedback
            "crit",
            "enchanted_hit",
            "damage_indicator",
            "sweep_attack",
            // "I just survived" / "they just survived"
            "totem_of_undying",
            // explosions you need to react to
            "explosion",
            "explosion_emitter",
            // potions: seeing a lingering cloud is the difference between living and not
            "effect",
            "entity_effect",
            "instant_effect",
            "dragon_breath",
            // ranged tells
            "sonic_boom",
            "fishing",
            "flash"
    );

    /**
     * Ambient decoration that spawns constantly and costs the most frames.
     * Binned even on MEDIUM.
     */
    private static final Set<String> AMBIENT_SPAM = Set.of(
            "smoke", "large_smoke", "campfire_cosy_smoke", "campfire_signal_smoke", "white_smoke",
            "cloud", "sneeze",
            "rain", "splash", "underwater", "bubble", "bubble_column_up", "bubble_pop", "current_down",
            "dripping_water", "falling_water", "dripping_lava", "falling_lava", "landing_lava",
            "dripping_honey", "falling_honey", "landing_honey",
            "dripping_obsidian_tear", "falling_obsidian_tear", "landing_obsidian_tear",
            "dripping_dripstone_water", "falling_dripstone_water",
            "dripping_dripstone_lava", "falling_dripstone_lava",
            "mycelium", "portal", "reverse_portal", "nautilus",
            "spore_blossom_air", "falling_spore_blossom", "warped_spore", "crimson_spore",
            "ash", "white_ash", "soul", "snowflake", "firefly",
            "cherry_leaves", "pale_oak_leaves", "tinted_leaves", "falling_dust",
            "note", "composter", "happy_villager", "angry_villager", "heart",
            "end_rod", "dolphin", "squid_ink", "glow", "glow_squid_ink",
            "ambient_entity_effect", "witch", "enchant", "dust_plume",
            "vault_connection", "trial_spawner_detection", "trial_spawner_detection_ominous",
            "ominous_spawning", "infested", "item_cobweb", "raid_omen", "trial_omen"
    );

    /**
     * The mace smash and wind charge visuals. These are the ground slam you get
     * when you fall onto someone with a mace, and they fill the screen at the
     * exact moment you need to see what is happening. Dropped on None and
     * Minimum, kept on Medium.
     */
    private static final Set<String> MACE_AND_WIND = Set.of(
            "gust",
            "small_gust",
            "gust_emitter_large",
            "gust_emitter_small",
            "wind_burst",
            "smash_attack",
            "shockwave"
    );

    private static final String TOTEM = "totem_of_undying";

    /**
     * A vanilla totem pop throws out thirty particles at once and fills the
     * screen. On MINIMUM we let a couple through - enough to see that someone
     * popped, not enough to blind you while you are still swinging.
     */
    private static final int TOTEM_PARTICLE_BUDGET = 2;

    /** Particles from one pop all arrive in the same tick; this groups them. */
    private static final long TOTEM_BURST_WINDOW_MS = 500L;

    private static long totemBurstStart;
    private static int totemBurstCount;

    private ParticleFilter() {
    }

    /**
     * @return true when the particle may be created, false when it should be dropped.
     */
    public static boolean allows(ParticleOptions options) {
        QualityLevel level = PotatoConfig.particles();

        if (level == QualityLevel.NONE) {
            return false;
        }

        String path = pathOf(options);
        if (path == null) {
            // Unknown or modded particle: strict on MINIMUM, lenient on MEDIUM.
            return level == QualityLevel.MEDIUM;
        }

        if (level == QualityLevel.MINIMUM) {
            if (MACE_AND_WIND.contains(path)) {
                return false;
            }
            if (!COMBAT_CRITICAL.contains(path)) {
                return false;
            }
            return !TOTEM.equals(path) || allowTotemParticle();
        }

        // MEDIUM: everything except the constant ambient drizzle.
        return !AMBIENT_SPAM.contains(path);
    }

    /**
     * Lets the first few particles of a totem burst through and drops the rest.
     * The window resets once the burst is over, so the next pop gets its own
     * small allowance.
     */
    private static boolean allowTotemParticle() {
        long now = System.currentTimeMillis();
        if (now - totemBurstStart > TOTEM_BURST_WINDOW_MS) {
            totemBurstStart = now;
            totemBurstCount = 0;
        }
        return ++totemBurstCount <= TOTEM_PARTICLE_BUDGET;
    }

    /** Registry path of the particle type, or null if it cannot be resolved. */
    private static String pathOf(ParticleOptions options) {
        try {
            Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
            return id == null ? null : id.getPath();
        } catch (Throwable ignored) {
            // Registries are not always ready this early; treat as unknown.
            return null;
        }
    }
}
