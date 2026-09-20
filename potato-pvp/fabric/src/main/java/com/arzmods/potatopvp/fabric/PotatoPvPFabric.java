package com.arzmods.potatopvp.fabric;

import com.arzmods.potatopvp.PotatoPvP;
import com.arzmods.potatopvp.client.PotatoKeys;
import com.arzmods.potatopvp.client.PotatoOptions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * Fabric entrypoint. Everything interesting lives in the shared module - this
 * only wires up the three things Fabric does its own way: where the config
 * folder is, how a key is registered, and how to get a tick.
 */
public class PotatoPvPFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PotatoPvP.onInitialize(FabricLoader.getInstance().getConfigDir());

        // Registered by hand rather than through Fabric API, which moved this
        // helper. Done twice because options may not exist yet at init time,
        // and again once the client is up; the call is idempotent.
        PotatoKeys.registerInto(Minecraft.getInstance());

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            PotatoKeys.registerInto(client);
            PotatoOptions.applyAll();
        });
        ClientTickEvents.END_CLIENT_TICK.register(PotatoKeys::handleClientTick);
    }
}
