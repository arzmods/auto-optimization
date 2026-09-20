package com.arzmods.potatopvp.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * The Z key. Created here so both loaders share one definition; each loader
 * registers it its own way.
 */
public final class PotatoKeys {

    public static final String CATEGORY = "key.categories.potatopvp";

    /**
     * Key code for Z.
     *
     * <p>Not taken from org.lwjgl.glfw any more: Minecraft 26.x ships
     * lwjgl-sdl instead of lwjgl-glfw, so that package is not on the
     * classpath at all. The numeric value is the one Minecraft's own input
     * layer uses for Z.
     */
    public static final int KEY_Z = 90;

    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.potatopvp.open_menu",
            KEY_Z,
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
