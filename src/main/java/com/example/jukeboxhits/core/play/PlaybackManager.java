package com.example.jukeboxhits.core.play;

import com.example.jukeboxhits.core.JukeboxCore;
import com.example.jukeboxhits.core.SongEntry;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Tracks what is playing where, so a jukebox can be stopped and does not stack sounds.
 *
 * Decoding happens on a worker thread. Decoding a few minutes of audio takes long enough
 * that doing it on the server thread would visibly freeze the game.
 */
public final class PlaybackManager {

    private static final Map<String, Active> ACTIVE = new ConcurrentHashMap<>();

    private static final ExecutorService DECODERS = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "jukeboxhits-decoder");
        thread.setDaemon(true);
        return thread;
    });

    private record Active(LocationalAudioChannel channel, AudioPlayer player, int code) {
    }

    private PlaybackManager() {
    }

    /** Stable key for one jukebox, so two players cannot start the same block twice. */
    public static String keyFor(String levelName, int x, int y, int z) {
        return levelName + "@" + x + "," + y + "," + z;
    }

    public static boolean isPlaying(String key) {
        Active active = ACTIVE.get(key);
        return active != null && active.player().isPlaying();
    }

    /**
     * Starts a song at a world position.
     *
     * @param onError called with a player-facing message if it could not start
     */
    public static void start(String key, SongEntry entry, Object minecraftLevel,
                             double x, double y, double z, Consumer<String> onError) {
        VoicechatApi api = JukeboxVoicechatPlugin.api();
        if (!(api instanceof VoicechatServerApi serverApi)) {
            onError.accept("Simple Voice Chat is not running on this server.");
            return;
        }

        stop(key);

        Path file = JukeboxCore.store().fileFor(entry.file);
        DECODERS.submit(() -> {
            short[] samples;
            try {
                samples = AudioLoader.load(file);
            } catch (Exception e) {
                JukeboxCore.LOGGER.warn("Could not decode song {} ({})", entry.code, entry.file, e);
                onError.accept("Could not play that song: " + e.getMessage());
                return;
            }

            try {
                ServerLevel level = api.fromServerLevel(minecraftLevel);
                Position position = api.createPosition(x, y, z);

                LocationalAudioChannel channel =
                        serverApi.createLocationalAudioChannel(UUID.randomUUID(), level, position);
                if (channel == null) {
                    onError.accept("Simple Voice Chat refused to open an audio channel.");
                    return;
                }
                channel.setDistance(JukeboxCore.config().playbackDistance);

                AudioPlayer player = serverApi.createAudioPlayer(
                        channel, api.createEncoder(), samples);
                player.setOnStopped(() -> ACTIVE.remove(key));

                ACTIVE.put(key, new Active(channel, player, entry.code));
                player.startPlaying();
            } catch (Exception e) {
                JukeboxCore.LOGGER.error("Could not start playback for song {}", entry.code, e);
                onError.accept("Could not start playback. Check the server log.");
            }
        });
    }

    public static boolean stop(String key) {
        Active active = ACTIVE.remove(key);
        if (active == null) {
            return false;
        }
        try {
            active.player().stopPlaying();
        } catch (Exception e) {
            JukeboxCore.LOGGER.warn("Error stopping playback at {}", key, e);
        }
        return true;
    }

    public static int stopAll() {
        int stopped = 0;
        for (String key : ACTIVE.keySet()) {
            if (stop(key)) {
                stopped++;
            }
        }
        return stopped;
    }

    public static int activeCount() {
        return ACTIVE.size();
    }
}
