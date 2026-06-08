package ru.mexanobop.farmhelperhypixel;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class FarmHelperScreen extends Screen {
    private static final int WIN_W = 340;
    private static final int WIN_H = 250;
    private static final int BORDER = 2;
    private static final int PAD = 10;

    private final FarmHelperConfig config;

    private String section = "controls";
    private boolean dropdownOpen = false;

    private String capturingFor = null;

    private boolean addingBind = false;
    private boolean capturingNewBindKey = false;
    private int newBindKey = 0;
    private String pendingCommandText = "";
    private TextFieldWidget commandField = null;

    public FarmHelperScreen(FarmHelperConfig config) {
        super(Text.literal("FarmHelperHypixel"));
        this.config = config;
    }

    private int wx() { return (width - WIN_W) / 2; }
    private int wy() { return (height - WIN_H) / 2; }

    @Override
    protected void init() {
        int wx = wx(), wy = wy();

        String dropLabel = section.equals("controls") ? "Controls ▼" : "Binds ▼";
        addDrawableChild(ButtonWidget.builder(Text.literal(dropLabel), btn -> {
            dropdownOpen = !dropdownOpen;
        }).position(wx + PAD, wy + PAD).size(90, 16).build());

        String farmLabel = config.farmModeEnabled ? "Farm Mode: ON" : "Farm Mode: OFF";
        addDrawableChild(ButtonWidget.builder(Text.literal(farmLabel), btn -> {
            config.farmModeEnabled = !config.farmModeEnabled;
            config.save();
            clearAndInit();
        }).position(wx + WIN_W - 130 - PAD, wy + PAD).size(130, 16).build());

        int contentY = wy + 34;
        if (section.equals("controls")) {
            initControls(wx, contentY);
        } else {
            initBinds(wx, contentY);
        }
    }

    private void initControls(int wx, int startY) {
        String[] labels = {"Forward:", "Back:", "Left:", "Right:", "Attack:"};
        int[] keys = {config.forwardKey, config.backKey, config.leftKey, config.rightKey, config.attackKey};
        String[] actions = {"forward", "back", "left", "right", "attack"};

        for (int i = 0; i < labels.length; i++) {
            final String action = actions[i];
            final int key = keys[i];
            boolean capturing = action.equals(capturingFor);
            String keyLabel = capturing ? "Press a key..." : keyName(key);

            addDrawableChild(ButtonWidget.builder(Text.literal(keyLabel), btn -> {
                capturingFor = action;
                clearAndInit();
            }).position(wx + WIN_W - 120 - PAD, startY + i * 22).size(110, 16).build());
        }

        int toggleY = startY + labels.length * 22 + 6;

        addDrawableChild(ButtonWidget.builder(
                Text.literal(config.autoAttack ? "Auto-attack: ON" : "Auto-attack: OFF"), btn -> {
                    config.autoAttack = !config.autoAttack;
                    config.save();
                    clearAndInit();
                }).position(wx + WIN_W - 150 - PAD, toggleY).size(140, 16).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(config.reduceSensitivity ? "Low sens: ON" : "Low sens: OFF"), btn -> {
                    config.reduceSensitivity = !config.reduceSensitivity;
                    config.save();
                    clearAndInit();
                }).position(wx + WIN_W - 150 - PAD, toggleY + 22).size(140, 16).build());
    }

    private void initBinds(int wx, int startY) {
        int count = config.commandBinds.size();

        for (int i = 0; i < count; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal("✕"), btn -> {
                config.commandBinds.remove(idx);
                config.save();
                clearAndInit();
            }).position(wx + WIN_W - 30 - PAD, startY + i * 22).size(20, 16).build());
        }

        int addY = startY + count * 22 + 6;

        if (addingBind) {
            String keyLabel = capturingNewBindKey ? "Press a key..."
                    : (newBindKey == 0 ? "[ Key ]" : keyName(newBindKey));

            addDrawableChild(ButtonWidget.builder(Text.literal(keyLabel), btn -> {
                capturingNewBindKey = true;
                clearAndInit();
            }).position(wx + PAD, addY).size(90, 16).build());

            commandField = new TextFieldWidget(textRenderer, wx + PAD + 98, addY, 110, 16, Text.empty());
            commandField.setPlaceholder(Text.literal("/command"));
            commandField.setText(pendingCommandText);
            commandField.setChangedListener(text -> pendingCommandText = text);
            addDrawableChild(commandField);

            if (newBindKey != 0) setFocused(commandField);

            addDrawableChild(ButtonWidget.builder(Text.literal("✓"), btn -> {
                String cmd = pendingCommandText.replaceFirst("^/", "").trim();
                if (newBindKey != 0 && !cmd.isEmpty()) {
                    config.commandBinds.add(new FarmHelperConfig.CommandBind(newBindKey, cmd));
                    config.save();
                    addingBind = false;
                    capturingNewBindKey = false;
                    newBindKey = 0;
                    pendingCommandText = "";
                    clearAndInit();
                }
            }).position(wx + PAD + 98 + 118, addY).size(20, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("✗"), btn -> {
                addingBind = false;
                capturingNewBindKey = false;
                newBindKey = 0;
                pendingCommandText = "";
                clearAndInit();
            }).position(wx + PAD + 98 + 142, addY).size(20, 16).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.literal("+ Add Bind"), btn -> {
                addingBind = true;
                clearAndInit();
            }).position(wx + PAD, addY).size(80, 16).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x99000000);

        int wx = wx(), wy = wy();
        long time = System.currentTimeMillis();
        float shimmer = (float) (Math.sin(time / 1800.0) * 0.5 + 0.5);
        int g = (int) (45 + shimmer * 40);
        int bgColor = 0xFF000000 | (g << 8);

        context.fill(wx, wy, wx + WIN_W, wy + WIN_H, bgColor);

        renderDiamonds(context, wx, wy, time);

        int b = BORDER;
        context.fill(wx, wy, wx + WIN_W, wy + b, 0xFF000000);
        context.fill(wx, wy + WIN_H - b, wx + WIN_W, wy + WIN_H, 0xFF000000);
        context.fill(wx, wy, wx + b, wy + WIN_H, 0xFF000000);
        context.fill(wx + WIN_W - b, wy, wx + WIN_W, wy + WIN_H, 0xFF000000);

        context.fill(wx + b, wy + 30, wx + WIN_W - b, wy + 31, 0x66000000);

        int contentY = wy + 34;
        if (section.equals("controls")) {
            String[] labels = {"Forward:", "Back:", "Left:", "Right:", "Attack:"};
            for (int i = 0; i < labels.length; i++) {
                context.drawText(textRenderer, labels[i], wx + PAD, contentY + i * 22 + 4, 0xFFDDDDDD, true);
            }
            int toggleY = contentY + labels.length * 22 + 6;
            context.drawText(textRenderer, "Auto-attack:", wx + PAD, toggleY + 4, 0xFFDDDDDD, true);
            context.drawText(textRenderer, "Low sensitivity:", wx + PAD, toggleY + 26, 0xFFDDDDDD, true);
        } else {
            for (int i = 0; i < config.commandBinds.size(); i++) {
                FarmHelperConfig.CommandBind bind = config.commandBinds.get(i);
                String label = "[" + keyName(bind.key) + "]  →  /" + bind.command;
                context.drawText(textRenderer, label, wx + PAD, contentY + i * 22 + 4, 0xFFDDDDDD, true);
            }
        }

        super.render(context, mouseX, mouseY, delta);

        if (dropdownOpen) {
            renderDropdownOverlay(context, wx, wy, mouseX, mouseY);
        }
    }

    private void renderDiamonds(DrawContext context, int wx, int wy, long time) {
        double phase = time / 5000.0;
        int[][] shapes = {
                {wx + 55,  wy + 140, 18},
                {wx + 145, wy + 170, 13},
                {wx + 235, wy + 130, 17},
                {wx + 295, wy + 175, 11},
                {wx + 100, wy + 200, 9},
        };
        for (int i = 0; i < shapes.length; i++) {
            int cx = shapes[i][0] + (int) (Math.sin(phase + i * 1.3) * 7);
            int cy = shapes[i][1] + (int) (Math.cos(phase * 0.75 + i) * 5);
            renderDiamond(context, cx, cy, shapes[i][2], 0x22007700);
        }
    }

    private void renderDiamond(DrawContext context, int cx, int cy, int size, int color) {
        for (int dy = -size; dy <= size; dy++) {
            int hw = size - Math.abs(dy);
            if (hw > 0) context.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, color);
        }
    }

    private void renderDropdownOverlay(DrawContext context, int wx, int wy, int mouseX, int mouseY) {
        int dx = wx + PAD;
        int dy = wy + PAD + 18;
        int dw = 90, dh = 16;
        String[] options = {"Controls", "Binds"};

        for (int i = 0; i < options.length; i++) {
            int oy = dy + i * dh;
            boolean hovered = mouseX >= dx && mouseX <= dx + dw && mouseY >= oy && mouseY <= oy + dh;
            context.fill(dx, oy, dx + dw, oy + dh, hovered ? 0xFF005500 : 0xFF003300);
            context.fill(dx, oy, dx + dw, oy + 1, 0xFF000000);
            context.drawText(textRenderer, options[i], dx + 6, oy + 4, 0xFFEEEEEE, true);
        }
        context.fill(dx, dy + options.length * dh, dx + dw, dy + options.length * dh + 1, 0xFF000000);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (dropdownOpen) {
            int wx = wx(), wy = wy();
            int dx = wx + PAD, dy = wy + PAD + 18;
            int dw = 90, dh = 16;
            String[] sections = {"controls", "binds"};

            for (int i = 0; i < sections.length; i++) {
                int oy = dy + i * dh;
                if (click.x() >= dx && click.x() <= dx + dw && click.y() >= oy && click.y() <= oy + dh) {
                    section = sections[i];
                    dropdownOpen = false;
                    capturingFor = null;
                    addingBind = false;
                    capturingNewBindKey = false;
                    newBindKey = 0;
                    pendingCommandText = "";
                    clearAndInit();
                    return true;
                }
            }

            dropdownOpen = false;
            clearAndInit();
            return true;
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();

        if (capturingFor != null) {
            switch (capturingFor) {
                case "forward" -> config.forwardKey = keyCode;
                case "back"    -> config.backKey    = keyCode;
                case "left"    -> config.leftKey    = keyCode;
                case "right"   -> config.rightKey   = keyCode;
                case "attack"  -> config.attackKey  = keyCode;
            }
            config.save();
            capturingFor = null;
            clearAndInit();
            return true;
        }

        if (capturingNewBindKey) {
            newBindKey = keyCode;
            capturingNewBindKey = false;
            clearAndInit();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    static String keyName(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_UP        -> "Up";
            case GLFW.GLFW_KEY_DOWN      -> "Down";
            case GLFW.GLFW_KEY_LEFT      -> "Left";
            case GLFW.GLFW_KEY_RIGHT     -> "Right";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PgDn";
            case GLFW.GLFW_KEY_PAGE_UP   -> "PgUp";
            case GLFW.GLFW_KEY_END       -> "End";
            case GLFW.GLFW_KEY_HOME      -> "Home";
            case GLFW.GLFW_KEY_INSERT    -> "Ins";
            case GLFW.GLFW_KEY_DELETE    -> "Del";
            case GLFW.GLFW_KEY_ESCAPE    -> "Esc";
            case GLFW.GLFW_KEY_ENTER     -> "Enter";
            case GLFW.GLFW_KEY_TAB       -> "Tab";
            case GLFW.GLFW_KEY_BACKSPACE -> "Bksp";
            case GLFW.GLFW_KEY_SPACE     -> "Space";
            case GLFW.GLFW_KEY_F1  -> "F1";
            case GLFW.GLFW_KEY_F2  -> "F2";
            case GLFW.GLFW_KEY_F3  -> "F3";
            case GLFW.GLFW_KEY_F4  -> "F4";
            case GLFW.GLFW_KEY_F5  -> "F5";
            case GLFW.GLFW_KEY_F6  -> "F6";
            case GLFW.GLFW_KEY_F7  -> "F7";
            case GLFW.GLFW_KEY_F8  -> "F8";
            case GLFW.GLFW_KEY_F9  -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT     -> "Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT         -> "Alt";
            default -> {
                String n = GLFW.glfwGetKeyName(key, 0);
                yield n != null ? n.toUpperCase() : "Key" + key;
            }
        };
    }
}
