package ru.mexanobop.farmhelperhypixel;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class FarmHelperScreen extends Screen {

    // Window layout
    private static final int WIN_W  = 368;
    private static final int WIN_H  = 268;
    private static final int TITLE_H = 28;  // title bar height
    private static final int TOOL_Y  = TITLE_H + 4;   // toolbar top
    private static final int TOOL_H  = 16;
    private static final int CONT_Y  = TOOL_Y + TOOL_H + 6; // content top (= 54)

    // Button colors
    private static final int C_BTN      = 0xFF002800;
    private static final int C_BTN_ON   = 0xFF005522;
    private static final int C_BTN_OFF  = 0xFF2A0000;
    private static final int C_BTN_DEL  = 0xFF3A0000;
    private static final int C_CAPTURE  = 0xFF006611;

    // Text colors
    private static final int T_MAIN  = 0xFFCCEECC;
    private static final int T_ON    = 0xFF55FF88;
    private static final int T_OFF   = 0xFFFF5555;
    private static final int T_LABEL = 0xFF779977;

    private final FarmHelperConfig config;

    private String  section            = "controls";
    private boolean dropdownOpen       = false;
    private String  capturingFor       = null;
    private boolean addingBind         = false;
    private boolean capturingNewBindKey = false;
    private int     newBindKey         = 0;
    private String  pendingCommandText = "";

    private TextFieldWidget commandField = null;
    private long openTime = 0;

    // Custom buttons rebuilt on state change
    private record Btn(int x, int y, int w, int h, String label, int bg, int fg, Runnable action) {}
    private final List<Btn> buttons = new ArrayList<>();

    public FarmHelperScreen(FarmHelperConfig config) {
        super(Text.literal("FarmHelperHypixel"));
        this.config = config;
    }

    private int wx() { return (width  - WIN_W) / 2; }
    private int wy() { return (height - WIN_H) / 2; }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        if (openTime == 0) {
            openTime = System.currentTimeMillis();
            client.getSoundManager().play(
                PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.6f));
        }
        clearChildren();
        buttons.clear();
        buildButtons();
    }

    private void buildButtons() {
        int wx = wx(), wy = wy();
        int toolY   = wy + TOOL_Y;
        int contY   = wy + CONT_Y;

        // Dropdown
        String dropLabel = (section.equals("controls") ? "Controls" : "Binds") + " ▼";
        btn(wx + 8, toolY, 95, TOOL_H, dropLabel, C_BTN, T_MAIN, () -> {
            dropdownOpen = !dropdownOpen;
            sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0f);
        });

        // Farm Mode toggle
        boolean fm = config.farmModeEnabled;
        btn(wx + WIN_W - 140, toolY, 132, TOOL_H,
                fm ? "✓  Farm Mode  ON" : "✗  Farm Mode  OFF",
                fm ? C_BTN_ON : C_BTN_OFF,
                fm ? T_ON : T_OFF,
                () -> {
                    config.farmModeEnabled = !config.farmModeEnabled;
                    config.save();
                    sound(config.farmModeEnabled
                            ? SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP
                            : SoundEvents.BLOCK_LEVER_CLICK,
                            config.farmModeEnabled ? 1.6f : 0.8f);
                    clearAndInit();
                });

        if (section.equals("controls")) buildControlBtns(wx, contY);
        else                            buildBindBtns(wx, contY);
    }

    private void buildControlBtns(int wx, int contY) {
        String[] labels  = {"Forward", "Back", "Left", "Right", "Attack"};
        int[]    keys    = {config.forwardKey, config.backKey, config.leftKey,
                            config.rightKey, config.attackKey};
        String[] actions = {"forward", "back", "left", "right", "attack"};

        for (int i = 0; i < labels.length; i++) {
            final String act = actions[i];
            final int    key = keys[i];
            boolean cap = act.equals(capturingFor);
            btn(wx + WIN_W - 128, contY + i * 22, 120, 16,
                    cap ? "  Press a key…  " : "  " + keyName(key) + "  ",
                    cap ? C_CAPTURE : C_BTN,
                    cap ? T_ON : T_MAIN,
                    () -> {
                        capturingFor = act;
                        sound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f);
                        clearAndInit();
                    });
        }

        int togY = contY + labels.length * 22 + 10;

        boolean aa = config.autoAttack;
        btn(wx + WIN_W - 80, togY, 72, 16, aa ? "ON" : "OFF",
                aa ? C_BTN_ON : C_BTN_OFF, aa ? T_ON : T_OFF, () -> {
                    config.autoAttack = !config.autoAttack;
                    config.save();
                    sound(config.autoAttack ? SoundEvents.ENTITY_ITEM_PICKUP : SoundEvents.BLOCK_LEVER_CLICK,
                            config.autoAttack ? 1.6f : 0.8f);
                    clearAndInit();
                });

        boolean rs = config.reduceSensitivity;
        btn(wx + WIN_W - 80, togY + 22, 72, 16, rs ? "ON" : "OFF",
                rs ? C_BTN_ON : C_BTN_OFF, rs ? T_ON : T_OFF, () -> {
                    config.reduceSensitivity = !config.reduceSensitivity;
                    config.save();
                    sound(config.reduceSensitivity ? SoundEvents.ENTITY_ITEM_PICKUP : SoundEvents.BLOCK_LEVER_CLICK,
                            config.reduceSensitivity ? 1.6f : 0.8f);
                    clearAndInit();
                });
    }

    private void buildBindBtns(int wx, int contY) {
        List<FarmHelperConfig.CommandBind> binds = config.commandBinds;

        for (int i = 0; i < binds.size(); i++) {
            final int idx = i;
            btn(wx + WIN_W - 30, contY + i * 22, 22, 16, "✕", C_BTN_DEL, T_OFF, () -> {
                config.commandBinds.remove(idx);
                config.save();
                sound(SoundEvents.BLOCK_LEVER_CLICK, 0.7f);
                clearAndInit();
            });
        }

        int addY = contY + binds.size() * 22 + 8;

        if (addingBind) {
            boolean capK = capturingNewBindKey;
            String keyLabel = capK ? "  Press a key…  "
                    : (newBindKey == 0 ? "  [ Set key ]  " : "  " + keyName(newBindKey) + "  ");
            btn(wx + 8, addY, 108, 16, keyLabel,
                    capK ? C_CAPTURE : (newBindKey != 0 ? C_BTN_ON : C_BTN),
                    capK ? T_ON      : (newBindKey != 0 ? T_ON     : T_MAIN),
                    () -> {
                        capturingNewBindKey = true;
                        sound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f);
                        clearAndInit();
                    });

            commandField = new TextFieldWidget(textRenderer, wx + 124, addY, 138, 16, Text.empty());
            commandField.setPlaceholder(Text.literal("/command…"));
            commandField.setText(pendingCommandText);
            commandField.setChangedListener(t -> pendingCommandText = t);
            addDrawableChild(commandField);
            if (newBindKey != 0) setFocused(commandField);

            btn(wx + 270, addY, 22, 16, "✓", C_BTN_ON, T_ON, () -> {
                String cmd = pendingCommandText.replaceFirst("^/", "").trim();
                if (newBindKey != 0 && !cmd.isEmpty()) {
                    config.commandBinds.add(new FarmHelperConfig.CommandBind(newBindKey, cmd));
                    config.save();
                    sound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f);
                    addingBind = false; capturingNewBindKey = false;
                    newBindKey = 0; pendingCommandText = "";
                    clearAndInit();
                }
            });

            btn(wx + 296, addY, 22, 16, "✗", C_BTN_DEL, T_OFF, () -> {
                addingBind = false; capturingNewBindKey = false;
                newBindKey = 0; pendingCommandText = "";
                sound(SoundEvents.BLOCK_LEVER_CLICK, 0.7f);
                clearAndInit();
            });
        } else {
            btn(wx + 8, addY, 92, 16, "+ Add Bind", C_BTN, T_MAIN, () -> {
                addingBind = true;
                sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.2f);
                clearAndInit();
            });
        }
    }

    private void btn(int x, int y, int w, int h, String label, int bg, int fg, Runnable action) {
        buttons.add(new Btn(x, y, w, h, label, bg, fg, action));
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long time = System.currentTimeMillis();
        int wx = wx(), wy = wy();

        // Dim overlay — fades in quickly
        float fadeIn = Math.min(1.0f, (time - openTime) / 180f);
        ctx.fill(0, 0, width, height, argb(0x99, fadeIn));

        // Gradient background (8 bands)
        float shimmer = (float)(Math.sin(time / 2200.0) * 0.5 + 0.5);
        for (int i = 0; i < 8; i++) {
            int bY  = wy + (WIN_H * i) / 8;
            int bY2 = wy + (WIN_H * (i + 1)) / 8;
            int g = (int)(18 + (i / 7.0f) * 22 + shimmer * 16);
            ctx.fill(wx, bY, wx + WIN_W, bY2, 0xFF000000 | (g << 8));
        }

        // Title bar (darker)
        ctx.fill(wx, wy, wx + WIN_W, wy + TITLE_H, 0xFF000B00);

        // Animated title
        float pulse = (float)(Math.sin(time / 650.0) * 0.5 + 0.5);
        int tr = (int)(100 + pulse * 100);
        int tg = 230;
        int tb = (int)(100 + pulse * 100);
        int titleCol = 0xFF000000 | (tr << 16) | (tg << 8) | tb;
        String titleText = "✶  F A R M H E L P E R  ✶";
        int tw = textRenderer.getWidth(titleText);
        ctx.drawText(textRenderer, titleText, wx + (WIN_W - tw) / 2, wy + 10, titleCol, true);

        // Borders
        ctx.fill(wx,           wy,            wx + WIN_W,     wy + 2,             0xFF000000);
        ctx.fill(wx,           wy + WIN_H - 2, wx + WIN_W,    wy + WIN_H,         0xFF000000);
        ctx.fill(wx,           wy,            wx + 2,          wy + WIN_H,         0xFF000000);
        ctx.fill(wx + WIN_W-2, wy,            wx + WIN_W,      wy + WIN_H,         0xFF000000);
        // Inner highlight glow
        ctx.fill(wx + 2, wy + 2, wx + WIN_W - 2, wy + 3,             0x22006600);
        ctx.fill(wx + 2, wy + 2, wx + 3,         wy + WIN_H - 2,     0x22006600);

        // Separators
        ctx.fill(wx + 2, wy + TITLE_H,             wx + WIN_W - 2, wy + TITLE_H + 1,     0x88003800);
        ctx.fill(wx + 2, wy + TOOL_Y + TOOL_H + 2, wx + WIN_W - 2, wy + TOOL_Y + TOOL_H + 3, 0x55003300);

        // Section labels
        int contY = wy + CONT_Y;
        if (section.equals("controls")) {
            String[] rowLabels = {"Forward", "Back", "Left", "Right", "Attack"};
            for (int i = 0; i < rowLabels.length; i++)
                ctx.drawText(textRenderer, rowLabels[i] + ":", wx + 14, contY + i * 22 + 4, T_LABEL, true);
            int togY = contY + rowLabels.length * 22 + 10;
            ctx.drawText(textRenderer, "Auto-attack:",     wx + 14, togY + 4,      T_LABEL, true);
            ctx.drawText(textRenderer, "Low sensitivity:", wx + 14, togY + 22 + 4, T_LABEL, true);
        } else {
            List<FarmHelperConfig.CommandBind> binds = config.commandBinds;
            if (binds.isEmpty())
                ctx.drawText(textRenderer, "No binds yet. Add one below.", wx + 14, contY + 4, T_LABEL, true);
            for (int i = 0; i < binds.size(); i++) {
                FarmHelperConfig.CommandBind b = binds.get(i);
                ctx.drawText(textRenderer, "[ " + keyName(b.key) + " ]  →  /" + b.command,
                        wx + 14, contY + i * 22 + 4, T_MAIN, true);
            }
        }

        // Decorative diamonds
        renderDiamonds(ctx, wx, wy, time);

        // Custom buttons
        for (Btn b : buttons) drawBtn(ctx, b, mx, my, time);

        // Real widgets (TextFieldWidget)
        super.render(ctx, mx, my, delta);

        // Dropdown on top (must be after super.render so it's on top layer)
        if (dropdownOpen) renderDropdown(ctx, wx, wy, mx, my);
    }

    private void drawBtn(DrawContext ctx, Btn b, int mx, int my, long time) {
        boolean hov = mx >= b.x && mx <= b.x + b.w && my >= b.y && my <= b.y + b.h;
        int bg = b.bg;
        if (hov) {
            int r = (bg >> 16) & 0xFF;
            int g = Math.min(255, ((bg >> 8) & 0xFF) + 35);
            int bl = bg & 0xFF;
            bg = 0xFF000000 | (r << 16) | (g << 8) | bl;
        }
        ctx.fill(b.x, b.y, b.x + b.w, b.y + b.h, bg);

        // Border (brighter on hover)
        int bdr = hov ? 0xFF006600 : 0xFF002800;
        ctx.fill(b.x,           b.y,           b.x + b.w,     b.y + 1,     bdr);
        ctx.fill(b.x,           b.y + b.h - 1, b.x + b.w,     b.y + b.h,   bdr);
        ctx.fill(b.x,           b.y,           b.x + 1,       b.y + b.h,   bdr);
        ctx.fill(b.x + b.w - 1, b.y,           b.x + b.w,     b.y + b.h,   bdr);

        // Top highlight strip
        ctx.fill(b.x + 1, b.y + 1, b.x + b.w - 1, b.y + 2, 0x18FFFFFF);

        int tx = b.x + (b.w - textRenderer.getWidth(b.label)) / 2;
        int ty = b.y + (b.h - 7) / 2;
        ctx.drawText(textRenderer, b.label, tx, ty, b.fg, true);
    }

    private void renderDropdown(DrawContext ctx, int wx, int wy, int mx, int my) {
        int dx   = wx + 8;
        int dy   = wy + TOOL_Y + TOOL_H + 2;
        int dw   = 95;
        int dh   = 18;
        String[] names = {"Controls", "Binds"};
        String[] ids   = {"controls", "binds"};

        // Panel shadow
        ctx.fill(dx + 2, dy + 2, dx + dw + 2, dy + names.length * dh + 2, 0x55000000);
        // Panel border
        ctx.fill(dx - 1, dy - 1, dx + dw + 1, dy + names.length * dh + 1, 0xFF000000);

        for (int i = 0; i < names.length; i++) {
            int oy  = dy + i * dh;
            boolean active = section.equals(ids[i]);
            boolean hov    = mx >= dx && mx <= dx + dw && my >= oy && my <= oy + dh;
            int bg = active ? 0xFF004A22 : (hov ? 0xFF003300 : 0xFF001800);
            ctx.fill(dx, oy, dx + dw, oy + dh, bg);
            ctx.fill(dx, oy, dx + dw, oy + 1, 0xFF002200);
            int fg = active ? T_ON : (hov ? T_MAIN : T_LABEL);
            if (active) ctx.drawText(textRenderer, "✓ " + names[i], dx + 5, oy + 5, fg, true);
            else        ctx.drawText(textRenderer, "  " + names[i], dx + 5, oy + 5, fg, true);
        }
    }

    private void renderDiamonds(DrawContext ctx, int wx, int wy, long time) {
        double phase = time / 7000.0;
        // Main decorative diamonds
        int[][] shapes = {
            {wx + 50,  wy + 155, 16},
            {wx + 140, wy + 180, 12},
            {wx + 230, wy + 150, 15},
            {wx + 305, wy + 185, 10},
            {wx + 90,  wy + 215, 9},
            {wx + 330, wy + 115, 13},
        };
        for (int i = 0; i < shapes.length; i++) {
            float p = (float)(Math.sin(phase * 2.1 + i * 0.85) * 0.5 + 0.5);
            int cx = shapes[i][0] + (int)(Math.sin(phase + i * 1.15) * 7);
            int cy = shapes[i][1] + (int)(Math.cos(phase * 0.8 + i)  * 5);
            int alpha = (int)(15 + p * 28);
            diamond(ctx, cx, cy, shapes[i][2], (alpha << 24) | 0x00BB00);
        }
        // Small sparkle dots
        for (int i = 0; i < 5; i++) {
            double sp = phase * 4.0 + i * 1.3;
            double sv = Math.sin(sp);
            if (sv > 0.85) {
                int sx = wx + 30 + i * 62 + (int)(Math.cos(sp * 0.7) * 12);
                int sy = wy + WIN_H - 40 + (int)(Math.sin(sp * 0.5) * 18);
                int a  = (int)((sv - 0.85) / 0.15 * 110);
                ctx.fill(sx, sy, sx + 2, sy + 2, (a << 24) | 0x0066FF00);
            }
        }
    }

    private void diamond(DrawContext ctx, int cx, int cy, int size, int color) {
        for (int dy = -size; dy <= size; dy++) {
            int hw = size - Math.abs(dy);
            if (hw > 0) ctx.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, color);
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (dropdownOpen) {
            int wx = wx(), wy = wy();
            int dx = wx + 8;
            int dy = wy + TOOL_Y + TOOL_H + 2;
            int dw = 95, dh = 18;
            String[] ids = {"controls", "binds"};

            for (int i = 0; i < ids.length; i++) {
                int oy = dy + i * dh;
                if (click.x() >= dx && click.x() <= dx + dw && click.y() >= oy && click.y() <= oy + dh) {
                    if (!section.equals(ids[i])) {
                        section = ids[i];
                        capturingFor = null;
                        addingBind = capturingNewBindKey = false;
                        newBindKey = 0; pendingCommandText = "";
                    }
                    dropdownOpen = false;
                    sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0f);
                    clearAndInit();
                    return true;
                }
            }
            dropdownOpen = false;
            clearAndInit();
            return true;
        }

        for (Btn b : buttons) {
            if (click.x() >= b.x && click.x() <= b.x + b.w &&
                click.y() >= b.y && click.y() <= b.y + b.h) {
                b.action().run();
                return true;
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int k = input.key();

        if (capturingFor != null) {
            switch (capturingFor) {
                case "forward" -> config.forwardKey = k;
                case "back"    -> config.backKey    = k;
                case "left"    -> config.leftKey    = k;
                case "right"   -> config.rightKey   = k;
                case "attack"  -> config.attackKey  = k;
            }
            config.save();
            sound(SoundEvents.ENTITY_ITEM_PICKUP, 1.9f);
            capturingFor = null;
            clearAndInit();
            return true;
        }

        if (capturingNewBindKey) {
            newBindKey = k;
            capturingNewBindKey = false;
            sound(SoundEvents.ENTITY_ITEM_PICKUP, 1.9f);
            clearAndInit();
            return true;
        }

        if (k == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }

        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() { return false; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void sound(net.minecraft.sound.SoundEvent se, float pitch) {
        if (client != null) client.getSoundManager().play(PositionedSoundInstance.ui(se, pitch));
    }

    private static int argb(int baseAlpha, float multiplier) {
        return ((int)(baseAlpha * multiplier) << 24);
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
            case GLFW.GLFW_KEY_F1  -> "F1";  case GLFW.GLFW_KEY_F2  -> "F2";
            case GLFW.GLFW_KEY_F3  -> "F3";  case GLFW.GLFW_KEY_F4  -> "F4";
            case GLFW.GLFW_KEY_F5  -> "F5";  case GLFW.GLFW_KEY_F6  -> "F6";
            case GLFW.GLFW_KEY_F7  -> "F7";  case GLFW.GLFW_KEY_F8  -> "F8";
            case GLFW.GLFW_KEY_F9  -> "F9";  case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11"; case GLFW.GLFW_KEY_F12 -> "F12";
            case GLFW.GLFW_KEY_LEFT_SHIFT,   GLFW.GLFW_KEY_RIGHT_SHIFT   -> "Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT,     GLFW.GLFW_KEY_RIGHT_ALT     -> "Alt";
            default -> {
                String n = GLFW.glfwGetKeyName(key, 0);
                yield n != null ? n.toUpperCase() : "Key" + key;
            }
        };
    }
}
