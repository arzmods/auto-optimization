
package com.example.hardwarescaler;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;

import java.util.List;

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
        List<GraphicsCard> gpus = hardware.getGraphicsCards();

        int cpuCores = processor.getLogicalProcessorCount();
        long totalRamBytes = memory.getTotal();
        long totalRamGB = totalRamBytes / (1024L * 1024L * 1024L);

        boolean hasDiscreteGpu = hasDiscreteGpu(gpus);
        String gpuName = gpus.isEmpty() ? "Unknown" : gpus.get(0).getName();

        Tier tier = detectTier(cpuCores, totalRamGB, hasDiscreteGpu);

        System.out.println("[HardwareScaler] Detected: " + cpuCores + " logical cores, " + totalRamGB + " GB RAM");
        System.out.println("[HardwareScaler] GPU: " + gpuName + " (discrete: " + hasDiscreteGpu + ")");
        System.out.println("[HardwareScaler] Assigned performance tier: " + tier);

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> applyPreset(tier));
    }

    private boolean hasDiscreteGpu(List<GraphicsCard> gpus) {
        for (GraphicsCard gpu : gpus) {
            String name = gpu.getName().toLowerCase();
            if (name.contains("intel") && (name.contains("hd") || name.contains("uhd") || name.contains("iris"))) {
                continue;
            }
            if (name.contains("microsoft basic")) {
                continue;
            }
            if (name.contains("amd radeon(tm) graphics") || name.contains("vega")) {
                continue;
            }
            if (name.contains("nvidia") || name.contains("geforce") || name.contains("rtx") || name.contains("gtx")
                    || name.contains("radeon rx") || name.contains("arc")) {
                return true;
            }
        }
        return false;
    }

    private Tier detectTier(int cpuCores, long totalRamGB, boolean hasDiscreteGpu) {
        int score = 0;

        if (cpuCores >= 12) score += 3;
        else if (cpuCores >= 6) score += 2;
        else score += 1;

        if (totalRamGB >= 32) score += 3;
        else if (totalRamGB >= 16) score += 2;
        else score += 1;

        if (hasDiscreteGpu) score += 3;
        else score += 1;

        if (score <= 4) {
            return Tier.LOW;
        } else if (score <= 7) {
            return Tier.MEDIUM;
        } else {
            return Tier.HIGH;
        }
    }

    private void applyPreset(Tier tier) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) {
            System.out.println("[HardwareScaler] Client not ready yet, skipping preset application");
            return;
        }

        switch (tier) {
            case LOW:
                client.options.renderDistance().set(6);
                System.out.println("[HardwareScaler] Applying LOW preset (render distance 6)");
                break;
            case MEDIUM:
                client.options.renderDistance().set(10);
                System.out.println("[HardwareScaler] Applying MEDIUM preset (render distance 10)");
                break;
            case HIGH:
                client.options.renderDistance().set(16);
                System.out.println("[HardwareScaler] Applying HIGH preset (render distance 16)");
                break;
        }

        client.options.save();
    }
}
