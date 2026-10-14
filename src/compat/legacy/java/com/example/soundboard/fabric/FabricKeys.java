package com.example.soundboard.fabric;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

/** Fabric API renamed its key helper in 26.x; this is the 1.x one. */
final class FabricKeys {

    private FabricKeys() {
    }

    static void register(KeyMapping key) {
        KeyBindingHelper.registerKeyBinding(key);
    }
}
