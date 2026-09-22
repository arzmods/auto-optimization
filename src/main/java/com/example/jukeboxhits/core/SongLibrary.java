package com.example.jukeboxhits.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The code -> song table, owned by the server and persisted to disk.
 *
 * This is the piece that makes the mod feel like Roblox: codes are short, stable, and
 * shared by everyone on the server.
 */
public final class SongLibrary {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final Map<Integer, SongEntry> byCode = new TreeMap<>();

    public SongLibrary(Path file) {
        this.file = file;
    }

    public synchronized void load() {
        byCode.clear();
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Map<String, SongEntry> raw = GSON.fromJson(reader,
                    new TypeToken<LinkedHashMap<String, SongEntry>>() {
                    }.getType());
            if (raw == null) {
                return;
            }
            for (SongEntry entry : raw.values()) {
                if (entry != null && entry.code > 0) {
                    byCode.put(entry.code, entry);
                }
            }
        } catch (Exception e) {
            JukeboxCore.LOGGER.error("Could not read the song library at {}", file, e);
        }
        JukeboxCore.LOGGER.info("Song library loaded with {} song(s)", byCode.size());
    }

    public synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            Map<String, SongEntry> raw = new LinkedHashMap<>();
            for (Map.Entry<Integer, SongEntry> e : byCode.entrySet()) {
                raw.put(String.valueOf(e.getKey()), e.getValue());
            }
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(raw, writer);
            }
        } catch (IOException e) {
            JukeboxCore.LOGGER.error("Could not save the song library to {}", file, e);
        }
    }

    /** Lowest unused code, so codes stay short and memorable rather than sparse. */
    public synchronized int nextFreeCode() {
        int code = 1;
        while (byCode.containsKey(code)) {
            code++;
        }
        return code;
    }

    public synchronized void put(SongEntry entry) {
        byCode.put(entry.code, entry);
        save();
    }

    public synchronized SongEntry remove(int code) {
        SongEntry removed = byCode.remove(code);
        if (removed != null) {
            save();
        }
        return removed;
    }

    public synchronized SongEntry byCode(int code) {
        return byCode.get(code);
    }

    public synchronized boolean has(int code) {
        return byCode.containsKey(code);
    }

    public synchronized int size() {
        return byCode.size();
    }

    public synchronized List<SongEntry> all() {
        return new ArrayList<>(byCode.values());
    }

    public synchronized List<SongEntry> search(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<SongEntry> hits = new ArrayList<>();
        for (SongEntry entry : byCode.values()) {
            if (entry.label().toLowerCase(Locale.ROOT).contains(needle)) {
                hits.add(entry);
            }
        }
        hits.sort(Comparator.comparingInt(a -> a.code));
        return hits;
    }
}
