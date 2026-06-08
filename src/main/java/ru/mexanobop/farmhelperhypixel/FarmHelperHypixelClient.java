package ru.mexanobop.farmhelperhypixel;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public final class FarmHelperHypixelClient implements ClientModInitializer {
    private static final String MOD_ID = "farmhelperhypixel";
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

    private static KeyBinding openMenuKey;
    private static FarmHelperConfig config;

    private static boolean settingsApplied = false;
    private static SavedState savedState = null;
    private static final Set<Integer> pressedLastTick = new HashSet<>();

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

        // Auto-attack hold
        if (config.autoAttack) {
            holdAttack(client);
        } else {
            releaseAttack(client);
        }

        // Command binds (edge detection: fire once per press)
        long handle = client.getWindow().getHandle();
        for (FarmHelperConfig.CommandBind bind : config.commandBinds) {
            boolean pressed = GLFW.glfwGetKey(handle, bind.key) == GLFW.GLFW_PRESS;
            if (pressed && !pressedLastTick.contains(bind.key)) {
                sendCommand(client, bind.command);
            }
            if (pressed) pressedLastTick.add(bind.key);
            else pressedLastTick.remove(bind.key);
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

        if (config.reduceSensitivity) {
            client.options.getMouseSensitivity().setValue(0.0);
        }

        settingsApplied = true;
        KeyBinding.updateKeysByCode();
        client.options.write();
    }

    private static void removeBinds(MinecraftClient client) {
        if (client.options == null) return;

        releaseAttack(client);

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
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatCommand(command);
        }
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
