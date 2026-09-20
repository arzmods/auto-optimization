package com.arzmods.potatopvp.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * The Z key. Created here so both loaders share one definition; each loader
 * registers it its own way.
 */
public final class PotatoKeys {

    public static final String CATEGORY = "key.categories.potatopvp";

    /**
     * Taken from Minecraft's own input constants rather than org.lwjgl.glfw:
     * 26.x ships lwjgl-sdl and no lwjgl-glfw, so that package is not on the
     * classpath at all.
     */
    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.potatopvp.open_menu",
            InputConstants.KEY_Z,
            CATEGORY);

    private PotatoKeys() {
    }

    /**
     * Called from each loader's client tick hook.
     *
     * <p>No "is a screen already open" guard: key mappings only accumulate
     * clicks while the player is actually in the world, so a queued press
     * cannot arrive while a menu is up.
     */
    public static void handleClientTick(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        while (OPEN_MENU.consumeClick()) {
            minecraft.setScreenAndShow(new PotatoOptionsScreen(null));
        }
    }
}
