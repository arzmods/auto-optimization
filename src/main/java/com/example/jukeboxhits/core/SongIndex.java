package com.example.jukeboxhits.core;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Reads the code -> song table baked into the jar at /jukeboxhits/songs.json. */
public final class SongIndex {

    public static final class Song {
        public int code;
        public String id;
        public String title = "";
        public String artist = "";
        public String file = "";
        @SerializedName("length_seconds")
        public int lengthSeconds = 180;
        public boolean enabled = false;

        public String label() {
            if (artist == null || artist.isBlank()) {
                return title == null ? id : title;
            }
            return artist + " - " + title;
        }

        public String songId() {
            return JukeboxCore.MOD_ID + ":" + id;
        }
    }

    private static final class SongFile {
        List<Song> songs = new ArrayList<>();
    }

    private static final Map<Integer, Song> BY_CODE = new LinkedHashMap<>();

    private SongIndex() {
    }

    public static void load() {
        BY_CODE.clear();
        try (InputStream in = SongIndex.class.getResourceAsStream("/jukeboxhits/songs.json")) {
            if (in == null) {
                JukeboxCore.LOGGER.warn("songs.json is missing from the jar - no songs available");
                return;
            }
            SongFile parsed = new Gson()
                    .fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), SongFile.class);
            if (parsed == null || parsed.songs == null) {
                return;
            }
            for (Song song : parsed.songs) {
                if (!song.enabled || song.id == null || song.id.isBlank()) {
                    continue;
                }
                Song clash = BY_CODE.put(song.code, song);
                if (clash != null) {
                    JukeboxCore.LOGGER.warn("Code {} is used twice, keeping '{}'", song.code, song.id);
                }
            }
        } catch (Exception e) {
            JukeboxCore.LOGGER.error("Failed to read songs.json", e);
        }
        JukeboxCore.LOGGER.info("Loaded {} song(s)", BY_CODE.size());
    }

    public static Song byCode(int code) {
        return BY_CODE.get(code);
    }

    public static List<Song> all() {
        return new ArrayList<>(BY_CODE.values());
    }

    public static int count() {
        return BY_CODE.size();
    }

    public static List<Song> search(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<Song> hits = new ArrayList<>();
        for (Song song : BY_CODE.values()) {
            if (song.label().toLowerCase(Locale.ROOT).contains(needle)) {
                hits.add(song);
            }
        }
        return hits;
    }
}
