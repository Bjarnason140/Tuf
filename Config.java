package dev.tuff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persistent settings, saved to .minecraft/config/tuffclient.json */
public final class Config {
    public static final Map<String, Integer> DEFAULTS = new LinkedHashMap<>();
    public static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        def("chest", "Chest", 0xFFA500);
        def("trapped_chest", "Trapped Chest", 0xFF3030);
        def("hopper", "Hopper", 0x00FFFF);
        def("dropper", "Dropper", 0xFFFF00);
        def("dispenser", "Dispenser", 0x55FF55);
        def("spawner", "Spawner", 0xFF00FF);
    }
    private static void def(String k, String label, int rgb) { DEFAULTS.put(k, rgb); LABELS.put(k, label); }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tuffclient.json");
    private static final Map<String, Integer> colors = new LinkedHashMap<>();

    static { load(); }

    private Config() {}

    /** Opaque ARGB. */
    public static int get(String key) {
        return 0xFF000000 | colors.getOrDefault(key, DEFAULTS.getOrDefault(key, 0xFFFFFF));
    }

    public static void set(String key, int rgb) { colors.put(key, rgb & 0xFFFFFF); save(); }

    public static void resetColors() { colors.clear(); save(); }

    public static String hex(String key) { return String.format("#%06X", get(key) & 0xFFFFFF); }

    /** Accepts "#RRGGBB" or "RRGGBB". Returns -1 if invalid. */
    public static int parseHex(String s) {
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() != 6) return -1;
        try { return Integer.parseInt(s, 16); } catch (NumberFormatException e) { return -1; }
    }

    private static void load() {
        try {
            if (!Files.exists(FILE)) return;
            Map<String, String> raw = GSON.fromJson(Files.readString(FILE), new TypeToken<Map<String, String>>() {}.getType());
            if (raw == null) return;
            raw.forEach((k, v) -> { int c = parseHex(v); if (c >= 0) colors.put(k, c); });
        } catch (Exception e) {
            System.err.println("[TuffClient] Failed to load config: " + e);
        }
    }

    private static void save() {
        try {
            Map<String, String> raw = new LinkedHashMap<>();
            colors.forEach((k, v) -> raw.put(k, String.format("#%06X", v)));
            Files.writeString(FILE, GSON.toJson(raw));
        } catch (Exception e) {
            System.err.println("[TuffClient] Failed to save config: " + e);
        }
    }
}
