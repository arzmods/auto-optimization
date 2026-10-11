package com.example.soundboard;

import com.example.soundboard.audio.AudioLoader;
import com.example.soundboard.audio.Pcm;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.audiochannel.ClientStaticAudioChannel;
import de.maxhenkel.voicechat.api.events.MergeClientSoundEvent;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Plays one song at a time into the microphone.
 *
 * Simple Voice Chat fires {@link MergeClientSoundEvent} once per 20 ms frame, even while
 * you are not talking, so that event is the clock: each time it fires, the next frame of
 * the song is merged into what your microphone sends (so others hear it) and played on a
 * local channel (so you hear it too).
 */
public final class SoundboardPlayer {

    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "soundboard-loader");
        thread.setDaemon(true);
        return thread;
    });

    private record Track(String name, short[][] frames, AtomicInteger next) {
    }

    private static volatile VoicechatApi api;
    private static volatile boolean connected;
    private static volatile Track track;
    private static volatile String status = "";
    /** Bumped on every play/stop so a slow decode cannot start after it was cancelled. */
    private static final AtomicInteger requests = new AtomicInteger();

    /** Only touched from Simple Voice Chat's microphone thread. */
    private static volatile ClientStaticAudioChannel localChannel;

    private SoundboardPlayer() {
    }

    static void setApi(VoicechatApi voicechatApi) {
        api = voicechatApi;
    }

    static void setConnected(boolean isConnected) {
        connected = isConnected;
        // A channel belongs to one connection; make a fresh one next time.
        localChannel = null;
    }

    public static boolean isConnected() {
        return connected;
    }

    public static void play(Path file) {
        String name = file.getFileName().toString();
        int request = requests.incrementAndGet();
        track = null;
        status = "Loading " + name + "...";

        // Decoding a long MP3 takes a moment, so keep it off the game thread.
        LOADER.submit(() -> {
            try {
                short[][] frames = Pcm.frames(AudioLoader.load(file, api));
                if (requests.get() == request) {
                    track = new Track(name, frames, new AtomicInteger());
                    status = "";
                }
            } catch (Exception e) {
                Soundboard.LOGGER.warn("Could not play {}", file, e);
                if (requests.get() == request) {
                    status = "Could not play " + name + ": " + e.getMessage();
                }
            }
        });
    }

    public static void stop() {
        requests.incrementAndGet();
        track = null;
        status = "";
    }

    /** One line for the screen: what is playing, or what went wrong. */
    public static String statusLine() {
        Track current = track;
        if (current == null) {
            return status.isEmpty() ? "Nothing playing" : status;
        }
        int done = Math.min(current.next().get(), current.frames().length);
        return "Playing " + current.name() + "   " + time(done) + " / " + time(current.frames().length);
    }

    private static String time(int frames) {
        int seconds = frames * Pcm.FRAME_SIZE / Pcm.SAMPLE_RATE;
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    static void onMerge(MergeClientSoundEvent event) {
        Track current = track;
        if (current == null) {
            return;
        }
        int index = current.next().getAndIncrement();
        if (index >= current.frames().length) {
            if (track == current) {
                track = null;
            }
            return;
        }
        short[] frame = current.frames()[index];
        event.mergeAudio(frame);
        hearLocally(event.getVoicechat(), frame);
    }

    private static void hearLocally(VoicechatClientApi clientApi, short[] frame) {
        if (localChannel == null) {
            localChannel = clientApi.createStaticAudioChannel(UUID.randomUUID());
        }
        localChannel.play(frame.clone());
    }
}
