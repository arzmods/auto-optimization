package com.example.jukeboxhits.forge;

import com.example.jukeboxhits.core.DiscTag;
import com.example.jukeboxhits.core.JukeboxCore;
import com.example.jukeboxhits.core.SongCommand;
import com.example.jukeboxhits.core.SongEntry;
import com.example.jukeboxhits.core.play.PlaybackManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Forge entrypoint (Minecraft 1.21+). All the actual work lives in the core package.
 * Not part of the Fabric build - see loaders/README.md.
 */
@Mod(JukeboxCore.MOD_ID)
public class JukeboxForge {

    public JukeboxForge() {
        JukeboxCore.init(FMLPaths.CONFIGDIR.get());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        SongCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(Blocks.JUKEBOX)) {
            return;
        }

        String key = PlaybackManager.keyFor(
                level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());

        ItemStack held = event.getItemStack();
        int code = held.isEmpty() ? -1 : DiscTag.decode(held.getHoverName().getString());

        if (code < 0) {
            if (PlaybackManager.isPlaying(key) && PlaybackManager.stop(key)) {
                event.getEntity().displayClientMessage(Component.literal("Stopped."), true);
                event.setCanceled(true);
            }
            return;
        }

        SongEntry entry = JukeboxCore.library().byCode(code);
        if (entry == null) {
            event.getEntity().displayClientMessage(
                    Component.literal("Code " + code + " is not in the library any more."), true);
            event.setCanceled(true);
            return;
        }

        PlaybackManager.start(key, entry, level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                message -> event.getEntity().displayClientMessage(Component.literal(message), false));

        event.getEntity().displayClientMessage(Component.literal("♪ " + entry.label()), true);
        event.setCanceled(true);
    }
}
