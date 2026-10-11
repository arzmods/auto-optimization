package com.example.soundboard.audio;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.mp3.Mp3Decoder;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Turns a song file into 48 kHz mono PCM, ready for Simple Voice Chat.
 *
 * MP3 goes through Simple Voice Chat's own decoder; WAV and friends go through the JDK.
 * Both paths end in the same tested {@link Pcm} conversions.
 */
public final class AudioLoader {

    private AudioLoader() {
    }

    public static boolean isSupported(String fileName) {
        return AudioDecoder.isSupportedExtension(fileName) || isMp3(fileName);
    }

    private static boolean isMp3(String fileName) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(".mp3");
    }

    /** @return 48 kHz mono samples, or throws with a message worth showing the player. */
    public static short[] load(Path file, VoicechatApi api) throws IOException {
        String name = file.getFileName().toString();

        if (isMp3(name)) {
            return loadMp3(file, api);
        }
        if (AudioDecoder.isSupportedExtension(name)) {
            try {
                return AudioDecoder.decode(file.toFile());
            } catch (javax.sound.sampled.UnsupportedAudioFileException e) {
                throw new IOException("that file is not audio Minecraft can read.", e);
            }
        }
        throw new IOException("unsupported file type. Use .mp3 or .wav.");
    }

    private static short[] loadMp3(Path file, VoicechatApi api) throws IOException {
        if (api == null) {
            throw new IOException("Simple Voice Chat is not ready yet, try again in a moment.");
        }

        try (InputStream in = Files.newInputStream(file)) {
            Mp3Decoder decoder = api.createMp3Decoder(in);
            if (decoder == null) {
                throw new IOException("MP3 decoding is not available. Use a .wav instead.");
            }

            short[] interleaved = decoder.decode();
            if (interleaved == null || interleaved.length == 0) {
                throw new IOException("that MP3 decoded to nothing - it may be corrupt.");
            }

            AudioFormat format = decoder.getAudioFormat();
            int channels = format == null ? 1 : Math.max(1, format.getChannels());
            int rate = format == null ? Pcm.SAMPLE_RATE : Math.round(format.getSampleRate());

            short[] mono = Pcm.toMono(interleaved, channels);
            return Pcm.resample(mono, rate, Pcm.SAMPLE_RATE);
        }
    }
}
