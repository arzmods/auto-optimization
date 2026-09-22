package com.example.jukeboxhits.core.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Decodes an audio file into 48 kHz mono PCM.
 *
 * Only the JDK's own sound support is used here (WAV, AIFF, AU), which keeps this class
 * free of Minecraft and Simple Voice Chat imports so it can be unit tested standalone.
 * MP3 is handled one layer up, where Simple Voice Chat's decoder is available.
 */
public final class AudioDecoder {

    /** Refuse anything longer than this; a stuck decode should not eat the server's heap. */
    public static final int MAX_SECONDS = 60 * 15;

    private AudioDecoder() {
    }

    public static boolean isSupportedExtension(String fileName) {
        String lower = fileName.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".wav") || lower.endsWith(".aiff") || lower.endsWith(".aif")
                || lower.endsWith(".au") || lower.endsWith(".snd");
    }

    public static short[] decode(File file) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(file)) {
            return decodeStream(in);
        }
    }

    public static short[] decode(InputStream stream) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(stream)) {
            return decodeStream(in);
        }
    }

    private static short[] decodeStream(AudioInputStream in)
            throws IOException, UnsupportedAudioFileException {
        AudioFormat source = in.getFormat();

        // Normalise to signed 16-bit PCM first; the JDK can do this conversion for the
        // encodings it supports, and everything downstream assumes 16-bit samples.
        AudioFormat pcm = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                source.getSampleRate(),
                16,
                source.getChannels(),
                source.getChannels() * 2,
                source.getSampleRate(),
                false // little endian
        );

        try (AudioInputStream converted = AudioSystem.isConversionSupported(pcm, source)
                ? AudioSystem.getAudioInputStream(pcm, in)
                : in) {

            AudioFormat format = converted.getFormat();
            if (format.getSampleSizeInBits() != 16) {
                throw new UnsupportedAudioFileException(
                        "need 16-bit audio, got " + format.getSampleSizeInBits() + "-bit");
            }

            byte[] bytes = readAllBytes(converted, format);
            short[] interleaved = toShorts(bytes, format.isBigEndian());
            short[] mono = Pcm.toMono(interleaved, format.getChannels());

            return Pcm.resample(mono, Math.round(format.getSampleRate()), Pcm.SAMPLE_RATE);
        }
    }

    private static byte[] readAllBytes(AudioInputStream in, AudioFormat format) throws IOException {
        int frameSize = Math.max(1, format.getFrameSize());
        long maxBytes = (long) MAX_SECONDS * Math.round(format.getSampleRate()) * frameSize;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) > 0) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("audio is longer than the " + MAX_SECONDS + "s limit");
            }
            out.write(buffer, 0, read);
        }

        byte[] bytes = out.toByteArray();
        // A trailing partial frame would shear the sample alignment, so drop it.
        return bytes.length % 2 == 0 ? bytes : java.util.Arrays.copyOf(bytes, bytes.length - 1);
    }

    private static short[] toShorts(byte[] bytes, boolean bigEndian) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes)
                .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        short[] shorts = new short[bytes.length / 2];
        buffer.asShortBuffer().get(shorts);
        return shorts;
    }
}
