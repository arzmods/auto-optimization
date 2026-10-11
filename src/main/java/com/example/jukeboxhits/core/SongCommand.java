package com.example.jukeboxhits.core;

import com.example.jukeboxhits.core.audio.AudioStore;
import com.example.jukeboxhits.core.play.AudioLoader;
import com.example.jukeboxhits.core.play.PlaybackManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * /song <code>              get a disc for that code
 * /song upload <url> <name> download a song and give it the next free code
 * /song play <name|code>    play where you are standing, no disc needed
 * /song disc <name|code>    get a disc for that song
 * /song import             add every audio file dropped into the songs folder
 * /song stop                stop everything you can hear
 * /song list [page]         browse codes
 * /song search <text>       find a code
 * /song remove <code>       delete a song
 * /song reload              re-read the config
 *
 * Only vanilla and Brigadier types appear here, so this is identical on every loader.
 */
public final class SongCommand {

    private static final int PER_PAGE = 12;

    private static final ExecutorService UPLOADS = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "jukeboxhits-upload");
        thread.setDaemon(true);
        return thread;
    });

    private SongCommand() {
    }

    private static boolean canUpload(CommandSourceStack source) {
        return JukeboxCore.config().allowAllPlayersUpload || source.hasPermission(2);
    }

    private static boolean canGetDisc(CommandSourceStack source) {
        return JukeboxCore.config().allowAllPlayersDisc || source.hasPermission(2);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("song");

        root.then(Commands.argument("code", IntegerArgumentType.integer(1))
                .requires(SongCommand::canGetDisc)
                .executes(ctx -> giveDisc(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "code"))));

        root.then(Commands.literal("disc")
                .requires(SongCommand::canGetDisc)
                .then(Commands.argument("song", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(songNames(), builder))
                        .executes(ctx -> withSong(ctx.getSource(), StringArgumentType.getString(ctx, "song"),
                                code -> giveDisc(ctx.getSource(), code)))));

        root.then(Commands.literal("upload")
                .requires(SongCommand::canUpload)
                .then(Commands.argument("url", StringArgumentType.string())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> upload(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "url"),
                                        StringArgumentType.getString(ctx, "name"))))));

        root.then(Commands.literal("play")
                .requires(SongCommand::canGetDisc)
                .then(Commands.argument("song", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(songNames(), builder))
                        .executes(ctx -> withSong(ctx.getSource(), StringArgumentType.getString(ctx, "song"),
                                code -> playHere(ctx.getSource(), code)))));

        root.then(Commands.literal("import")
                .requires(SongCommand::canUpload)
                .executes(ctx -> importFolder(ctx.getSource())));

        root.then(Commands.literal("stop")
                .executes(ctx -> stopAll(ctx.getSource())));

        root.then(Commands.literal("list")
                .executes(ctx -> list(ctx.getSource(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> list(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page")))));

        root.then(Commands.literal("search")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> search(ctx.getSource(), StringArgumentType.getString(ctx, "text")))));

        root.then(Commands.literal("remove")
                .requires(SongCommand::canUpload)
                .then(Commands.argument("code", IntegerArgumentType.integer(1))
                        .executes(ctx -> remove(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "code")))));

        root.then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> reload(ctx.getSource())));

        dispatcher.register(root);
    }

    private static int upload(CommandSourceStack source, String url, String name) {
        SongLibrary library = JukeboxCore.library();
        if (library.size() >= JukeboxCore.config().maxSongs) {
            source.sendFailure(Component.literal(
                    "The library is full (" + JukeboxCore.config().maxSongs
                            + " songs). Remove one first."));
            return 0;
        }

        if (SongMatcher.isNameTaken(library.all(), name)) {
            source.sendFailure(Component.literal(
                    "A song called \"" + name + "\" already exists. Pick another name."));
            return 0;
        }

        int code = library.nextFreeCode();
        String fileName = AudioStore.safeFileName(code, url);

        if (!AudioLoader.isSupported(fileName)) {
            source.sendFailure(Component.literal(
                    "That link must end in .mp3 or .wav so the server knows the format."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Downloading... this can take a moment for a big file."), false);

        // Network and decoding both block, so neither belongs on the server thread.
        UPLOADS.submit(() -> {
            Path saved;
            try {
                saved = JukeboxCore.store().download(url, fileName);
            } catch (AudioStore.UploadException e) {
                source.sendFailure(Component.literal(e.getMessage()));
                return;
            } catch (Exception e) {
                JukeboxCore.LOGGER.warn("Upload failed for {}", url, e);
                source.sendFailure(Component.literal("Download failed: " + e.getMessage()));
                return;
            }

            double seconds;
            try {
                seconds = com.example.jukeboxhits.core.audio.Pcm.durationSeconds(
                        AudioLoader.load(saved));
            } catch (Exception e) {
                try {
                    Files.deleteIfExists(saved);
                } catch (Exception ignored) {
                    // Nothing useful to do if the cleanup itself fails.
                }
                source.sendFailure(Component.literal(
                        "That file downloaded but would not decode: " + e.getMessage()));
                return;
            }

            String uploader = "";
            ServerPlayer player = source.getPlayer();
            if (player != null) {
                uploader = player.getStringUUID();
            }

            SongEntry entry = new SongEntry(code, name, "", fileName, seconds, uploader);
            JukeboxCore.library().put(entry);

            source.sendSuccess(() -> Component.literal(
                    "Added as code " + code + ": " + entry.label()
                            + " (" + entry.durationText() + "). Try /song " + code), true);
        });
        return 1;
    }

    private static int giveDisc(CommandSourceStack source, int code) {
        SongEntry entry = JukeboxCore.library().byCode(code);
        if (entry == null) {
            source.sendFailure(Component.literal(
                    "No song on code " + code + ". Use /song list."));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Run this as a player."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        String discName = DiscTag.encode(code, entry.label());
        String command = JukeboxCore.config().giveCommand
                .replace("%player%", player.getStringUUID())
                .replace("%disc%", JukeboxCore.config().discItem)
                .replace("%name%", "'" + jsonString(discName) + "'");

        try {
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(), command);
        } catch (Exception e) {
            JukeboxCore.LOGGER.error("give command failed: {}", command, e);
            source.sendFailure(Component.literal(
                    "Could not build the disc. Check giveCommand in the config."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "#" + code + "  " + entry.label() + "  - put it in a jukebox"), false);
        return 1;
    }

    private static int playHere(CommandSourceStack source, int code) {
        SongEntry entry = JukeboxCore.library().byCode(code);
        if (entry == null) {
            source.sendFailure(Component.literal("No song on code " + code + "."));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Run this as a player."));
            return 0;
        }

        BlockPos pos = player.blockPosition();
        String key = PlaybackManager.keyFor(
                player.level().dimension().location().toString(),
                pos.getX(), pos.getY(), pos.getZ());

        PlaybackManager.start(key, entry, player.level(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                message -> source.sendFailure(Component.literal(message)));

        source.sendSuccess(() -> Component.literal("Playing #" + code + "  " + entry.label()), false);
        return 1;
    }

    private static int stopAll(CommandSourceStack source) {
        int stopped = PlaybackManager.stopAll();
        source.sendSuccess(() -> Component.literal(
                stopped == 0 ? "Nothing was playing." : "Stopped " + stopped + " song(s)."), false);
        return stopped;
    }

    private static int list(CommandSourceStack source, int page) {
        List<SongEntry> songs = JukeboxCore.library().all();
        if (songs.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No songs yet. Add one with /song upload <url> <name>"), false);
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
            SongEntry entry = songs.get(i);
            source.sendSuccess(() -> Component.literal(
                    "  " + entry.code + "  " + entry.label() + "  " + entry.durationText()), false);
        }
        return songs.size();
    }

    private static int search(CommandSourceStack source, String text) {
        List<SongEntry> hits = JukeboxCore.library().search(text);
        if (hits.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Nothing matched \"" + text + "\"."), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal(hits.size() + " match(es):"), false);
        for (SongEntry entry : hits) {
            source.sendSuccess(() -> Component.literal(
                    "  " + entry.code + "  " + entry.label()), false);
        }
        return hits.size();
    }

    private static int remove(CommandSourceStack source, int code) {
        SongEntry removed = JukeboxCore.library().remove(code);
        if (removed == null) {
            source.sendFailure(Component.literal("No song on code " + code + "."));
            return 0;
        }
        try {
            Files.deleteIfExists(JukeboxCore.store().fileFor(removed.file));
        } catch (Exception e) {
            JukeboxCore.LOGGER.warn("Removed song {} but could not delete {}", code, removed.file, e);
        }
        source.sendSuccess(() -> Component.literal("Removed #" + code + "  " + removed.label()), true);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        JukeboxCore.reload();
        source.sendSuccess(() -> Component.literal(
                "Reloaded. " + JukeboxCore.library().size() + " song(s)."), true);
        return 1;
    }

    /** Every song name, for tab completion. */
    private static List<String> songNames() {
        List<String> names = new ArrayList<>();
        for (SongEntry entry : JukeboxCore.library().all()) {
            if (entry.title != null && !entry.title.isBlank()) {
                names.add(entry.title);
            }
        }
        return names;
    }

    /** Resolves a typed name or code to a song, or explains to the player why it could not. */
    private static int withSong(CommandSourceStack source, String typed,
                                java.util.function.IntUnaryOperator action) {
        SongMatcher.Match match = SongMatcher.find(JukeboxCore.library().all(), typed);
        if (match.found()) {
            return action.applyAsInt(match.song().code);
        }
        if (match.ambiguous()) {
            StringBuilder names = new StringBuilder();
            int shown = 0;
            for (SongEntry entry : match.candidates()) {
                if (shown++ == 5) {
                    names.append(", ...");
                    break;
                }
                names.append(shown == 1 ? "" : ", ").append(entry.title);
            }
            source.sendFailure(Component.literal(
                    "\"" + typed + "\" matches more than one song: " + names + ". Be more specific."));
            return 0;
        }
        source.sendFailure(Component.literal(
                "No song called \"" + typed + "\". Use /song list to see them all."));
        return 0;
    }

    /**
     * Adds every audio file sitting in the songs folder that is not in the library yet,
     * named after its file. This is how a file on your own computer becomes a song:
     * copy it into the folder, run /song import.
     */
    private static int importFolder(CommandSourceStack source) {
        Path folder = JukeboxCore.store().directory();
        source.sendSuccess(() -> Component.literal("Looking in " + folder + " ..."), false);

        UPLOADS.submit(() -> {
            List<String> known = new ArrayList<>();
            for (SongEntry entry : JukeboxCore.library().all()) {
                known.add(entry.file);
            }

            List<Path> fresh = new ArrayList<>();
            try {
                Files.createDirectories(folder);
                try (Stream<Path> files = Files.list(folder)) {
                    files.filter(Files::isRegularFile)
                            .filter(f -> AudioLoader.isSupported(f.getFileName().toString()))
                            .filter(f -> !known.contains(f.getFileName().toString()))
                            .sorted()
                            .forEach(fresh::add);
                }
            } catch (Exception e) {
                source.sendFailure(Component.literal("Could not read the songs folder: " + e.getMessage()));
                return;
            }

            if (fresh.isEmpty()) {
                source.sendSuccess(() -> Component.literal(
                        "No new songs found. Put .mp3 or .wav files in " + folder + " first."), false);
                return;
            }

            int added = 0;
            for (Path file : fresh) {
                if (JukeboxCore.library().size() >= JukeboxCore.config().maxSongs) {
                    source.sendFailure(Component.literal("Library is full; stopped importing."));
                    break;
                }
                String fileName = file.getFileName().toString();
                String name = SongMatcher.nameFromFile(fileName);
                if (SongMatcher.isNameTaken(JukeboxCore.library().all(), name)) {
                    name = name + " " + JukeboxCore.library().nextFreeCode();
                }

                double seconds;
                try {
                    seconds = com.example.jukeboxhits.core.audio.Pcm.durationSeconds(AudioLoader.load(file));
                } catch (Exception e) {
                    String why = e.getMessage();
                    source.sendFailure(Component.literal("Skipped " + fileName + ": " + why));
                    continue;
                }

                int code = JukeboxCore.library().nextFreeCode();
                SongEntry entry = new SongEntry(code, name, "", fileName, seconds, "");
                JukeboxCore.library().put(entry);
                added++;
                source.sendSuccess(() -> Component.literal(
                        "  " + code + "  " + entry.title + "  (" + entry.durationText() + ")"), false);
            }

            int total = added;
            source.sendSuccess(() -> Component.literal(
                    "Imported " + total + " song(s). Play one with /song play <name>"), true);
        });
        return 1;
    }

    /** Minimal JSON string escaping for the name passed through the give command. */
    static String jsonString(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
