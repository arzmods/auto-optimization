package com.arzmods.potatopvp.client;

import com.arzmods.potatopvp.PotatoConfig;
import com.arzmods.potatopvp.QualityLevel;
import net.minecraft.resources.ResourceLocation;

/**
 * Decides which block textures are still allowed to animate.
 *
 * <p>An animated sprite is re-uploaded to the GPU every tick it changes frame.
 * A world full of water, lava, fire, portals, sea lanterns and prismarine is
 * doing that constantly, which is exactly the kind of work a weak machine
 * cannot spare.
 */
public final class PotatoAnimations {

    /**
     * Sprites whose animation is worth paying for: moving water and lava read
     * as "this is a fluid, do not walk into it", and fire needs to look like
     * fire. Everything else is decoration.
     */
    private static final String[] GAMEPLAY_ANIMATED = { "water", "lava", "fire", "flow" };

    private PotatoAnimations() {
    }

    /**
     * @return true if this sprite may keep its animation ticker
     */
    public static boolean allowsAnimation(ResourceLocation name) {
        QualityLevel level = PotatoConfig.animations();

        if (level == QualityLevel.NONE) {
            return false; // every animated texture freezes on frame one
        }
        if (level == QualityLevel.MEDIUM) {
            return true; // vanilla behaviour
        }

        // MINIMUM: fluids and fire only.
        if (name == null) {
            return false;
        }
        String path = name.getPath();
        for (String keyword : GAMEPLAY_ANIMATED) {
            if (path.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
