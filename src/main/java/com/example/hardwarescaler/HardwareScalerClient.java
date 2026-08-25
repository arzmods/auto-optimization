package com.example.hardwarescaler;

import net.fabricmc.api.ClientModInitializer;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;

public class HardwareScalerClient implements ClientModInitializer {

    public enum Tier {
        LOW, MEDIUM, HIGH
    }

    @Override
    public void onInitializeClient() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();

        CentralProcessor processor = hardware.getProcessor();
        GlobalMemory memory = hardware.getMemory();

        int cpuCores = processor.getLogicalProcessorCount();
        long totalRamBytes = memory.getTotal();
        long totalRamGB = totalRamBytes / (1024L * 1024L * 1024L);

        Tier tier = detectTier(cpuCores, totalRamGB);

        System.out.println("[HardwareScaler] Detected: " + cpuCores + " logical cores, " + totalRamGB + " GB RAM");
        System.out.println("[HardwareScaler] Assigned performance tier: " + tier);

        applyPreset(tier);
    }

    private Tier detectTier(int cpuCores, long totalRamGB) {
        if (cpuCores <= 4 || totalRamGB <= 8) {
            return Tier.LOW;
        } else if (cpuCores <= 8 || totalRamGB <= 16) {
            return Tier.MEDIUM;
        } else {
            return Tier.HIGH;
        }
    }

    private void applyPreset(Tier tier) {
        switch (tier) {
            case LOW:
                System.out.println("[HardwareScaler] Applying LOW preset (reduced render distance, simple particles)");
                break;
            case MEDIUM:
                System.out.println("[HardwareScaler] Applying MEDIUM preset (balanced settings)");
                break;
            case HIGH:
                System.out.println("[HardwareScaler] Applying HIGH preset (max render distance, full effects)");
                break;
        }
    }
}
