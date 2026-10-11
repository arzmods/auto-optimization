package com.example.soundboard;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;

/**
 * The song list. Built only from buttons and text labels, which look and work the same
 * on 1.20.1 and 26.3, so this class needs no per-version drawing code.
 */
public class SoundboardScreen extends Screen {

    private static final int PER_PAGE = 7;
    private static final int MAX_NAME = 34;

    /** Remembered between openings so you land back where you were. */
    private static int page;

    private StringWidget statusLabel;
    private String shownStatus = "";

    public SoundboardScreen() {
        super(Component.literal("Soundboard"));
    }

    @Override
    protected void init() {
        List<Path> songs = Soundboard.songs();
        int pages = Math.max(1, (songs.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));

        int center = width / 2;
        int y = 15;
        addRenderableWidget(label("Soundboard  (page " + (page + 1) + "/" + pages + ")", y));
        y += 14;
        shownStatus = statusText();
        statusLabel = addRenderableWidget(label(shownStatus, y));
        y += 18;

        if (songs.isEmpty()) {
            addRenderableWidget(label("No songs yet. Put .mp3 or .wav files in the", y));
            addRenderableWidget(label("soundboard folder, then reopen this screen.", y + 12));
        }
        int start = page * PER_PAGE;
        for (int i = start; i < Math.min(start + PER_PAGE, songs.size()); i++) {
            Path song = songs.get(i);
            addRenderableWidget(Button.builder(Component.literal(shorten(song.getFileName().toString())),
                            button -> SoundboardPlayer.play(song))
                    .bounds(center - 100, y, 200, 20).build());
            y += 22;
        }

        int bottom = height - 50;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> turnPage(-1))
                .bounds(center - 154, bottom, 20, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal("Stop"), button -> SoundboardPlayer.stop())
                .bounds(center - 130, bottom, 128, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Open folder"),
                        button -> McCompat.openFolder(Soundboard.folder()))
                .bounds(center + 2, bottom, 128, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> turnPage(1))
                .bounds(center + 134, bottom, 20, 20).build()).active = page < pages - 1;
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(center - 100, bottom + 24, 200, 20).build());
    }

    @Override
    public void tick() {
        // The status changes on its own (loading, progress, song ending), so refresh it.
        String now = statusText();
        if (!now.equals(shownStatus)) {
            shownStatus = now;
            removeWidget(statusLabel);
            statusLabel = addRenderableWidget(label(now, statusLabel.getY()));
        }
    }

    @Override
    public boolean isPauseScreen() {
        // Pausing singleplayer would also pause the voice chat the song travels through.
        return false;
    }

    private void turnPage(int delta) {
        page += delta;
        rebuildWidgets();
    }

    private static String statusText() {
        if (!SoundboardPlayer.isConnected()) {
            return "Voice chat is not connected - nobody will hear it";
        }
        return SoundboardPlayer.statusLine();
    }

    private StringWidget label(String text, int y) {
        StringWidget label = new StringWidget(Component.literal(text), font);
        label.setX(width / 2 - label.getWidth() / 2);
        label.setY(y);
        return label;
    }

    private static String shorten(String name) {
        return name.length() <= MAX_NAME ? name : name.substring(0, MAX_NAME - 3) + "...";
    }
}
