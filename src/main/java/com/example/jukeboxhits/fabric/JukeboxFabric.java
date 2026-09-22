package com.example.jukeboxhits.fabric;

import com.example.jukeboxhits.core.DiscTag;
import com.example.jukeboxhits.core.JukeboxCore;
import com.example.jukeboxhits.core.SongCommand;
import com.example.jukeboxhits.core.SongEntry;
import com.example.jukeboxhits.core.play.PlaybackManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Fabric entrypoint. All the actual work lives in the core package. */
public class JukeboxFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        JukeboxCore.init(FabricLoader.getInstance().getConfigDir());

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> SongCommand.register(dispatcher));

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hit.getBlockPos();
            if (!level.getBlockState(pos).is(Blocks.JUKEBOX)) {
                return InteractionResult.PASS;
            }

            String key = PlaybackManager.keyFor(
                    level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());

            ItemStack held = player.getItemInHand(hand);
            int code = held.isEmpty() ? -1 : DiscTag.decode(held.getHoverName().getString());

            // Empty hand on a jukebox we are driving: stop it, and let vanilla keep
            // handling every jukebox this mod is not currently playing.
            if (code < 0) {
                if (PlaybackManager.isPlaying(key) && PlaybackManager.stop(key)) {
                    player.displayClientMessage(Component.literal("Stopped."), true);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }

            SongEntry entry = JukeboxCore.library().byCode(code);
            if (entry == null) {
                player.displayClientMessage(
                        Component.literal("Code " + code + " is not in the library any more."), true);
                return InteractionResult.SUCCESS;
            }

            PlaybackManager.start(key, entry, level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    message -> player.displayClientMessage(Component.literal(message), false));

            player.displayClientMessage(
                    Component.literal("♪ " + entry.label()), true);
            return InteractionResult.SUCCESS;
        });
    }
}
