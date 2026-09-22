package com.arzmods.potatopvp.client;

import com.arzmods.potatopvp.PotatoPvP;
import com.arzmods.potatopvp.QualityLevel;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Throws detail away from block textures as they are loaded.
 *
 * <p>Each sprite is chopped into a grid of blocks and every block is replaced
 * by the average colour of its opaque pixels. On {@link QualityLevel#NONE} the
 * grid is 1x1, so a whole frame collapses into a single flat colour - that is
 * the "no textures" look, and it is very cheap for the GPU to sample.
 *
 * <p>Two things are deliberately preserved:
 * <ul>
 *   <li><b>Per-pixel alpha.</b> Without this, leaves, glass and iron bars turn
 *       into solid cubes and the world becomes unreadable.</li>
 *   <li><b>Everything that is not a block.</b> Items, mobs, players and the
 *       whole interface keep their real textures, because telling a gapple
 *       from an ender pearl in your hotbar is not optional in PvP.</li>
 * </ul>
 */
public final class PotatoTextures {

    /** Sprite name prefixes we are willing to flatten. */
    private static final String[] AFFECTED_PREFIXES = { "block/", "particle/" };

    /** Matches the end crystal's texture, which lives outside any atlas. */
    private static final String END_CRYSTAL = "end_crystal";

    /**
     * NativeImage's pixel accessors have been renamed more than once across
     * Minecraft versions (getPixelRGBA -> getPixel, ...). Looking them up once
     * by shape rather than by name means this class keeps working either way.
     */
    private static final java.util.concurrent.atomic.AtomicInteger SEEN =
            new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger REDUCED =
            new java.util.concurrent.atomic.AtomicInteger();

    private static MethodHandle getPixel;
    private static MethodHandle setPixel;
    private static boolean resolved;
    private static boolean usable;

    private PotatoTextures() {
    }

    /**
     * Reduces the given image in place.
     *
     * @param name        sprite id, e.g. <code>minecraft:block/stone</code>
     * @param frameWidth  width of one animation frame (the sprite's logical width)
     * @param frameHeight height of one animation frame
     * @param image       the decoded png, modified in place
     * @param level       how much detail to keep
     */
    public static void degrade(Identifier name, int frameWidth, int frameHeight, NativeImage image, QualityLevel level, String via) {
        int seen = SEEN.incrementAndGet();
        if (seen <= 3) {
            PotatoPvP.LOGGER.info("[Potato PvP] sprite #{} via {} name={} frame={}x{} image={}x{} level={}",
                    seen, via, name, frameWidth, frameHeight,
                    image == null ? -1 : image.getWidth(), image == null ? -1 : image.getHeight(), level);
        }
        if (seen % 400 == 0) {
            PotatoPvP.LOGGER.info("[Potato PvP] {} sprites seen, {} reduced", seen, REDUCED.get());
        }

        if (image == null || frameWidth <= 0 || frameHeight <= 0) {
            return;
        }
        if (!affects(name)) {
            if (seen <= 3) {
                PotatoPvP.LOGGER.info("[Potato PvP] not a block sprite, left alone: {}", name);
            }
            return;
        }
        int divisions = divisionsFor(level);
        if (divisions <= 0) {
            return;
        }
        if (!ensureResolved()) {
            return;
        }

        // Block size is derived from the frame, never the whole image, so that a
        // stacked animation strip does not get its frames blended together.
        int blockWidth = Math.max(1, frameWidth / divisions);
        int blockHeight = Math.max(1, frameHeight / divisions);
        if (blockWidth == 1 && blockHeight == 1) {
            return; // nothing to gain
        }

        int width = image.getWidth();
        int height = image.getHeight();

        try {
            for (int blockY = 0; blockY < height; blockY += blockHeight) {
                for (int blockX = 0; blockX < width; blockX += blockWidth) {
                    int maxX = Math.min(blockX + blockWidth, width);
                    int maxY = Math.min(blockY + blockHeight, height);
                    averageBlock(image, blockX, blockY, maxX, maxY);
                }
            }
            int done = REDUCED.incrementAndGet();
            if (done == 1) {
                PotatoPvP.LOGGER.info("[Potato PvP] first sprite actually reduced: {} ({}x{} blocks of {}x{})",
                        name, divisions, divisions, blockWidth, blockHeight);
            }
        } catch (Throwable t) {
            usable = false;
            PotatoPvP.LOGGER.warn("[Potato PvP] Texture reduction failed for {}, leaving it alone", name, t);
        }
    }

    /** Replaces one rectangle with the average colour of its opaque pixels. */
    private static void averageBlock(NativeImage image, int fromX, int fromY, int toX, int toY) throws Throwable {
        long sumA = 0;
        long sumB = 0;
        long sumC = 0;
        int opaqueCount = 0;

        for (int y = fromY; y < toY; y++) {
            for (int x = fromX; x < toX; x++) {
                int pixel = (int) getPixel.invoke(image, x, y);
                if (((pixel >>> 24) & 0xFF) == 0) {
                    continue; // fully transparent pixels must not drag the colour down
                }
                sumA += (pixel >>> 16) & 0xFF;
                sumB += (pixel >>> 8) & 0xFF;
                sumC += pixel & 0xFF;
                opaqueCount++;
            }
        }

        if (opaqueCount == 0) {
            return;
        }

        // The three colour channels are averaged by position, not by name. That
        // keeps this correct whether the buffer is ABGR or ARGB - alpha is the
        // top byte in both, which is the only thing we actually need to know.
        int avgA = (int) (sumA / opaqueCount);
        int avgB = (int) (sumB / opaqueCount);
        int avgC = (int) (sumC / opaqueCount);
        int flatColour = (avgA << 16) | (avgB << 8) | avgC;

        for (int y = fromY; y < toY; y++) {
            for (int x = fromX; x < toX; x++) {
                int pixel = (int) getPixel.invoke(image, x, y);
                int alpha = pixel & 0xFF000000;
                setPixel.invoke(image, x, y, alpha | flatColour);
            }
        }
    }

    /** How many blocks across one frame is cut into. */
    private static int divisionsFor(QualityLevel level) {
        return switch (level) {
            case NONE -> 1;     // one flat colour per frame
            case MINIMUM -> 2;  // 2x2, just enough to hint at shape
            case MEDIUM -> 4;   // 4x4, recognisable but cheap
        };
    }

    private static boolean affects(Identifier name) {
        if (name == null) {
            return false;
        }
        String path = name.getPath();
        for (String prefix : AFFECTED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        // End crystals are the one thing outside the block atlas worth
        // reducing: in crystal PvP there can be a great many of them on screen
        // at once, and unlike a player skin nothing is read off their surface.
        return path.contains(END_CRYSTAL);
    }

    /**
     * Finds the pixel getter and setter once.
     *
     * <p>Matching on the name alone is what made this silently do nothing:
     * these methods have been called getPixelRGBA, getPixelABGR and getPixel
     * across versions, and nothing guarantees the next name contains "pixel"
     * at all. So candidates are found by <em>shape</em> - a getter is
     * (int, int) returning int, a setter is (int, int, int) returning void -
     * and the name is only used to rank them when there is more than one.
     *
     * <p>A shape-only match is accepted only when it is the sole candidate, so
     * an unrelated method can never be mistaken for a pixel accessor. Whatever
     * is picked is written to the log.
     */
    private static synchronized boolean ensureResolved() {
        if (resolved) {
            return usable;
        }
        resolved = true;
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        List<Method> getters = new ArrayList<>();
        List<Method> setters = new ArrayList<>();

        for (Method method : NativeImage.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            boolean allInts = true;
            for (Class<?> param : params) {
                if (param != int.class) {
                    allInts = false;
                    break;
                }
            }
            if (!allInts) {
                continue;
            }
            if (method.getReturnType() == int.class && params.length == 2) {
                getters.add(method);
            } else if (method.getReturnType() == void.class && params.length == 3) {
                setters.add(method);
            }
        }

        Method getter = pick(getters);
        Method setter = pick(setters);

        if (getter != null) {
            getPixel = unreflect(lookup, getter);
        }
        if (setter != null) {
            setPixel = unreflect(lookup, setter);
        }

        usable = getPixel != null && setPixel != null;
        if (usable) {
            PotatoPvP.LOGGER.info("[Potato PvP] pixel accessors: {} / {}", getter.getName(), setter.getName());
        } else {
            PotatoPvP.LOGGER.warn("[Potato PvP] Could not find NativeImage pixel accessors "
                    + "(getters found: {}, setters found: {}); the Textures setting will do nothing.",
                    names(getters), names(setters));
        }
        return usable;
    }

    /** Highest scoring candidate, or the only one if none of them score. */
    private static Method pick(List<Method> candidates) {
        Method best = null;
        int bestScore = 0;
        for (Method candidate : candidates) {
            int score = score(candidate.getName());
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        if (best != null) {
            return best;
        }
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    private static int score(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("pixel")) {
            return 3;
        }
        if (lower.contains("colour") || lower.contains("color")) {
            return 2;
        }
        if (lower.contains("rgba") || lower.contains("abgr") || lower.contains("argb")) {
            return 1;
        }
        return 0;
    }

    private static String names(List<Method> methods) {
        StringBuilder out = new StringBuilder();
        for (Method method : methods) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(method.getName());
        }
        return out.length() == 0 ? "none" : out.toString();
    }

    private static MethodHandle unreflect(MethodHandles.Lookup lookup, Method method) {
        try {
            method.setAccessible(true);
            return lookup.unreflect(method);
        } catch (Throwable t) {
            return null;
        }
    }


    /**
     * Digs the decoded image out of a SpriteContents.
     *
     * <p>NeoForge patches in a public getOriginalImage(), but vanilla - and so
     * Fabric - has only a private field, and its name has changed between
     * versions. Locating it by type instead works on both loaders: it is the
     * only plain NativeImage field on the class, the mipmap pyramid beside it
     * being an array. Looked up once and cached.
     */
    public static NativeImage findImage(Object spriteContents) {
        if (spriteContents == null) {
            return null;
        }
        try {
            ensureImageField(spriteContents.getClass());
            if (imageField == null) {
                return null;
            }
            Object value = imageField.get(spriteContents);
            if (value == null) {
                return null;
            }
            if (imageFieldIsArray) {
                NativeImage[] levels = (NativeImage[]) value;
                return levels.length == 0 ? null : levels[0];
            }
            return (NativeImage) value;
        } catch (Throwable t) {
            return null;
        }
    }

    private static java.lang.reflect.Field imageField;
    private static boolean imageFieldIsArray;
    private static boolean imageFieldSearched;

    private static synchronized void ensureImageField(Class<?> type) {
        if (imageFieldSearched) {
            return;
        }
        imageFieldSearched = true;

        // A plain NativeImage field is the usual shape, but some versions keep
        // only the mipmap pyramid, so an array of them counts too.
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (java.lang.reflect.Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                boolean plain = field.getType() == NativeImage.class;
                boolean array = field.getType() == NativeImage[].class;
                if (!plain && !array) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    imageField = field;
                    imageFieldIsArray = array;
                    PotatoPvP.LOGGER.info("[Potato PvP] sprite image field: {}.{}{}",
                            current.getSimpleName(), field.getName(), array ? " (array)" : "");
                    return;
                } catch (Throwable ignored) {
                    // keep looking
                }
            }
        }
        PotatoPvP.LOGGER.warn("[Potato PvP] Could not reach the sprite image field; "
                + "the Textures setting will have no effect on this Minecraft version.");
    }

    /**
     * Copies the first animation frame over every later frame, so an animated
     * sprite still ticks but never appears to change.
     *
     * <p>This is deliberately not done by making isAnimated() report false.
     * Minecraft 26.3 uses that flag to decide how much of the image to upload
     * to the atlas, and lying about it produces
     * "Dest texture is not large enough to write a rectangle", which kills the
     * resource reload and stops the game booting. Rewriting pixels cannot
     * affect any of that - the image keeps its real size and frame count.
     */
    public static void freezeFrames(int frameWidth, int frameHeight, NativeImage image) {
        if (image == null || frameWidth <= 0 || frameHeight <= 0) {
            return;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (height <= frameHeight) {
            return; // single frame, nothing to freeze
        }
        if (!ensureResolved()) {
            return;
        }
        try {
            for (int y = frameHeight; y < height; y++) {
                int sourceY = y % frameHeight;
                for (int x = 0; x < width; x++) {
                    setPixel.invoke(image, x, y, (int) getPixel.invoke(image, x, sourceY));
                }
            }
        } catch (Throwable t) {
            PotatoPvP.LOGGER.warn("[Potato PvP] Could not freeze an animated sprite", t);
        }
    }
}
