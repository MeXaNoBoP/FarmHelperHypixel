package ru.mexanobop.farmhelperhypixel;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class FarmHelperHypixelClient implements ClientModInitializer {
    private static final String MOD_ID = "farmhelperhypixel";
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

    private static KeyBinding toggleFarmKey;
    private static KeyBinding homeKey;
    private static boolean farmModeEnabled;
    private static boolean attackToggled;
    private static SavedState savedState;

    @Override
    public void onInitializeClient() {
        toggleFarmKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                CATEGORY
        ));
        homeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".home",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                CATEGORY
        ));

        ClientTickEvents.START_CLIENT_TICK.register(FarmHelperHypixelClient::onClientTickStart);
        ClientTickEvents.END_CLIENT_TICK.register(FarmHelperHypixelClient::onClientTickEnd);
    }

    private static void onClientTickStart(MinecraftClient client) {
        if (farmModeEnabled && client.player != null) {
            applyAttackHold(client);
        }
    }

    private static void onClientTickEnd(MinecraftClient client) {
        while (toggleFarmKey.wasPressed()) {
            if (farmModeEnabled) {
                disableFarmMode(client);
            } else {
                enableFarmMode(client);
            }
        }

        if (!farmModeEnabled) {
            return;
        }

        if (client.player == null) {
            disableFarmMode(client);
            return;
        }

        while (homeKey.wasPressed()) {
            sendHomeCommand(client);
        }

        while (client.options.attackKey.wasPressed()) {
            attackToggled = !attackToggled;
            showActionBar(client, attackToggled
                    ? "message." + MOD_ID + ".attack_on"
                    : "message." + MOD_ID + ".attack_off");
        }

        applyAttackHold(client);
    }

    private static void enableFarmMode(MinecraftClient client) {
        if (client.options == null || farmModeEnabled) {
            return;
        }

        savedState = new SavedState(
                getBoundKey(client.options.forwardKey),
                getBoundKey(client.options.leftKey),
                getBoundKey(client.options.backKey),
                getBoundKey(client.options.rightKey),
                getBoundKey(client.options.attackKey),
                getBoundKey(homeKey),
                client.options.getMouseSensitivity().getValue()
        );

        client.options.getMouseSensitivity().setValue(0.0);
        bindKey(client.options.forwardKey, GLFW.GLFW_KEY_UP);
        bindKey(client.options.leftKey, GLFW.GLFW_KEY_LEFT);
        bindKey(client.options.backKey, GLFW.GLFW_KEY_DOWN);
        bindKey(client.options.rightKey, GLFW.GLFW_KEY_RIGHT);
        bindKey(client.options.attackKey, GLFW.GLFW_KEY_PAGE_DOWN);
        bindKey(homeKey, GLFW.GLFW_KEY_END);

        attackToggled = false;
        farmModeEnabled = true;
        KeyBinding.updateKeysByCode();
        client.options.write();
        showActionBar(client, "message." + MOD_ID + ".enabled");
    }

    private static void disableFarmMode(MinecraftClient client) {
        if (client.options == null || !farmModeEnabled) {
            return;
        }

        farmModeEnabled = false;
        attackToggled = false;
        setAttackHeld(client, false);

        if (savedState != null) {
            client.options.getMouseSensitivity().setValue(savedState.mouseSensitivity());
            bindKey(client.options.forwardKey, savedState.forwardKey());
            bindKey(client.options.leftKey, savedState.leftKey());
            bindKey(client.options.backKey, savedState.backKey());
            bindKey(client.options.rightKey, savedState.rightKey());
            bindKey(client.options.attackKey, savedState.attackKey());
            bindKey(homeKey, savedState.homeKey());
            savedState = null;
        }

        setAttackHeld(client, false);
        KeyBinding.updateKeysByCode();
        client.options.write();
        showActionBar(client, "message." + MOD_ID + ".disabled");
    }

    private static void bindKey(KeyBinding keyBinding, int glfwKeyCode) {
        bindKey(keyBinding, InputUtil.Type.KEYSYM.createFromCode(glfwKeyCode));
    }

    private static void bindKey(KeyBinding keyBinding, InputUtil.Key key) {
        keyBinding.setBoundKey(key);
    }

    private static InputUtil.Key getBoundKey(KeyBinding keyBinding) {
        return InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
    }

    private static void applyAttackHold(MinecraftClient client) {
        setAttackHeld(client, client.currentScreen == null && attackToggled);
    }

    private static void setAttackHeld(MinecraftClient client, boolean held) {
        InputUtil.Key attackKey = getBoundKey(client.options.attackKey);
        client.options.attackKey.setPressed(held);
        KeyBinding.setKeyPressed(attackKey, held);
    }

    private static void sendHomeCommand(MinecraftClient client) {
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatCommand("home");
        }
    }

    private static void showActionBar(MinecraftClient client, String translationKey) {
        if (client.player != null) {
            client.player.sendMessage(Text.translatable(translationKey), true);
        }
    }

    private record SavedState(
            InputUtil.Key forwardKey,
            InputUtil.Key leftKey,
            InputUtil.Key backKey,
            InputUtil.Key rightKey,
            InputUtil.Key attackKey,
            InputUtil.Key homeKey,
            double mouseSensitivity
    ) {
    }
}
