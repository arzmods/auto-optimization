package com.example.jukeboxhits.core.audio;

/** Standalone test - no JUnit, so it runs with plain javac/java. */
public class PcmTest {

    private static int failures = 0;

    static void check(String name, boolean ok, String detail) {
        if (ok) {
            System.out.println("  PASS  " + name);
        } else {
            System.out.println("  FAIL  " + name + "  " + detail);
            failures++;
        }
    }

    static void check(String name, boolean ok) {
        check(name, ok, "");
    }

    public static void main(String[] args) {
        System.out.println("toMono");
        short[] stereo = {100, 200, 300, 400, -100, -200};
        short[] mono = Pcm.toMono(stereo, 2);
        check("halves the length", mono.length == 3, "got " + mono.length);
        check("averages channels", mono[0] == 150 && mono[1] == 350 && mono[2] == -150,
                mono[0] + "," + mono[1] + "," + mono[2]);
        short[] alreadyMono = {1, 2, 3};
        check("passes mono through", Pcm.toMono(alreadyMono, 1) == alreadyMono);

        System.out.println("\nresample");
        short[] src = new short[1000];
        for (int i = 0; i < src.length; i++) {
            src[i] = (short) (Math.sin(i * 0.05) * 10000);
        }
        short[] up = Pcm.resample(src, 24000, 48000);
        check("upsampling doubles length", up.length == 2000, "got " + up.length);
        short[] down = Pcm.resample(src, 48000, 24000);
        check("downsampling halves length", down.length == 500, "got " + down.length);
        check("same rate is a no-op", Pcm.resample(src, 48000, 48000) == src);
        check("empty input stays empty", Pcm.resample(new short[0], 44100, 48000).length == 0);

        // 44.1k -> 48k is the common case; length must scale by the rate ratio.
        short[] cd = new short[44100];
        short[] converted = Pcm.resample(cd, 44100, 48000);
        check("44.1k->48k gives one second", converted.length == 48000, "got " + converted.length);

        // Resampling a constant signal must not change its value.
        short[] flat = new short[100];
        java.util.Arrays.fill(flat, (short) 5000);
        short[] flatUp = Pcm.resample(flat, 22050, 48000);
        boolean allFive = true;
        for (short s : flatUp) {
            if (s != 5000) { allFive = false; break; }
        }
        check("constant signal stays constant", allFive);

        boolean threw = false;
        try {
            Pcm.resample(src, 0, 48000);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check("rejects a zero sample rate", threw);

        System.out.println("\nframes");
        short[] exact = new short[Pcm.FRAME_SIZE * 3];
        check("exact multiple gives 3 frames", Pcm.frames(exact).length == 3);

        short[] ragged = new short[Pcm.FRAME_SIZE * 2 + 10];
        short[][] raggedFrames = Pcm.frames(ragged);
        check("ragged input rounds up", raggedFrames.length == 3, "got " + raggedFrames.length);
        check("every frame is full size",
                raggedFrames[0].length == Pcm.FRAME_SIZE
                        && raggedFrames[2].length == Pcm.FRAME_SIZE);

        short[] tail = new short[Pcm.FRAME_SIZE + 2];
        tail[Pcm.FRAME_SIZE] = 999;
        tail[Pcm.FRAME_SIZE + 1] = 888;
        short[][] tailFrames = Pcm.frames(tail);
        check("tail data survives padding",
                tailFrames[1][0] == 999 && tailFrames[1][1] == 888 && tailFrames[1][2] == 0);

        System.out.println("\nduration");
        check("48000 samples is one second",
                Math.abs(Pcm.durationSeconds(new short[48000]) - 1.0) < 1e-9);
        check("empty is zero", Pcm.durationSeconds(new short[0]) == 0.0);

        System.out.println("\ngain");
        short[] loud = {1000, -1000};
        short[] doubled = Pcm.applyGain(loud, 2.0);
        check("scales samples", doubled[0] == 2000 && doubled[1] == -2000);
        check("gain of 1 is a no-op", Pcm.applyGain(loud, 1.0) == loud);

        short[] nearMax = {32000, -32000};
        short[] clipped = Pcm.applyGain(nearMax, 4.0);
        check("clamps instead of wrapping",
                clipped[0] == Short.MAX_VALUE && clipped[1] == Short.MIN_VALUE,
                clipped[0] + "," + clipped[1]);

        System.out.println();
        if (failures > 0) {
            System.out.println(failures + " check(s) failed");
            System.exit(1);
        }
        System.out.println("all checks passed");
    }
}
