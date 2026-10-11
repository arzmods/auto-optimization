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
 * Everything is by number - no names to type or remember.
 *
 * /song upload <url>     download a song, get its number
 * /song import           add every new audio file in the songs folder, each gets a number
 * /song play <number>    play where you stand
 * /song disc <number>    get a disc for a jukebox (/song <number> does the same)
 * /song stop             stop everything
 * /song list [page]      see every number
 * /song remove <number>  delete a song
 * /song reload           re-read the config
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
        return JukeboxCore.config().allowAllPlayersUpload || McCompat.isGamemaster(source);
    }

    private static boolean canGetDisc(CommandSourceStack source) {
        return JukeboxCore.config().allowAllPlayersDisc || McCompat.isGamemaster(source);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("song");

        root.then(Commands.argument("code", IntegerArgumentType.integer(1))
                .requires(SongCommand::canGetDisc)
                .executes(ctx -> giveDisc(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "code"))));

        root.then(Commands.literal("disc")
                .requires(SongCommand::canGetDisc)
                .then(Commands.argument("number", IntegerArgumentType.integer(1))
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(songNumbers(), builder))
                        .executes(ctx -> giveDisc(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "number")))));

        root.then(Commands.literal("upload")
                .requires(SongCommand::canUpload)
                .then(Commands.argument("url", StringArgumentType.greedyString())
                        .executes(ctx -> upload(ctx.getSource(),
                                StringArgumentType.getString(ctx, "url")))));

        root.then(Commands.literal("play")
                .requires(SongCommand::canGetDisc)
                .then(Commands.argument("number", IntegerArgumentType.integer(1))
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(songNumbers(), builder))
                        .executes(ctx -> playHere(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "number")))));

        root.then(Commands.literal("import")
                .requires(SongCommand::canUpload)
                .executes(ctx -> importFolder(ctx.getSource())));

        root.then(Commands.literal("stop")
                .executes(ctx -> stopAll(ctx.getSource())));

        root.then(Commands.literal("list")
                .executes(ctx -> list(ctx.getSource(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> list(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page")))));


        root.then(Commands.literal("remove")
                .requires(SongCommand::canUpload)
                .then(Commands.argument("code", IntegerArgumentType.integer(1))
                        .executes(ctx -> remove(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "code")))));

        root.then(Commands.literal("reload")
                .requires(McCompat::isGamemaster)
                .executes(ctx -> reload(ctx.getSource())));

        dispatcher.register(root);
    }

    private static int upload(CommandSourceStack source, String url) {
        SongLibrary library = JukeboxCore.library();
        if (library.size() >= JukeboxCore.config().maxSongs) {
            source.sendFailure(Component.literal(
                    "The library is full (" + JukeboxCore.config().maxSongs
                            + " songs). Remove one first."));
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

            SongEntry entry = new SongEntry(code, fileHint(url), "", fileName, seconds, uploader);
            JukeboxCore.library().put(entry);

            source.sendSuccess(() -> Component.literal(
                    "Added as song " + code + " (" + entry.durationText() + "). Play it with /song play "
                            + code), true);
        });
        return 1;
    }

    private static int giveDisc(CommandSourceStack source, int code) {
        SongEntry entry = JukeboxCore.library().byCode(code);
        if (entry == null) {
            source.sendFailure(Component.literal(
                    "There is no song " + code + ". Use /song list."));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Run this as a player."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        String discName = DiscTag.encode(code, null);
        String command = JukeboxCore.config().giveCommand
                .replace("%player%", player.getStringUUID())
                .replace("%disc%", JukeboxCore.config().discItem)
                .replace("%name%", nameArgument(discName));

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
                "Song " + code + "  - put it in a jukebox"), false);
        return 1;
    }

    private static int playHere(CommandSourceStack source, int code) {
        SongEntry entry = JukeboxCore.library().byCode(code);
        if (entry == null) {
            source.sendFailure(Component.literal("There is no song " + code + "."));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Run this as a player."));
            return 0;
        }

        BlockPos pos = player.blockPosition();
        String key = PlaybackManager.keyFor(
                McCompat.dimensionId(player.level()),
                pos.getX(), pos.getY(), pos.getZ());

        PlaybackManager.start(key, entry, player.level(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                message -> source.sendFailure(Component.literal(message)));

        source.sendSuccess(() -> Component.literal("Playing song " + code), false);
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
                    "No songs yet. Add one with /song import or /song upload <url>"), false);
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
                    "  " + entry.code + "   " + entry.durationText() + "   " + entry.title), false);
        }
        return songs.size();
    }


    private static int remove(CommandSourceStack source, int code) {
        SongEntry removed = JukeboxCore.library().remove(code);
        if (removed == null) {
            source.sendFailure(Component.literal("There is no song " + code + "."));
            return 0;
        }
        try {
            Files.deleteIfExists(JukeboxCore.store().fileFor(removed.file));
        } catch (Exception e) {
            JukeboxCore.LOGGER.warn("Removed song {} but could not delete {}", code, removed.file, e);
        }
        source.sendSuccess(() -> Component.literal("Removed song " + code), true);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        JukeboxCore.reload();
        source.sendSuccess(() -> Component.literal(
                "Reloaded. " + JukeboxCore.library().size() + " song(s)."), true);
        return 1;
    }

    /** Every number in use, for tab completion. */
    private static List<String> songNumbers() {
        List<String> numbers = new ArrayList<>();
        for (SongEntry entry : JukeboxCore.library().all()) {
            numbers.add(String.valueOf(entry.code));
        }
        return numbers;
    }

    /** The original file name, kept only so /song list can remind you which number is which. */
    private static String fileHint(String urlOrFile) {
        String path = urlOrFile;
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
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

                double seconds;
                try {
                    seconds = com.example.jukeboxhits.core.audio.Pcm.durationSeconds(AudioLoader.load(file));
                } catch (Exception e) {
                    String why = e.getMessage();
                    source.sendFailure(Component.literal("Skipped " + fileName + ": " + why));
                    continue;
                }

                int code = JukeboxCore.library().nextFreeCode();
                SongEntry entry = new SongEntry(code, fileName, "", fileName, seconds, "");
                JukeboxCore.library().put(entry);
                added++;
                source.sendSuccess(() -> Component.literal(
                        "  " + code + "  " + fileName + "  (" + entry.durationText() + ")"), false);
            }

            int total = added;
            source.sendSuccess(() -> Component.literal(
                    "Imported " + total + " song(s). Play one with /song play <number>"), true);
        });
        return 1;
    }

    /**
     * The disc name as the give command expects it. From 1.21.5 a quoted string is already
     * a text component; before that the name is JSON wrapped in an SNBT string.
     */
    static String nameArgument(String name) {
        return BuildInfo.SNBT_TEXT_COMPONENTS ? jsonString(name) : "'" + jsonString(name) + "'";
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
