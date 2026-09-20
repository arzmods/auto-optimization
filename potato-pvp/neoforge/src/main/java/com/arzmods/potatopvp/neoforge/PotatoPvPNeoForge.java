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

    /**
     * One subscriber for both events. NeoForge's @EventBusSubscriber no longer
     * takes a bus argument - the mod bus and game bus distinction is gone, so
     * key registration and the per-tick check live together.
     */
    @EventBusSubscriber(modid = PotatoPvP.MOD_ID, value = Dist.CLIENT)
    public static final class Events {

        private static boolean appliedOnce;

        private Events() {
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(PotatoKeys.OPEN_MENU);
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
