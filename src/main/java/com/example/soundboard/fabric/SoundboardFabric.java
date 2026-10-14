package com.example.soundboard.fabric;

import com.example.soundboard.Soundboard;
import com.example.soundboard.SoundboardKeys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entrypoint. Everything else is shared with the NeoForge build. */
public class SoundboardFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Soundboard.init(FabricLoader.getInstance().getGameDir());
        FabricKeys.register(SoundboardKeys.OPEN);
        ClientTickEvents.END_CLIENT_TICK.register(SoundboardKeys::onClientTick);
    }
}
