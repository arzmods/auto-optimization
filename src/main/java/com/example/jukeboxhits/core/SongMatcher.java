package com.example.jukeboxhits.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Finds a song from whatever a player typed: a code ("7") or a name ("midnight drive").
 *
 * Matching is forgiving on purpose - case, punctuation and extra spaces are ignored, and a
 * unique start of a name is enough - so players do not have to type titles exactly.
 * Pure logic with no Minecraft imports, so it is unit tested standalone.
 */
public final class SongMatcher {

    /** Exactly one of song / candidates is meaningful. Both empty means nothing matched. */
    public record Match(SongEntry song, List<SongEntry> candidates) {
        public boolean found() {
            return song != null;
        }

        public boolean ambiguous() {
            return song == null && candidates.size() > 1;
        }
    }

    private SongMatcher() {
    }

    public static Match find(List<SongEntry> songs, String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return new Match(null, List.of());
        }

        if (q.chars().allMatch(Character::isDigit)) {
            for (SongEntry song : songs) {
                if (String.valueOf(song.code).equals(q.replaceFirst("^0+(?=.)", ""))) {
                    return new Match(song, List.of());
                }
            }
            return new Match(null, List.of());
        }

        String needle = normalize(q);
        if (needle.isEmpty()) {
            return new Match(null, List.of());
        }

        List<SongEntry> exact = new ArrayList<>();
        List<SongEntry> prefix = new ArrayList<>();
        List<SongEntry> contains = new ArrayList<>();
        for (SongEntry song : songs) {
            String title = normalize(song.title);
            String label = normalize(song.label());
            if (title.equals(needle) || label.equals(needle)) {
                exact.add(song);
            } else if (title.startsWith(needle) || label.startsWith(needle)) {
                prefix.add(song);
            } else if (title.contains(needle) || label.contains(needle)) {
                contains.add(song);
            }
        }

        for (List<SongEntry> tier : List.of(exact, prefix, contains)) {
            if (tier.size() == 1) {
                return new Match(tier.get(0), List.of());
            }
            if (tier.size() > 1) {
                return new Match(null, tier);
            }
        }
        return new Match(null, List.of());
    }

    /** True if another song already has this exact name, so two songs cannot clash. */
    public static boolean isNameTaken(List<SongEntry> songs, String name) {
        String wanted = normalize(name);
        for (SongEntry song : songs) {
            if (normalize(song.title).equals(wanted)) {
                return true;
            }
        }
        return false;
    }

    /** Turns "midnight_drive-final.mp3" into "midnight drive final". */
    public static String nameFromFile(String fileName) {
        String base = fileName;
        int dot = base.lastIndexOf('.');
        if (dot >= 0) {
            base = base.substring(0, dot);
        }
        String cleaned = base.replaceAll("[_\\-]+", " ").replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? "untitled" : cleaned;
    }

    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }
}
