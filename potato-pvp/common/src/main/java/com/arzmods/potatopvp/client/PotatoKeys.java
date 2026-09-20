package com.arzmods.potatopvp.client;

import com.arzmods.potatopvp.PotatoPvP;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * The Z key. Created here so both loaders share one definition; each loader
 * registers it its own way.
 */
public final class PotatoKeys {

    /**
     * Key categories are no longer a plain translation key - 26.3 wants a
     * registered Category built from an Identifier.
     */
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(PotatoPvP.MOD_ID, "main"));

    /**
     * Key code comes from Minecraft's own input constants rather than
     * org.lwjgl.glfw: 26.x ships lwjgl-sdl and no lwjgl-glfw, so that package
     * is not on the classpath at all.
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

    /**
     * Adds the key to the game's own list, without going through a loader API.
     *
     * <p>Fabric API moved its key binding helper out of
     * {@code net.fabricmc.fabric.api.client.keybinding.v1} and doing it by hand
     * is both simpler and loader independent. The array is found by type rather
     * than by field name so a rename cannot break it.
     *
     * <p>Safe to call more than once - it checks before adding.
     */
    public static void registerInto(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        try {
            for (Field field : Options.class.getDeclaredFields()) {
                if (field.getType() != KeyMapping[].class) {
                    continue;
                }
                field.setAccessible(true);
                KeyMapping[] existing = (KeyMapping[]) field.get(minecraft.options);
                if (existing == null) {
                    continue;
                }
                for (KeyMapping mapping : existing) {
                    if (mapping == OPEN_MENU) {
                        return; // already in
                    }
                }
                KeyMapping[] updated = Arrays.copyOf(existing, existing.length + 1);
                updated[existing.length] = OPEN_MENU;
                field.set(minecraft.options, updated);
                return;
            }
            PotatoPvP.LOGGER.warn("[Potato PvP] Could not find the key mapping list; "
                    + "Z will not be listed under Controls. Bind it there if it does not work.");
        } catch (Throwable t) {
            PotatoPvP.LOGGER.warn("[Potato PvP] Could not register the Z key", t);
        }
    }
}
