package com.example.jukeboxhits.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Written to <config dir>/jukeboxhits.json on first launch. */
public class JukeboxConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** The item /song hands out. Any music disc works. */
    public String discItem = "minecraft:music_disc_13";

    /**
     * Vanilla give command used to build the disc. %player%, %disc% and %song% are substituted.
     * Vanilla parses this string, so if the item-component syntax changes between Minecraft
     * versions it is a one-line edit here rather than a recompile.
     *
     * Alternate form used by some versions:
     *   give %player% %disc%[minecraft:jukebox_playable="%song%"]
     */
    public String giveCommand =
            "give %player% %disc%[minecraft:jukebox_playable={song:\"%song%\"}]";

    /** When false, /song requires permission level 2 (operator). */
    public boolean allowAllPlayers = false;

    public static JukeboxConfig load(Path configDir) {
        Path file = configDir.resolve("jukeboxhits.json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JukeboxConfig loaded = GSON.fromJson(reader, JukeboxConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (IOException e) {
                JukeboxCore.LOGGER.warn("Could not read jukeboxhits.json, using defaults", e);
            }
        }
        JukeboxConfig fresh = new JukeboxConfig();
        fresh.save(configDir);
        return fresh;
    }

    public void save(Path configDir) {
        Path file = configDir.resolve("jukeboxhits.json");
        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            JukeboxCore.LOGGER.warn("Could not write jukeboxhits.json", e);
        }
    }
}
