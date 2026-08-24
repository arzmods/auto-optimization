package com.example.hardwarescaler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File MOD_CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "hardwarescaler.json");
    private static final File SODIUM_CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "sodium-options.json");

    public static class StoredSpecs {
        public String lastGpu = "";
        public String lastCpu = "";
        public double lastRam = 0.0;
        public boolean hasInitialized = false;
    }

    public static StoredSpecs loadConfig() {
        if (!MOD_CONFIG_FILE.exists()) return new StoredSpecs();
        try (FileReader reader = new FileReader(MOD_CONFIG_FILE)) {
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
        try (FileWriter writer = new FileWriter(MOD_CONFIG_FILE)) {
            GSON.toJson(specs, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applyOptimizationPresets(String tierName) {
        JsonObject sodiumJson = new JsonObject();
        if (SODIUM_CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(SODIUM_CONFIG_FILE)) {
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

        try (FileWriter writer = new FileWriter(SODIUM_CONFIG_FILE)) {
            GSON.toJson(sodiumJson, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
