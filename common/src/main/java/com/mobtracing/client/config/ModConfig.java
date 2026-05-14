package com.mobtracing.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

// Persistent config, serialized to <game-dir>/config/mobtracing.json.
// setConfigDir() must be called by the platform entry point before load() is used.
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configDir = null;
    private static @NotNull ModConfig instance = new ModConfig();

    public static void setConfigDir(Path dir) {
        configDir = dir;
    }

    private static Path getConfigPath() {
        if (configDir == null) {
            throw new IllegalStateException(
                "ModConfig.setConfigDir() must be called before load() or save()");
        }
        return configDir.resolve("mobtracing.json");
    }

    // Master toggle — controls the entire overlay. Bound to F7 by default.
    public boolean enabled = false;

    public boolean showPaths = true;
    public boolean showTargetLines = true;
    public boolean showAggroRadius = false;
    public boolean showLookDirection = true;

    public float maxRenderDistance = 48.0f;
    public float labelOpacity = 0.90f;

    // How many client ticks between AI data refreshes (1 = every tick)
    public int updateIntervalTicks = 2;

    public static @NotNull ModConfig get() {
        return instance;
    }

    public static void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            if (loaded != null) instance = loaded;
        } catch (IOException e) {
            instance = new ModConfig();
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(getConfigPath())) {
            GSON.toJson(instance, writer);
        } catch (IOException ignored) {}
    }
}
