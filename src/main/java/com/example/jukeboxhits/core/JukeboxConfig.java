package com.example.jukeboxhits.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Written to <config>/jukeboxhits/config.json on first launch. */
public class JukeboxConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Largest file /song upload will fetch, in megabytes. */
    public int maxUploadMb = 20;

    /** Refuse new uploads past this many songs, so disk use stays bounded. */
    public int maxSongs = 500;

    /** How far the music carries, in blocks. */
    public float playbackDistance = 48.0f;

    /** Volume multiplier applied when decoding. 1.0 leaves the audio untouched. */
    public double gain = 1.0d;

    /** When false, only operators can run /song upload and /song remove. */
    public boolean allowAllPlayersUpload = false;

    /** When false, only operators can get discs for existing codes. */
    public boolean allowAllPlayersDisc = true;

    /** Item handed out by /song disc. Any music disc works. */
    public String discItem = "minecraft:music_disc_13";

    /**
     * Vanilla give command used to build the disc. %player%, %disc% and %name% are
     * substituted, and vanilla parses the result - so if item-component syntax changes in
     * a future Minecraft version this is a config edit rather than a recompile.
     *
     * Older syntax, if your version rejects the default:
     *   give %player% %disc%{display:{Name:%name%}}
     */
    public String giveCommand = "give %player% %disc%[minecraft:custom_name=%name%]";

    public static JukeboxConfig load(Path file) {
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JukeboxConfig loaded = GSON.fromJson(reader, JukeboxConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (IOException e) {
                JukeboxCore.LOGGER.warn("Could not read the config, using defaults", e);
            }
        }
        JukeboxConfig fresh = new JukeboxConfig();
        fresh.save(file);
        return fresh;
    }

    public void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            JukeboxCore.LOGGER.warn("Could not write the config", e);
        }
    }

    public long maxUploadBytes() {
        return (long) Math.max(1, maxUploadMb) * 1024L * 1024L;
    }
}
