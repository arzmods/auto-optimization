package com.example.soundboard;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.nio.file.Path;

/**
 * The few calls that were renamed between Minecraft 1.x and 26.x. This copy is for 1.x;
 * src/compat/modern has the 26.x one, and build.gradle compiles only the one that matches
 * minecraft_version.
 */
public final class McCompat {

    private McCompat() {
    }

    /** A key in the Controls menu, under "Miscellaneous". */
    public static KeyMapping registerKey(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyMapping(name, InputConstants.Type.KEYSYM, key, "key.categories.misc"));
    }

    public static boolean hasScreenOpen(Minecraft minecraft) {
        return minecraft.screen != null;
    }

    public static void openScreen(Minecraft minecraft, Screen screen) {
        minecraft.setScreen(screen);
    }

    public static void openFolder(Path folder) {
        Util.getPlatform().openFile(folder.toFile());
    }
}
