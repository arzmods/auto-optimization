package com.example.jukeboxhits.core.play;

import com.example.jukeboxhits.core.JukeboxCore;
import com.example.jukeboxhits.core.audio.AudioDecoder;
import com.example.jukeboxhits.core.audio.Pcm;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.mp3.Mp3Decoder;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Turns a stored file into 48 kHz mono PCM, ready for Simple Voice Chat.
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

    /** @return 48 kHz mono samples, or throws with a message worth showing a player. */
    public static short[] load(Path file) throws IOException {
        String name = file.getFileName().toString();
        short[] samples;

        if (isMp3(name)) {
            samples = loadMp3(file);
        } else if (AudioDecoder.isSupportedExtension(name)) {
            try {
                samples = AudioDecoder.decode(file.toFile());
            } catch (javax.sound.sampled.UnsupportedAudioFileException e) {
                throw new IOException("That file is not audio this server can read.", e);
            }
        } else {
            throw new IOException("Unsupported file type. Use .mp3 or .wav.");
        }

        double gain = JukeboxCore.config().gain;
        return gain == 1.0d ? samples : Pcm.applyGain(samples, gain);
    }

    private static short[] loadMp3(Path file) throws IOException {
        VoicechatApi api = JukeboxVoicechatPlugin.api();
        if (api == null) {
            throw new IOException("Simple Voice Chat is not ready yet, try again in a moment.");
        }

        try (InputStream in = Files.newInputStream(file)) {
            Mp3Decoder decoder = api.createMp3Decoder(in);
            if (decoder == null) {
                throw new IOException("This server cannot decode MP3. Upload a .wav instead.");
            }

            short[] interleaved = decoder.decode();
            if (interleaved == null || interleaved.length == 0) {
                throw new IOException("That MP3 decoded to nothing - it may be corrupt.");
            }

            AudioFormat format = decoder.getAudioFormat();
            int channels = format == null ? 1 : Math.max(1, format.getChannels());
            int rate = format == null ? Pcm.SAMPLE_RATE : Math.round(format.getSampleRate());

            short[] mono = Pcm.toMono(interleaved, channels);
            return Pcm.resample(mono, rate, Pcm.SAMPLE_RATE);
        }
    }
}
