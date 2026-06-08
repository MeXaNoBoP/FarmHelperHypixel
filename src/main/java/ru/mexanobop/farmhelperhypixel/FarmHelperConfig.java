package ru.mexanobop.farmhelperhypixel;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FarmHelperConfig {
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("farmhelperhypixel.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int forwardKey = GLFW.GLFW_KEY_UP;
    public int backKey = GLFW.GLFW_KEY_DOWN;
    public int leftKey = GLFW.GLFW_KEY_LEFT;
    public int rightKey = GLFW.GLFW_KEY_RIGHT;
    public int attackKey = GLFW.GLFW_KEY_PAGE_DOWN;
    public boolean reduceSensitivity = true;
    public boolean farmModeEnabled = false;
    public int farmToggleKey = GLFW.GLFW_KEY_PAGE_UP;
    public boolean hudEnabled = true;
    public int hudX = 4;
    public int hudY = 4;
    public List<CommandBind>   commandBinds   = new ArrayList<>();
    public List<InversionBind> inversionBinds = new ArrayList<>();

    public static class CommandBind {
        public int key;
        public String command;

        public CommandBind() {}

        public CommandBind(int key, String command) {
            this.key = key;
            this.command = command;
        }
    }

    public static class InversionBind {
        public int key1;
        public int key2;

        public InversionBind() {}

        public InversionBind(int key1, int key2) {
            this.key1 = key1;
            this.key2 = key2;
        }
    }

    public static final String[] CROP_KEYS = {
        "wheat", "carrot", "potato", "nether_wart", "sugar_cane", "blue_orchid",
        "cocoa", "cactus", "melon", "pumpkin", "sunflower"
    };

    public static class CropStats {
        public long lastBreakTime = 0; // epoch ms of last break, 0 = never
        public long activeMs      = 0; // accumulated active farming ms
        public int  count         = 0;
        // Session data — transient (not saved, reset when farm mode toggles on)
        public transient long sessionLastBreakTime = 0;
        public transient long sessionActiveMs      = 0;
        public transient int  sessionCount         = 0;
    }

    public Map<String, CropStats> cropStats = new LinkedHashMap<>();

    public static FarmHelperConfig load() {
        File file = CONFIG_FILE.toFile();
        if (!file.exists()) {
            FarmHelperConfig cfg = new FarmHelperConfig();
            cfg.commandBinds.add(new CommandBind(GLFW.GLFW_KEY_END, "home"));
            cfg.save();
            return cfg;
        }
        try (Reader r = new FileReader(file)) {
            FarmHelperConfig cfg = GSON.fromJson(r, FarmHelperConfig.class);
            if (cfg == null) cfg = new FarmHelperConfig();
            if (cfg.commandBinds   == null) cfg.commandBinds   = new ArrayList<>();
            if (cfg.inversionBinds == null) cfg.inversionBinds = new ArrayList<>();
            if (cfg.cropStats      == null) cfg.cropStats      = new LinkedHashMap<>();
            for (String key : CROP_KEYS) cfg.cropStats.putIfAbsent(key, new CropStats());
            return cfg;
        } catch (Exception e) {
            return new FarmHelperConfig();
        }
    }

    public void save() {
        try (Writer w = new FileWriter(CONFIG_FILE.toFile())) {
            GSON.toJson(this, w);
        } catch (Exception ignored) {}
    }
}
