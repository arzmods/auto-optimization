package com.example.jukeboxhits.neoforge;

import com.example.jukeboxhits.core.play.JukeboxVoicechatPlugin;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;

/**
 * Simple Voice Chat discovers plugins on NeoForge and Forge through this annotation,
 * whereas Fabric uses a "voicechat" entrypoint in fabric.mod.json. The annotation cannot
 * live on the shared class without dragging a Forge-only import into the Fabric build,
 * so it goes on this thin subclass instead.
 */
@ForgeVoicechatPlugin
public class JukeboxNeoForgePlugin extends JukeboxVoicechatPlugin {
}
