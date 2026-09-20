package com.arzmods.potatopvp.fabric;

import com.arzmods.potatopvp.PotatoPvP;
import com.arzmods.potatopvp.client.PotatoKeys;
import com.arzmods.potatopvp.client.PotatoOptions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric entrypoint. Everything interesting lives in the shared module - this
 * only wires up the three things Fabric does its own way: where the config
 * folder is, how a key is registered, and how to get a tick.
 */
public class PotatoPvPFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PotatoPvP.onInitialize(FabricLoader.getInstance().getConfigDir());

        KeyBindingHelper.registerKeyBinding(PotatoKeys.OPEN_MENU);

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> PotatoOptions.applyAll());
        ClientTickEvents.END_CLIENT_TICK.register(PotatoKeys::handleClientTick);
    }
}
