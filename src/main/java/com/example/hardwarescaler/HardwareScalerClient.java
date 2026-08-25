package com.example.hardwarescaler;

import net.fabricmc.api.ClientModInitializer;

public class HardwareScalerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("Hardware Scaler loaded!");
    }
}
