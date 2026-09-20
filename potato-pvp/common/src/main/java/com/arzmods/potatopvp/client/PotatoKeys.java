package com.arzmods.potatopvp.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * The Z key. Created here so both loaders share one definition; each loader
 * registers it its own way.
 */
public final class PotatoKeys {

    public static final String CATEGORY = "key.categories.potatopvp";

    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.potatopvp.open_menu",
            GLFW.GLFW_KEY_Z,
            CATEGORY);

    private PotatoKeys() {
    }

    /** Called from each loader's client tick hook. */
    public static void handleClientTick(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        while (OPEN_MENU.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new PotatoOptionsScreen(null));
            }
        }
    }
}
