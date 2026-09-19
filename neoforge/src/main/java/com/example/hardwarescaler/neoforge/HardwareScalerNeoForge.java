package com.example.hardwarescaler.neoforge;

import com.example.hardwarescaler.ConfigManager;
import com.example.hardwarescaler.HardwareScaler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = HardwareScalerNeoForge.MOD_ID, dist = Dist.CLIENT)
public class HardwareScalerNeoForge {

    public static final String MOD_ID = "hardwarescaler";

    private final HardwareScaler.Tier tier;
    private boolean applied = false;

    public HardwareScalerNeoForge(IEventBus modEventBus) {
        ConfigManager.setConfigDir(FMLPaths.CONFIGDIR.get());

        tier = HardwareScaler.detect();

        // NeoForge has no direct equivalent of Fabric's CLIENT_STARTED, so the
        // preset is applied on the first client tick instead -- by then the
        // options screen state is fully loaded, same as on Fabric.
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        if (applied) {
            return;
        }
        applied = true;
        HardwareScaler.applyPreset(tier);
    }
}
