package com.example.jukeboxhits.core;

/** One uploaded song, addressed by its short numeric code. */
public class SongEntry {

    public int code;
    public String title = "";
    public String artist = "";
    /** File name inside the songs directory, not a full path. */
    public String file = "";
    public double durationSeconds;
    /** UUID of whoever uploaded it, for /song remove permission checks and blame. */
    public String uploadedBy = "";
    public long uploadedAt;

    public SongEntry() {
    }

    public SongEntry(int code, String title, String artist, String file,
                     double durationSeconds, String uploadedBy) {
        this.code = code;
        this.title = title;
        this.artist = artist;
        this.file = file;
        this.durationSeconds = durationSeconds;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = System.currentTimeMillis();
    }

    public String label() {
        if (artist == null || artist.isBlank()) {
            return title == null || title.isBlank() ? "untitled" : title;
        }
        return artist + " - " + title;
    }

    public String durationText() {
        int total = (int) Math.round(durationSeconds);
        return total / 60 + ":" + String.format("%02d", total % 60);
    }
}
