package com.example.jukeboxhits.core.audio;

/**
 * Raw audio helpers. Pure Java - no Minecraft, no Simple Voice Chat, no third-party
 * libraries - so this class can be compiled and unit tested on its own.
 *
 * Simple Voice Chat wants 48 kHz, mono, signed 16-bit PCM, handed over in 20 ms frames.
 * Everything here exists to get an arbitrary uploaded file into that shape.
 */
public final class Pcm {

    /** Sample rate Simple Voice Chat expects. */
    public static final int SAMPLE_RATE = 48_000;

    /** 20 ms at 48 kHz. One Opus frame. */
    public static final int FRAME_SIZE = 960;

    private Pcm() {
    }

    /**
     * Collapses interleaved multi-channel audio down to mono by averaging channels.
     * Mono matters: Simple Voice Chat positions audio in 3D, and a stereo source cannot
     * be placed at a point in the world.
     */
    public static short[] toMono(short[] interleaved, int channels) {
        if (channels <= 1) {
            return interleaved;
        }
        int frames = interleaved.length / channels;
        short[] mono = new short[frames];
        for (int i = 0; i < frames; i++) {
            int sum = 0;
            for (int c = 0; c < channels; c++) {
                sum += interleaved[i * channels + c];
            }
            mono[i] = (short) (sum / channels);
        }
        return mono;
    }

    /**
     * Resamples mono audio with linear interpolation.
     *
     * Linear interpolation is not the highest-quality resampler in existence, but for
     * music played through a blocky game at a distance it is inaudible, and it avoids
     * pulling in a DSP dependency.
     */
    public static short[] resample(short[] samples, int fromRate, int toRate) {
        if (fromRate == toRate || samples.length == 0) {
            return samples;
        }
        if (fromRate <= 0 || toRate <= 0) {
            throw new IllegalArgumentException("sample rates must be positive");
        }

        long outLength = (long) samples.length * toRate / fromRate;
        if (outLength <= 0) {
            return new short[0];
        }
        if (outLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("resampled audio is too large");
        }

        short[] out = new short[(int) outLength];
        double step = (double) fromRate / toRate;

        for (int i = 0; i < out.length; i++) {
            double pos = i * step;
            int left = (int) pos;
            int right = left + 1;
            double frac = pos - left;

            short a = samples[Math.min(left, samples.length - 1)];
            short b = samples[Math.min(right, samples.length - 1)];

            out[i] = (short) Math.round(a + (b - a) * frac);
        }
        return out;
    }

    /**
     * Splits audio into fixed-size frames, zero-padding the last one.
     * Opus will not accept a short final frame, so the tail has to be padded rather
     * than truncated - otherwise songs audibly clip at the end.
     */
    public static short[][] frames(short[] samples) {
        int count = (samples.length + FRAME_SIZE - 1) / FRAME_SIZE;
        short[][] out = new short[count][];
        for (int i = 0; i < count; i++) {
            short[] frame = new short[FRAME_SIZE];
            int from = i * FRAME_SIZE;
            int len = Math.min(FRAME_SIZE, samples.length - from);
            System.arraycopy(samples, from, frame, 0, len);
            out[i] = frame;
        }
        return out;
    }

    /** Length in seconds of mono audio at the voice chat sample rate. */
    public static double durationSeconds(short[] samples) {
        return (double) samples.length / SAMPLE_RATE;
    }

    /** Scales samples by a factor, clamping so loud audio distorts rather than wraps. */
    public static short[] applyGain(short[] samples, double gain) {
        if (gain == 1.0d) {
            return samples;
        }
        short[] out = new short[samples.length];
        for (int i = 0; i < samples.length; i++) {
            int scaled = (int) Math.round(samples[i] * gain);
            out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, scaled));
        }
        return out;
    }
}
