package com.example.jukeboxhits.core;

/**
 * Encodes a song code into a music disc's display name, and reads it back.
 *
 * Item display names are one of the few parts of the item API that have stayed stable
 * across Minecraft versions, so carrying the code there instead of in NBT or a data
 * component means this keeps working across updates. Pure logic, no Minecraft imports.
 */
public final class DiscTag {

    /** Marks a disc as ours. Deliberately an unusual character so it cannot collide. */
    public static final String PREFIX = "♪#";

    private DiscTag() {
    }

    /** Builds the display name for a disc bound to a code, e.g. "♪#7 Artist - Title". */
    public static String encode(int code, String label) {
        if (code < 0) {
            throw new IllegalArgumentException("code must not be negative");
        }
        String suffix = label == null || label.isBlank() ? "" : " " + label;
        return PREFIX + code + suffix;
    }

    /**
     * Reads the code back out of a display name.
     *
     * @return the code, or -1 if this is not one of our discs
     */
    public static int decode(String displayName) {
        if (displayName == null || !displayName.startsWith(PREFIX)) {
            return -1;
        }
        int start = PREFIX.length();
        int end = start;
        while (end < displayName.length() && Character.isDigit(displayName.charAt(end))) {
            end++;
        }
        if (end == start) {
            return -1;
        }
        try {
            return Integer.parseInt(displayName.substring(start, end));
        } catch (NumberFormatException e) {
            // A code longer than an int can hold is not a code we ever wrote.
            return -1;
        }
    }

    public static boolean isJukeboxDisc(String displayName) {
        return decode(displayName) >= 0;
    }
}
