package com.example.jukeboxhits.forge;

import com.example.jukeboxhits.core.JukeboxCore;
import com.example.jukeboxhits.core.SongCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Forge entrypoint (Minecraft 1.21+). All the actual work lives in {@link JukeboxCore}.
 *
 * Forge for 1.20.1 and earlier will NOT work with this mod - the jukebox_song registry
 * this relies on did not exist before 1.21. See loaders/README.md.
 *
 * This file is not part of the Fabric build.
 */
@Mod(JukeboxCore.MOD_ID)
public class JukeboxForge {

    public JukeboxForge() {
        JukeboxCore.init(FMLPaths.CONFIGDIR.get());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        SongCommand.register(event.getDispatcher());
    }
}
