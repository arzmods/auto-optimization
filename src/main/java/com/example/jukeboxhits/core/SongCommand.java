package com.example.jukeboxhits.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * /song <code>          hand the player a disc for that code
 * /song list [page]     browse installed codes
 * /song search <text>   find a code by title or artist
 * /song reload          re-read the config file
 *
 * Only vanilla and Brigadier types are used here, so this class is identical on
 * Fabric, NeoForge and Forge.
 */
public final class SongCommand {

    private static final int PER_PAGE = 15;

    private SongCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("song")
                .requires(source -> JukeboxCore.config().allowAllPlayers || source.hasPermission(2));

        root.then(Commands.argument("code", IntegerArgumentType.integer(1))
                .executes(ctx -> give(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "code"))));

        root.then(Commands.literal("list")
                .executes(ctx -> list(ctx.getSource(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> list(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page")))));

        root.then(Commands.literal("search")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> search(ctx.getSource(), StringArgumentType.getString(ctx, "text")))));

        root.then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> reload(ctx.getSource())));

        dispatcher.register(root);
    }

    private static int give(CommandSourceStack source, int code) {
        SongIndex.Song song = SongIndex.byCode(code);
        if (song == null) {
            source.sendFailure(Component.literal(
                    "No song on code " + code + ". Use /song list to see what is installed."));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("/song has to be run by a player."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        String command = JukeboxCore.config().giveCommand
                .replace("%player%", player.getStringUUID())
                .replace("%disc%", JukeboxCore.config().discItem)
                .replace("%song%", song.songId());

        try {
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(), command);
        } catch (Exception e) {
            source.sendFailure(Component.literal(
                    "Could not build the disc. Check giveCommand in config/jukeboxhits.json."));
            JukeboxCore.LOGGER.error("give command failed: {}", command, e);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("#" + code + "  " + song.label()), false);
        return 1;
    }

    private static int list(CommandSourceStack source, int page) {
        List<SongIndex.Song> songs = SongIndex.all();
        if (songs.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No songs installed yet. See JUKEBOX.md for how to add them."), false);
            return 0;
        }

        int pages = (songs.size() + PER_PAGE - 1) / PER_PAGE;
        int current = Math.min(page, pages);
        int start = (current - 1) * PER_PAGE;
        int end = Math.min(start + PER_PAGE, songs.size());

        source.sendSuccess(() -> Component.literal(
                "Songs " + (start + 1) + "-" + end + " of " + songs.size()
                        + "   (page " + current + "/" + pages + ")"), false);

        for (int i = start; i < end; i++) {
            SongIndex.Song song = songs.get(i);
            source.sendSuccess(() -> Component.literal("  " + song.code + "  " + song.label()), false);
        }
        return songs.size();
    }

    private static int search(CommandSourceStack source, String text) {
        List<SongIndex.Song> hits = SongIndex.search(text);
        if (hits.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Nothing matched \"" + text + "\"."), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal(hits.size() + " match(es):"), false);
        for (SongIndex.Song song : hits) {
            source.sendSuccess(() -> Component.literal("  " + song.code + "  " + song.label()), false);
        }
        return hits.size();
    }

    private static int reload(CommandSourceStack source) {
        JukeboxCore.reloadConfig();
        SongIndex.load();
        source.sendSuccess(() -> Component.literal(
                "Reloaded config. " + SongIndex.count() + " song(s) available."), true);
        return 1;
    }
}
