package com.example.hardwarescaler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // Set once at startup by whichever loader is running: Fabric hands us
    // FabricLoader's config dir, NeoForge hands us FMLPaths.CONFIGDIR.
    private static Path configDir = Path.of("config");

    public static void setConfigDir(Path dir) {
        configDir = dir;
    }

    private static File modConfigFile() {
        return new File(configDir.toFile(), "hardwarescaler.json");
    }

    private static File sodiumConfigFile() {
        return new File(configDir.toFile(), "sodium-options.json");
    }

    public static class StoredSpecs {
        public String lastGpu = "";
        public String lastCpu = "";
        public double lastRam = 0.0;
        public boolean hasInitialized = false;
    }

    public static StoredSpecs loadConfig() {
        if (!modConfigFile().exists()) return new StoredSpecs();
        try (FileReader reader = new FileReader(modConfigFile())) {
            return GSON.fromJson(reader, StoredSpecs.class);
        } catch (IOException e) {
            return new StoredSpecs();
        }
    }

    public static void saveConfig(String gpu, String cpu, double ram) {
        StoredSpecs specs = new StoredSpecs();
        specs.lastGpu = gpu;
        specs.lastCpu = cpu;
        specs.lastRam = ram;
        specs.hasInitialized = true;
        try (FileWriter writer = new FileWriter(modConfigFile())) {
            GSON.toJson(specs, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applyOptimizationPresets(String tierName) {
        JsonObject sodiumJson = new JsonObject();
        if (sodiumConfigFile().exists()) {
            try (FileReader reader = new FileReader(sodiumConfigFile())) {
                sodiumJson = GSON.fromJson(reader, JsonObject.class);
            } catch (Exception e) {
                sodiumJson = new JsonObject();
            }
        }

        JsonObject videoOptions;
        if (sodiumJson.has("video")) {
            videoOptions = sodiumJson.getAsJsonObject("video");
        } else {
            videoOptions = new JsonObject();
            sodiumJson.add("video", videoOptions);
        }

        if (tierName.equals("TIER_1_9_ELITE_INTEGRATED")) {
            videoOptions.addProperty("render_distance", 16);
            videoOptions.addProperty("simulation_distance", 8);
            videoOptions.addProperty("fps_limit", 90);
            videoOptions.addProperty("quality_preset", "FANCY");
            videoOptions.addProperty("clouds", "OFF");
        } else if (tierName.equals("TIER_1_LOW")) {
            videoOptions.addProperty("render_distance", 6);
            videoOptions.addProperty("simulation_distance", 5);
            videoOptions.addProperty("fps_limit", 60);
            videoOptions.addProperty("quality_preset", "FAST");
        } else if (tierName.equals("TIER_2_MID")) {
            videoOptions.addProperty("render_distance", 14);
            videoOptions.addProperty("simulation_distance", 8);
            videoOptions.addProperty("fps_limit", 144);
            videoOptions.addProperty("quality_preset", "FANCY");
        } else {
            videoOptions.addProperty("render_distance", 24);
            videoOptions.addProperty("simulation_distance", 12);
            videoOptions.addProperty("fps_limit", 260);
            videoOptions.addProperty("quality_preset", "ULTRA");
        }

        try (FileWriter writer = new FileWriter(sodiumConfigFile())) {
            GSON.toJson(sodiumJson, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
