package com.example.soundboard;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;

/** Client entrypoint: the J key opens the soundboard. */
public class SoundboardClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Soundboard.songs(); // creates the folder on first launch so it is easy to find

        KeyMapping open = McCompat.registerKey("key.soundboardmc.open", InputConstants.KEY_J);
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            while (open.consumeClick()) {
                if (!McCompat.hasScreenOpen(minecraft)) {
                    McCompat.openScreen(minecraft, new SoundboardScreen());
                }
            }
        });
    }
}
