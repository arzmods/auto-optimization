package com.arzmods.potatopvp.client;

import com.arzmods.potatopvp.PotatoPvP;
import com.arzmods.potatopvp.QualityLevel;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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

    /**
     * NativeImage's pixel accessors have been renamed more than once across
     * Minecraft versions (getPixelRGBA -> getPixel, ...). Looking them up once
     * by shape rather than by name means this class keeps working either way.
     */
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
    public static void degrade(Identifier name, int frameWidth, int frameHeight, NativeImage image, QualityLevel level) {
        if (image == null || frameWidth <= 0 || frameHeight <= 0) {
            return;
        }
        if (!affects(name)) {
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
        return false;
    }

    /** Finds the pixel getter/setter once, by their shape rather than their name. */
    private static synchronized boolean ensureResolved() {
        if (resolved) {
            return usable;
        }
        resolved = true;
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        for (Method method : NativeImage.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (getPixel == null
                    && method.getReturnType() == int.class
                    && params.length == 2 && params[0] == int.class && params[1] == int.class
                    && method.getName().toLowerCase().contains("pixel")) {
                getPixel = unreflect(lookup, method);
            } else if (setPixel == null
                    && method.getReturnType() == void.class
                    && params.length == 3 && params[0] == int.class && params[1] == int.class && params[2] == int.class
                    && method.getName().toLowerCase().contains("pixel")) {
                setPixel = unreflect(lookup, method);
            }
        }

        usable = getPixel != null && setPixel != null;
        if (!usable) {
            PotatoPvP.LOGGER.warn("[Potato PvP] Could not find NativeImage pixel accessors; "
                    + "the Textures setting will have no effect on this Minecraft version.");
        }
        return usable;
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
            return imageField == null ? null : (NativeImage) imageField.get(spriteContents);
        } catch (Throwable t) {
            return null;
        }
    }

    private static java.lang.reflect.Field imageField;
    private static boolean imageFieldSearched;

    private static synchronized void ensureImageField(Class<?> type) {
        if (imageFieldSearched) {
            return;
        }
        imageFieldSearched = true;
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (java.lang.reflect.Field field : current.getDeclaredFields()) {
                if (field.getType() == NativeImage.class && !Modifier.isStatic(field.getModifiers())) {
                    try {
                        field.setAccessible(true);
                        imageField = field;
                        return;
                    } catch (Throwable ignored) {
                        // keep looking
                    }
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
