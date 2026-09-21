package com.example.jukeboxhits.fabric;

import com.example.jukeboxhits.core.JukeboxCore;
import com.example.jukeboxhits.core.SongCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entrypoint. All the actual work lives in {@link JukeboxCore}. */
public class JukeboxFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        JukeboxCore.init(FabricLoader.getInstance().getConfigDir());

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> SongCommand.register(dispatcher));
    }
}
