package com.example.soundboard;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** The J key. Each loader registers {@link #OPEN} its own way and calls {@link #onClientTick}. */
public final class SoundboardKeys {

    public static final KeyMapping OPEN = McCompat.newKey("key.soundboardmc.open", InputConstants.KEY_J);

    private SoundboardKeys() {
    }

    public static void onClientTick(Minecraft minecraft) {
        while (OPEN.consumeClick()) {
            if (!McCompat.hasScreenOpen(minecraft)) {
                McCompat.openScreen(minecraft, new SoundboardScreen());
            }
        }
    }
}
