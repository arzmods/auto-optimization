package com.arzmods.potatopvp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The saved settings, stored as <code>config/potatopvp.json</code>.
 *
 * <p>Everything is static because the mixins that read these values run in
 * rendering hot paths and are not given any context object to look things up
 * with. {@link #init(Path)} is called once by each loader's entrypoint with
 * that loader's config folder.
 */
public final class PotatoConfig {

    /** The out-of-the-box potato preset: no particles, no block textures, minimum animations. */
    public static final QualityLevel DEFAULT_PARTICLES = QualityLevel.NONE;
    public static final QualityLevel DEFAULT_TEXTURES = QualityLevel.NONE;
    public static final QualityLevel DEFAULT_ANIMATIONS = QualityLevel.MINIMUM;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static volatile QualityLevel particles = DEFAULT_PARTICLES;
    private static volatile QualityLevel textures = DEFAULT_TEXTURES;
    private static volatile QualityLevel animations = DEFAULT_ANIMATIONS;

    private static Path file;

    private PotatoConfig() {
    }

    /** What actually lands in the json file. Plain strings so the file stays hand-editable. */
    private static final class Data {
        String particles = DEFAULT_PARTICLES.getId();
        String textures = DEFAULT_TEXTURES.getId();
        String animations = DEFAULT_ANIMATIONS.getId();
    }

    /**
     * Points the config at a folder and loads it. If there is no file yet the
     * potato preset is written out, so a fresh install is already optimised.
     */
    public static void init(Path configDir) {
        file = configDir.resolve("potatopvp.json");
        load();
    }

    public static void load() {
        if (file == null || !Files.exists(file)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data != null) {
                particles = QualityLevel.byId(data.particles, DEFAULT_PARTICLES);
                textures = QualityLevel.byId(data.textures, DEFAULT_TEXTURES);
                animations = QualityLevel.byId(data.animations, DEFAULT_ANIMATIONS);
            }
        } catch (IOException | RuntimeException e) {
            PotatoPvP.LOGGER.warn("[Potato PvP] Could not read {}, using the potato preset instead", file, e);
            particles = DEFAULT_PARTICLES;
            textures = DEFAULT_TEXTURES;
            animations = DEFAULT_ANIMATIONS;
        }
    }

    public static void save() {
        if (file == null) {
            return;
        }
        Data data = new Data();
        data.particles = particles.getId();
        data.textures = textures.getId();
        data.animations = animations.getId();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException | RuntimeException e) {
            PotatoPvP.LOGGER.warn("[Potato PvP] Could not write {}", file, e);
        }
    }

    public static QualityLevel particles() {
        return particles;
    }

    public static QualityLevel textures() {
        return textures;
    }

    public static QualityLevel animations() {
        return animations;
    }

    public static void setParticles(QualityLevel level) {
        particles = level;
    }

    public static void setTextures(QualityLevel level) {
        textures = level;
    }

    public static void setAnimations(QualityLevel level) {
        animations = level;
    }

    /** Puts all three settings back to the default potato preset. */
    public static void resetToPotatoPreset() {
        particles = DEFAULT_PARTICLES;
        textures = DEFAULT_TEXTURES;
        animations = DEFAULT_ANIMATIONS;
    }
}
