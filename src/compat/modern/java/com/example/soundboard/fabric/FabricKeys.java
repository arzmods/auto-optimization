package com.example.soundboard.fabric;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/** Fabric API renamed its key helper in 26.x; this is the 26.x one. */
final class FabricKeys {

    private FabricKeys() {
    }

    static void register(KeyMapping key) {
        KeyMappingHelper.registerKeyMapping(key);
    }
}
