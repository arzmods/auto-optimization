package com.example.jukeboxhits.core;

public class DiscTagTest {

    private static int failures = 0;

    static void check(String name, boolean ok, String detail) {
        if (ok) {
            System.out.println("  PASS  " + name);
        } else {
            System.out.println("  FAIL  " + name + "  " + detail);
            failures++;
        }
    }

    public static void main(String[] args) {
        System.out.println("round trip");
        for (int code : new int[]{0, 1, 7, 42, 100, 99999}) {
            String name = DiscTag.encode(code, "Artist - Title");
            check("code " + code + " survives the round trip",
                    DiscTag.decode(name) == code, "got " + DiscTag.decode(name));
        }

        System.out.println("\nformat");
        check("looks right", DiscTag.encode(7, "Artist - Title").equals("♪#7 Artist - Title"),
                DiscTag.encode(7, "Artist - Title"));
        check("handles a missing label", DiscTag.encode(7, "").equals("♪#7"),
                DiscTag.encode(7, ""));
        check("handles a null label", DiscTag.encode(7, null).equals("♪#7"), "");

        System.out.println("\nrejecting things that are not ours");
        check("plain disc name", DiscTag.decode("Music Disc") == -1, "");
        check("empty string", DiscTag.decode("") == -1, "");
        check("null", DiscTag.decode(null) == -1, "");
        check("prefix with no digits", DiscTag.decode("♪#abc") == -1, "");
        check("prefix alone", DiscTag.decode("♪#") == -1, "");
        check("a name that merely contains the prefix",
                DiscTag.decode("my ♪#7 disc") == -1, "");
        check("a number that overflows int",
                DiscTag.decode("♪#99999999999999999999 x") == -1, "");

        System.out.println("\nisJukeboxDisc");
        check("true for ours", DiscTag.isJukeboxDisc("♪#3 Song"), "");
        check("false for vanilla", !DiscTag.isJukeboxDisc("Music Disc"), "");

        System.out.println("\nrejects a negative code");
        boolean threw = false;
        try {
            DiscTag.encode(-1, "x");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check("throws", threw, "");

        System.out.println();
        if (failures > 0) {
            System.out.println(failures + " check(s) failed");
            System.exit(1);
        }
        System.out.println("all checks passed");
    }
}
