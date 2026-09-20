package com.arzmods.potatopvp.neoforge;

import com.arzmods.potatopvp.PotatoPvP;
import com.arzmods.potatopvp.client.PotatoKeys;
import com.arzmods.potatopvp.client.PotatoOptions;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * NeoForge entrypoint. Mirrors the Fabric one: config folder, key registration,
 * client tick. The mod itself is the same shared code either way.
 */
@Mod(value = PotatoPvP.MOD_ID, dist = Dist.CLIENT)
public class PotatoPvPNeoForge {

    public PotatoPvPNeoForge(IEventBus modEventBus) {
        PotatoPvP.onInitialize(FMLPaths.CONFIGDIR.get());
    }

    /** Mod bus: registration that happens while the game is starting up. */
    @EventBusSubscriber(modid = PotatoPvP.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {

        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(PotatoKeys.OPEN_MENU);
        }
    }

    /** Game bus: the per-tick key check. */
    @EventBusSubscriber(modid = PotatoPvP.MOD_ID, value = Dist.CLIENT)
    public static final class GameBusEvents {

        private static boolean appliedOnce;

        private GameBusEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();

            if (!appliedOnce && minecraft != null && minecraft.options != null) {
                appliedOnce = true;
                PotatoOptions.applyAll();
            }

            PotatoKeys.handleClientTick(minecraft);
        }
    }
}
