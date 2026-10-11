package com.example.soundboard;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MergeClientSoundEvent;

/** Hooks the soundboard into Simple Voice Chat's microphone. */
public class SoundboardPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return Soundboard.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        SoundboardPlayer.setApi(api);
        Soundboard.LOGGER.info("Connected to Simple Voice Chat");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MergeClientSoundEvent.class, SoundboardPlayer::onMerge);
        registration.registerEvent(ClientVoicechatConnectionEvent.class,
                event -> SoundboardPlayer.setConnected(event.isConnected()));
    }
}
