package com.example.jukeboxhits.neoforge;

import com.example.jukeboxhits.core.JukeboxCore;
import com.example.jukeboxhits.core.SongCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * NeoForge entrypoint (Minecraft 1.21+). All the actual work lives in {@link JukeboxCore}.
 *
 * This file is not part of the Fabric build. See loaders/README.md.
 */
@Mod(JukeboxCore.MOD_ID)
public class JukeboxNeoForge {

    public JukeboxNeoForge() {
        JukeboxCore.init(FMLPaths.CONFIGDIR.get());
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        SongCommand.register(event.getDispatcher());
    }
}
