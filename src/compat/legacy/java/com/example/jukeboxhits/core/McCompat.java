package com.example.jukeboxhits.core;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The few vanilla calls that were renamed between Minecraft 1.x and 26.x. This copy is
 * for 1.x; src/compat/modern has the 26.x one, and build.gradle compiles only the one
 * that matches minecraft_version.
 */
public final class McCompat {

    private McCompat() {
    }

    /** Operator level 2, the level vanilla uses for /give and friends. */
    public static boolean isGamemaster(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    /** e.g. "minecraft:overworld". */
    public static String dimensionId(Level level) {
        return level.dimension().location().toString();
    }

    /** Overlay shows above the hotbar; otherwise it goes to chat. */
    public static void tell(Player player, Component message, boolean overlay) {
        player.displayClientMessage(message, overlay);
    }
}
