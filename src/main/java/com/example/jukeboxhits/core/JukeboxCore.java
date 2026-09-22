package com.example.jukeboxhits.core;

import com.example.jukeboxhits.core.audio.AudioStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Shared state and setup, with no loader-specific imports.
 *
 * Fabric, NeoForge and Forge each have a small entrypoint that calls {@link #init(Path)}
 * and wires up two hooks: command registration and right-click-on-block.
 */
public final class JukeboxCore {

    public static final String MOD_ID = "jukeboxhits";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static JukeboxConfig config = new JukeboxConfig();
    private static SongLibrary library;
    private static AudioStore store;
    private static Path root;

    private JukeboxCore() {
    }

    /** @param configDir the platform's config directory. */
    public static void init(Path configDir) {
        root = configDir.resolve(MOD_ID);
        config = JukeboxConfig.load(root.resolve("config.json"));
        library = new SongLibrary(root.resolve("songs.json"));
        library.load();
        store = new AudioStore(root.resolve("songs"), config.maxUploadBytes());
        LOGGER.info("Jukebox Hits ready: {} song(s), files in {}",
                library.size(), store.directory());
    }

    public static JukeboxConfig config() {
        return config;
    }

    public static SongLibrary library() {
        return library;
    }

    public static AudioStore store() {
        return store;
    }

    public static void reload() {
        config = JukeboxConfig.load(root.resolve("config.json"));
        store = new AudioStore(root.resolve("songs"), config.maxUploadBytes());
        library.load();
    }
}
