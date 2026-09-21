package com.example.jukeboxhits.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Everything the jukebox does, with no loader-specific imports.
 *
 * Fabric, NeoForge and Forge each have a small entrypoint class that calls
 * {@link #init(Path)} and hands the command tree to their own registration hook.
 * Nothing else differs between loaders.
 */
public final class JukeboxCore {

    public static final String MOD_ID = "jukeboxhits";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static JukeboxConfig config = new JukeboxConfig();
    private static Path configDir = Path.of("config");

    private JukeboxCore() {
    }

    /** @param loaderConfigDir the platform's config directory. */
    public static void init(Path loaderConfigDir) {
        configDir = loaderConfigDir;
        reloadConfig();
        SongIndex.load();
        LOGGER.info("Jukebox Hits ready with {} song(s)", SongIndex.count());
    }

    public static JukeboxConfig config() {
        return config;
    }

    public static void reloadConfig() {
        config = JukeboxConfig.load(configDir);
    }
}
