package com.example.hardwarescaler;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public class HardwareScalerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ConfigManager.setConfigDir(FabricLoader.getInstance().getConfigDir());

        HardwareScaler.Tier tier = HardwareScaler.detect();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> HardwareScaler.applyPreset(tier));
    }
}
