package ru.mexanobop.farmhelperhypixel;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FarmHelperConfig {
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("farmhelperhypixel.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int forwardKey = GLFW.GLFW_KEY_UP;
    public int backKey = GLFW.GLFW_KEY_DOWN;
    public int leftKey = GLFW.GLFW_KEY_LEFT;
    public int rightKey = GLFW.GLFW_KEY_RIGHT;
    public int attackKey = GLFW.GLFW_KEY_PAGE_DOWN;
    public boolean autoAttack = false;
    public boolean reduceSensitivity = true;
    public boolean farmModeEnabled = false;
    public List<CommandBind> commandBinds = new ArrayList<>();

    public static class CommandBind {
        public int key;
        public String command;

        public CommandBind() {}

        public CommandBind(int key, String command) {
            this.key = key;
            this.command = command;
        }
    }

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
            if (cfg.commandBinds == null) cfg.commandBinds = new ArrayList<>();
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
