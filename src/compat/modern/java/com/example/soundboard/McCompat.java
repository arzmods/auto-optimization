package com.example.soundboard;

import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.nio.file.Path;

/**
 * The few calls that were renamed between Minecraft 1.x and 26.x. This copy is for 26.x;
 * src/compat/legacy has the 1.x one, and build.gradle compiles only the one that matches
 * minecraft_version.
 */
public final class McCompat {

    private McCompat() {
    }

    /** A key for the Controls menu, under "Miscellaneous". The loader still has to register it. */
    public static KeyMapping newKey(String name, int key) {
        return new KeyMapping(name, key, KeyMapping.Category.MISC);
    }

    public static boolean hasScreenOpen(Minecraft minecraft) {
        return minecraft.gui.screen() != null;
    }

    public static void openScreen(Minecraft minecraft, Screen screen) {
        minecraft.gui.setScreen(screen);
    }

    public static void openFolder(Path folder) {
        Blaze3D.openPath(folder);
    }
}
