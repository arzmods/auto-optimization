package com.example.soundboard.neoforge;

import com.example.soundboard.Soundboard;
import com.example.soundboard.SoundboardKeys;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge entrypoint. Everything else is shared with the Fabric build. */
@Mod(value = Soundboard.MOD_ID, dist = Dist.CLIENT)
public class SoundboardNeoForge {

    public SoundboardNeoForge(IEventBus modBus) {
        Soundboard.init(FMLPaths.GAMEDIR.get());
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> event.register(SoundboardKeys.OPEN));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class,
                event -> SoundboardKeys.onClientTick(Minecraft.getInstance()));
    }
}
