package com.example.jukeboxhits.core;

import java.util.List;

public class SongMatcherTest {

    private static int failures = 0;

    static void check(String name, boolean ok, String detail) {
        if (ok) {
            System.out.println("  PASS  " + name);
        } else {
            System.out.println("  FAIL  " + name + "  " + detail);
            failures++;
        }
    }

    static SongEntry song(int code, String title) {
        return new SongEntry(code, title, "", "song_" + code + ".mp3", 120, "");
    }

    public static void main(String[] args) {
        List<SongEntry> songs = List.of(
                song(1, "Midnight Drive"),
                song(2, "Sunrise"),
                song(3, "Sunset Boulevard"),
                song(4, "Rock & Roll!"),
                song(5, "Drive"));

        System.out.println("by code");
        check("finds code 2", SongMatcher.find(songs, "2").song().code == 2, "");
        check("ignores leading zeros", SongMatcher.find(songs, "002").song().code == 2, "");
        check("unknown code is not found", !SongMatcher.find(songs, "99").found(), "");

        System.out.println("\nby name");
        check("exact name", SongMatcher.find(songs, "Midnight Drive").song().code == 1, "");
        check("ignores case", SongMatcher.find(songs, "midnight drive").song().code == 1, "");
        check("ignores extra spaces", SongMatcher.find(songs, "  midnight   drive ").song().code == 1, "");
        check("ignores punctuation", SongMatcher.find(songs, "rock and roll").found() == false
                && SongMatcher.find(songs, "rock roll").song().code == 4, "");
        check("unique start is enough", SongMatcher.find(songs, "midn").song().code == 1, "");
        check("exact beats partial", SongMatcher.find(songs, "drive").song().code == 5,
                "'drive' is both a full title and part of 'Midnight Drive'");
        check("unique middle word", SongMatcher.find(songs, "boulevard").song().code == 3, "");

        System.out.println("\nambiguous");
        SongMatcher.Match sun = SongMatcher.find(songs, "sun");
        check("'sun' matches two songs", sun.ambiguous() && sun.candidates().size() == 2,
                "got " + sun.candidates().size());

        System.out.println("\nnot found");
        check("nonsense", !SongMatcher.find(songs, "zzzz").found(), "");
        check("empty", !SongMatcher.find(songs, "").found(), "");
        check("only punctuation", !SongMatcher.find(songs, "!!!").found(), "");
        check("null", !SongMatcher.find(songs, null).found(), "");

        System.out.println("\nname taken");
        check("same name, different case", SongMatcher.isNameTaken(songs, "SUNRISE"), "");
        check("new name is free", !SongMatcher.isNameTaken(songs, "Thunder"), "");

        System.out.println("\nname from file");
        check("underscores", SongMatcher.nameFromFile("midnight_drive.mp3").equals("midnight drive"),
                SongMatcher.nameFromFile("midnight_drive.mp3"));
        check("dashes", SongMatcher.nameFromFile("my-cool-song.wav").equals("my cool song"), "");
        check("no extension", SongMatcher.nameFromFile("track").equals("track"), "");
        check("only an extension", SongMatcher.nameFromFile(".mp3").equals("untitled"),
                SongMatcher.nameFromFile(".mp3"));

        System.out.println();
        if (failures > 0) {
            System.out.println(failures + " check(s) failed");
            System.exit(1);
        }
        System.out.println("all checks passed");
    }
}
