package com.example.soundboard;

import com.example.soundboard.audio.AudioLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Where songs live: a "soundboard" folder next to mods/ and config/. */
public final class Soundboard {

    public static final String MOD_ID = "soundboardmc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static Path gameDir;

    private Soundboard() {
    }

    /** Called once by the loader entrypoint (Fabric or NeoForge) with the .minecraft folder. */
    public static void init(Path minecraftDir) {
        gameDir = minecraftDir;
        songs(); // creates the folder on first launch so it is easy to find
    }

    public static Path folder() {
        return gameDir.resolve("soundboard");
    }

    /** Every playable file in the folder, sorted by name. Creates the folder if needed. */
    public static List<Path> songs() {
        List<Path> songs = new ArrayList<>();
        try {
            Files.createDirectories(folder());
            try (Stream<Path> files = Files.list(folder())) {
                files.filter(Files::isRegularFile)
                        .filter(f -> AudioLoader.isSupported(f.getFileName().toString()))
                        .sorted(Comparator.comparing(f -> f.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .forEach(songs::add);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read the soundboard folder {}", folder(), e);
        }
        return songs;
    }
}
