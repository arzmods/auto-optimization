package com.example.jukeboxhits.core.audio;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Standalone test - builds real WAV files on disk and decodes them back. */
public class AudioDecoderTest {

    private static int failures = 0;

    static void check(String name, boolean ok, String detail) {
        if (ok) {
            System.out.println("  PASS  " + name);
        } else {
            System.out.println("  FAIL  " + name + "  " + detail);
            failures++;
        }
    }

    /** Writes a real .wav file: a sine tone of the given rate, channels and duration. */
    static File writeWav(File dir, String name, int sampleRate, int channels, double seconds)
            throws Exception {
        int frames = (int) (sampleRate * seconds);
        ByteBuffer buffer = ByteBuffer.allocate(frames * channels * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < frames; i++) {
            short sample = (short) (Math.sin(i * 2 * Math.PI * 440 / sampleRate) * 12000);
            for (int c = 0; c < channels; c++) {
                buffer.putShort(sample);
            }
        }
        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        File out = new File(dir, name);
        try (AudioInputStream in = new AudioInputStream(
                new ByteArrayInputStream(buffer.array()), format, frames)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, out);
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(System.getProperty("java.io.tmpdir"), "jukebox-test-" + System.nanoTime());
        if (!dir.mkdirs()) {
            throw new IllegalStateException("could not create temp dir");
        }

        System.out.println("extension check");
        check("accepts .wav", AudioDecoder.isSupportedExtension("song.wav"), "");
        check("accepts uppercase .WAV", AudioDecoder.isSupportedExtension("SONG.WAV"), "");
        check("rejects .mp3 (handled elsewhere)", !AudioDecoder.isSupportedExtension("song.mp3"), "");
        check("rejects .txt", !AudioDecoder.isSupportedExtension("notes.txt"), "");

        System.out.println("\ndecoding a 44.1kHz stereo wav (the usual case)");
        File stereo = writeWav(dir, "stereo.wav", 44100, 2, 2.0);
        short[] decoded = AudioDecoder.decode(stereo);
        double seconds = Pcm.durationSeconds(decoded);
        check("comes out at ~2 seconds", Math.abs(seconds - 2.0) < 0.02, "got " + seconds + "s");
        check("comes out mono at 48kHz",
                Math.abs(decoded.length - 96000) < 1000, "got " + decoded.length + " samples");
        boolean hasSignal = false;
        for (short s : decoded) {
            if (Math.abs(s) > 1000) { hasSignal = true; break; }
        }
        check("actually contains audio", hasSignal, "");

        System.out.println("\ndecoding other rates");
        File mono48 = writeWav(dir, "mono48.wav", 48000, 1, 1.0);
        short[] passthrough = AudioDecoder.decode(mono48);
        check("48kHz mono passes through at 1s",
                Math.abs(Pcm.durationSeconds(passthrough) - 1.0) < 0.01,
                "got " + Pcm.durationSeconds(passthrough));

        File low = writeWav(dir, "low.wav", 22050, 1, 1.5);
        short[] upsampled = AudioDecoder.decode(low);
        check("22.05kHz is upsampled to 1.5s",
                Math.abs(Pcm.durationSeconds(upsampled) - 1.5) < 0.02,
                "got " + Pcm.durationSeconds(upsampled));

        System.out.println("\nframing the decoded audio");
        short[][] frames = Pcm.frames(decoded);
        check("splits into 20ms frames", frames.length == (decoded.length + 959) / 960,
                "got " + frames.length);
        check("each frame is exactly 960 samples", frames[0].length == 960, "");

        System.out.println("\nrejecting junk");
        boolean threw = false;
        try {
            File junk = new File(dir, "junk.wav");
            java.nio.file.Files.write(junk.toPath(), "definitely not audio".getBytes());
            AudioDecoder.decode(junk);
        } catch (Exception e) {
            threw = true;
        }
        check("throws on a non-audio file", threw, "");

        for (File f : dir.listFiles()) {
            f.delete();
        }
        dir.delete();

        System.out.println();
        if (failures > 0) {
            System.out.println(failures + " check(s) failed");
            System.exit(1);
        }
        System.out.println("all checks passed");
    }
}
