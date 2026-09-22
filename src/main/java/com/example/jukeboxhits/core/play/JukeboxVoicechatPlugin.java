package com.example.jukeboxhits.core.play;

import com.example.jukeboxhits.core.JukeboxCore;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;

/**
 * Registers this mod with Simple Voice Chat and holds onto the API handle.
 *
 * Simple Voice Chat is what makes uploads possible at all: vanilla Minecraft can only
 * play audio that shipped with the game or a resource pack, but the voice chat mod
 * already has a live audio pipeline to every client, so arbitrary audio can be streamed
 * down it. This is the same approach AudioPlayer and Custom Discs take.
 */
public class JukeboxVoicechatPlugin implements VoicechatPlugin {

    private static volatile VoicechatApi api;

    @Override
    public String getPluginId() {
        return JukeboxCore.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi voicechatApi) {
        api = voicechatApi;
        JukeboxCore.LOGGER.info("Connected to Simple Voice Chat");
    }

    /** @return the API handle, or null if Simple Voice Chat has not started yet. */
    public static VoicechatApi api() {
        return api;
    }

    public static boolean isReady() {
        return api != null;
    }
}
