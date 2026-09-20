package com.arzmods.potatopvp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Constants shared by the Fabric and the NeoForge build. */
public final class PotatoPvP {

    public static final String MOD_ID = "potatopvp";
    public static final String MOD_NAME = "Potato PvP";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private PotatoPvP() {
    }

    /** Called by both loaders once the config folder is known. */
    public static void onInitialize(java.nio.file.Path configDir) {
        PotatoConfig.init(configDir);
        LOGGER.info("[Potato PvP] loaded - particles: {}, textures: {}, animations: {}",
                PotatoConfig.particles(), PotatoConfig.textures(), PotatoConfig.animations());
    }
}
