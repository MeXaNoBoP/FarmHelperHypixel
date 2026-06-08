package ru.mexanobop.farmhelperhypixel;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FarmHelperHypixelClient implements ClientModInitializer {
    private static final String MOD_ID = "farmhelperhypixel";
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

    private static KeyBinding openMenuKey;
    static FarmHelperConfig config;

    private static boolean settingsApplied = false;
    private static SavedState savedState = null;
    private static final Set<Integer> pressedLastTick = new HashSet<>();

    // Inversion state: index → 0=none, 1=key1 held, 2=key2 held
    private static final Map<Integer, Integer> invState = new HashMap<>();
    private static final Map<Integer, Boolean> invPrev1 = new HashMap<>();
    private static final Map<Integer, Boolean> invPrev2 = new HashMap<>();

    @Override
    public void onInitializeClient() {
        config = FarmHelperConfig.load();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(FarmHelperHypixelClient::onTick);
    }

    private static void onTick(MinecraftClient client) {
        while (openMenuKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new FarmHelperScreen(config));
            }
        }

        if (client.player == null) {
            if (settingsApplied) removeBinds(client);
            return;
        }

        if (config.farmModeEnabled && !settingsApplied) {
            applyBinds(client);
        } else if (!config.farmModeEnabled && settingsApplied) {
            removeBinds(client);
        }

        if (!config.farmModeEnabled || client.currentScreen != null) {
            releaseAttack(client);
            pressedLastTick.clear();
            return;
        }

        if (config.autoAttack) holdAttack(client);
        else releaseAttack(client);

        long handle = client.getWindow().getHandle();

        // Command binds (edge-detect: fire once per press)
        for (FarmHelperConfig.CommandBind bind : config.commandBinds) {
            boolean pressed = GLFW.glfwGetKey(handle, bind.key) == GLFW.GLFW_PRESS;
            if (pressed && !pressedLastTick.contains(bind.key)) sendCommand(client, bind.command);
            if (pressed) pressedLastTick.add(bind.key);
            else pressedLastTick.remove(bind.key);
        }

        // Inversion binds
        for (int i = 0; i < config.inversionBinds.size(); i++) {
            FarmHelperConfig.InversionBind inv = config.inversionBinds.get(i);
            int state = invState.getOrDefault(i, 0);

            boolean p1 = GLFW.glfwGetKey(handle, inv.key1) == GLFW.GLFW_PRESS;
            boolean p2 = GLFW.glfwGetKey(handle, inv.key2) == GLFW.GLFW_PRESS;
            boolean was1 = invPrev1.getOrDefault(i, false);
            boolean was2 = invPrev2.getOrDefault(i, false);

            InputUtil.Key k1 = InputUtil.Type.KEYSYM.createFromCode(inv.key1);
            InputUtil.Key k2 = InputUtil.Type.KEYSYM.createFromCode(inv.key2);

            if (p1 && !was1) {
                if (state == 1) {
                    state = 0;
                    KeyBinding.setKeyPressed(k1, false);
                } else {
                    if (state == 2) KeyBinding.setKeyPressed(k2, false);
                    state = 1;
                    KeyBinding.setKeyPressed(k1, true);
                }
            } else if (p2 && !was2) {
                if (state == 2) {
                    state = 0;
                    KeyBinding.setKeyPressed(k2, false);
                } else {
                    if (state == 1) KeyBinding.setKeyPressed(k1, false);
                    state = 2;
                    KeyBinding.setKeyPressed(k2, true);
                }
            }

            invState.put(i, state);
            invPrev1.put(i, p1);
            invPrev2.put(i, p2);
        }
    }

    private static void applyBinds(MinecraftClient client) {
        if (client.options == null) return;

        savedState = new SavedState(
                getBoundKey(client.options.forwardKey),
                getBoundKey(client.options.leftKey),
                getBoundKey(client.options.backKey),
                getBoundKey(client.options.rightKey),
                getBoundKey(client.options.attackKey),
                client.options.getMouseSensitivity().getValue()
        );

        bindKey(client.options.forwardKey, config.forwardKey);
        bindKey(client.options.leftKey,    config.leftKey);
        bindKey(client.options.backKey,    config.backKey);
        bindKey(client.options.rightKey,   config.rightKey);
        bindKey(client.options.attackKey,  config.attackKey);

        if (config.reduceSensitivity) client.options.getMouseSensitivity().setValue(0.0);

        settingsApplied = true;
        KeyBinding.updateKeysByCode();
        client.options.write();
    }

    private static void removeBinds(MinecraftClient client) {
        if (client.options == null) return;

        releaseAttack(client);
        releaseInversions();

        if (savedState != null) {
            bindKey(client.options.forwardKey, savedState.forwardKey());
            bindKey(client.options.leftKey,    savedState.leftKey());
            bindKey(client.options.backKey,    savedState.backKey());
            bindKey(client.options.rightKey,   savedState.rightKey());
            bindKey(client.options.attackKey,  savedState.attackKey());
            client.options.getMouseSensitivity().setValue(savedState.mouseSensitivity());
            savedState = null;
        }

        settingsApplied = false;
        pressedLastTick.clear();
        KeyBinding.updateKeysByCode();
        client.options.write();
    }

    static void releaseInversions() {
        for (Map.Entry<Integer, Integer> e : invState.entrySet()) {
            int i = e.getKey(), st = e.getValue();
            if (st == 0 || i >= config.inversionBinds.size()) continue;
            FarmHelperConfig.InversionBind inv = config.inversionBinds.get(i);
            if (st == 1) KeyBinding.setKeyPressed(InputUtil.Type.KEYSYM.createFromCode(inv.key1), false);
            if (st == 2) KeyBinding.setKeyPressed(InputUtil.Type.KEYSYM.createFromCode(inv.key2), false);
        }
        invState.clear();
        invPrev1.clear();
        invPrev2.clear();
    }

    private static void holdAttack(MinecraftClient client) {
        InputUtil.Key key = getBoundKey(client.options.attackKey);
        client.options.attackKey.setPressed(true);
        KeyBinding.setKeyPressed(key, true);
    }

    private static void releaseAttack(MinecraftClient client) {
        if (client.options == null) return;
        InputUtil.Key key = getBoundKey(client.options.attackKey);
        client.options.attackKey.setPressed(false);
        KeyBinding.setKeyPressed(key, false);
    }

    private static void sendCommand(MinecraftClient client, String command) {
        if (client.getNetworkHandler() != null) client.getNetworkHandler().sendChatCommand(command);
    }

    private static void bindKey(KeyBinding binding, int glfwKey) {
        binding.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(glfwKey));
    }

    private static void bindKey(KeyBinding binding, InputUtil.Key key) {
        binding.setBoundKey(key);
    }

    private static InputUtil.Key getBoundKey(KeyBinding binding) {
        return InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
    }

    private record SavedState(
            InputUtil.Key forwardKey,
            InputUtil.Key leftKey,
            InputUtil.Key backKey,
            InputUtil.Key rightKey,
            InputUtil.Key attackKey,
            double mouseSensitivity
    ) {}
}
