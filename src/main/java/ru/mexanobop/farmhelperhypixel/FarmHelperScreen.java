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

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int WIN_W  = 504;
    private static final int WIN_H  = 324;
    private static final int TOP_H  = 40;   // header with progress dots
    private static final int BOT_H  = 32;   // bottom navigation bar
    private static final int LEFT_W = 176;  // description panel width

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_BG       = 0xFF0C0C0C;
    private static final int C_LEFT     = 0xFF0F0F0F;
    private static final int C_SEP      = 0xFF1E1E1E;
    private static final int C_BORDER   = 0xFF232323;
    private static final int C_ITEM     = 0xFF161616;
    private static final int C_ACTIVE   = 0xFF0C3526;
    private static final int C_ACTIVE_B = 0xFF1D6247; // active border
    private static final int C_BTN      = 0xFF181818;
    private static final int C_HEADER   = 0xFF080808;

    // ── Text ──────────────────────────────────────────────────────────────────
    private static final int T_WHITE  = 0xFFFFFFFF;
    private static final int T_MAIN   = 0xFFE8E8E8;
    private static final int T_GREY   = 0xFF999999;
    private static final int T_DIM    = 0xFF555555;
    private static final int T_GREEN  = 0xFF5BE38B;
    private static final int T_RED    = 0xFFE35B5B;

    // ── Sections ──────────────────────────────────────────────────────────────
    private static final String[] SEC_NAMES = {"Controls", "Commands", "Farm Mode"};
    private static final String[] SEC_DESC  = {
        "Rebind the keys that are active when farm mode is enabled. Movement and attack keys will be remapped to your chosen bindings.",
        "Add key-to-command bindings. While farm mode is on, pressing the bound key will send the command to the server automatically.",
        "Toggle farm mode on or off. When active, all your key bindings and settings are applied. Press F8 at any time to open this menu."
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private final FarmHelperConfig config;
    private int     section             = 0;
    private String  capturingFor        = null;
    private boolean addingBind          = false;
    private boolean capturingNewBindKey = false;
    private int     newBindKey          = 0;
    private String  pendingCmd          = "";
    private TextFieldWidget cmdField    = null;
    private long    openTime            = 0;

    private record Btn(int x, int y, int w, int h, String label, int bg, int fg, boolean activeBorder, Runnable action) {}
    private final List<Btn> btns = new ArrayList<>();

    public FarmHelperScreen(FarmHelperConfig config) {
        super(Text.literal("FarmHelperHypixel"));
        this.config = config;
    }

    private int wx() { return (width  - WIN_W) / 2; }
    private int wy() { return (height - WIN_H) / 2; }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        if (openTime == 0) {
            openTime = System.currentTimeMillis();
            client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.6f));
        }
        clearChildren();
        btns.clear();
        cmdField = null;
        buildButtons();
    }

    private void buildButtons() {
        int wx = wx(), wy = wy();
        int rx  = wx + LEFT_W + 1;
        int rw  = WIN_W - LEFT_W - 2;
        int cy  = wy + TOP_H;
        int by  = wy + WIN_H - BOT_H;

        // Right panel buttons per section
        switch (section) {
            case 0 -> buildControlBtns(rx, cy, rw);
            case 1 -> buildBindBtns(wx, rx, cy, rw);
            case 2 -> buildStatusBtns(rx, cy, rw);
        }

        // Back
        if (section > 0)
            btn(wx + 10, by + 8, 62, 16, "← Back", C_BTN, T_GREY, false, this::goBack);

        // Next / Done
        boolean last = section == SEC_NAMES.length - 1;
        btn(wx + WIN_W - 84, by + 8, 74, 16,
                last ? "Done  ✓" : "Next  →",
                last ? C_ACTIVE : C_BTN,
                last ? T_GREEN : T_MAIN,
                last, this::goNext);
    }

    private void buildControlBtns(int rx, int cy, int rw) {
        String[] labels  = {"Forward", "Back", "Left", "Right", "Attack"};
        int[]    keys    = {config.forwardKey, config.backKey, config.leftKey, config.rightKey, config.attackKey};
        String[] actions = {"forward", "back", "left", "right", "attack"};

        for (int i = 0; i < labels.length; i++) {
            final String act = actions[i];
            boolean cap = act.equals(capturingFor);
            String lbl = cap ? "Press a key…" : keyName(keys[i]);
            btn(rx + rw - 102, cy + 12 + i * 26, 96, 16,
                    lbl, cap ? C_ACTIVE : C_ITEM, cap ? T_GREEN : T_MAIN, cap,
                    () -> { capturingFor = act; sound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f); clearAndInit(); });
        }

        int ty = cy + 12 + labels.length * 26 + 10;
        boolean aa = config.autoAttack;
        btn(rx + rw - 56, ty, 50, 14, aa ? "ON" : "OFF",
                aa ? C_ACTIVE : C_ITEM, aa ? T_GREEN : T_RED, aa,
                () -> { config.autoAttack = !config.autoAttack; config.save();
                        sound(config.autoAttack ? SoundEvents.ENTITY_ITEM_PICKUP : SoundEvents.BLOCK_LEVER_CLICK,
                              config.autoAttack ? 1.6f : 0.8f); clearAndInit(); });

        boolean rs = config.reduceSensitivity;
        btn(rx + rw - 56, ty + 22, 50, 14, rs ? "ON" : "OFF",
                rs ? C_ACTIVE : C_ITEM, rs ? T_GREEN : T_RED, rs,
                () -> { config.reduceSensitivity = !config.reduceSensitivity; config.save();
                        sound(config.reduceSensitivity ? SoundEvents.ENTITY_ITEM_PICKUP : SoundEvents.BLOCK_LEVER_CLICK,
                              config.reduceSensitivity ? 1.6f : 0.8f); clearAndInit(); });
    }

    private void buildBindBtns(int wx, int rx, int cy, int rw) {
        List<FarmHelperConfig.CommandBind> binds = config.commandBinds;
        for (int i = 0; i < binds.size(); i++) {
            final int idx = i;
            btn(rx + rw - 24, cy + 12 + i * 26, 18, 16, "✕", C_ITEM, T_RED, false,
                () -> { config.commandBinds.remove(idx); config.save();
                        sound(SoundEvents.BLOCK_LEVER_CLICK, 0.7f); clearAndInit(); });
        }

        int ay = cy + 12 + binds.size() * 26 + 8;

        if (addingBind) {
            boolean capK = capturingNewBindKey;
            String kl = capK ? "Press a key…" : (newBindKey == 0 ? "[ Set key ]" : keyName(newBindKey));
            btn(rx + 6, ay, 96, 16, kl,
                    (capK || newBindKey != 0) ? C_ACTIVE : C_ITEM,
                    (capK || newBindKey != 0) ? T_GREEN : T_GREY,
                    newBindKey != 0 && !capK,
                    () -> { capturingNewBindKey = true; sound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f); clearAndInit(); });

            cmdField = new TextFieldWidget(textRenderer, rx + 110, ay, rw - 142, 16, Text.empty());
            cmdField.setPlaceholder(Text.literal("/command…"));
            cmdField.setText(pendingCmd);
            cmdField.setChangedListener(t -> pendingCmd = t);
            addDrawableChild(cmdField);
            if (newBindKey != 0) setFocused(cmdField);

            btn(rx + rw - 24, ay, 18, 16, "✓", C_ACTIVE, T_GREEN, true, () -> {
                String cmd = pendingCmd.replaceFirst("^/", "").trim();
                if (newBindKey != 0 && !cmd.isEmpty()) {
                    config.commandBinds.add(new FarmHelperConfig.CommandBind(newBindKey, cmd));
                    config.save();
                    sound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f);
                    addingBind = capturingNewBindKey = false;
                    newBindKey = 0; pendingCmd = "";
                    clearAndInit();
                }
            });
        } else {
            btn(rx + 6, ay, 96, 16, "+ Add Bind", C_ITEM, T_GREY, false,
                () -> { addingBind = true; sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.2f); clearAndInit(); });
        }
    }

    private void buildStatusBtns(int rx, int cy, int rw) {
        boolean fm = config.farmModeEnabled;
        btn(rx + 10, cy + 18, rw - 20, 28,
                fm ? "✓   Farm Mode is ON" : "✗   Farm Mode is OFF",
                fm ? C_ACTIVE : C_ITEM, fm ? T_GREEN : T_RED, fm,
                () -> { config.farmModeEnabled = !config.farmModeEnabled; config.save();
                        sound(config.farmModeEnabled ? SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP : SoundEvents.BLOCK_LEVER_CLICK,
                              config.farmModeEnabled ? 1.6f : 0.8f); clearAndInit(); });
    }

    private void btn(int x, int y, int w, int h, String label, int bg, int fg, boolean ab, Runnable action) {
        btns.add(new Btn(x, y, w, h, label, bg, fg, ab, action));
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long time = System.currentTimeMillis();
        int wx = wx(), wy = wy();
        float fade = Math.min(1.0f, (time - openTime) / 180f);

        // Dim overlay
        ctx.fill(0, 0, width, height, (int)(0xCC * fade) << 24);

        // Window panels
        ctx.fill(wx, wy, wx + WIN_W, wy + WIN_H, C_BG);
        ctx.fill(wx, wy + TOP_H, wx + LEFT_W, wy + WIN_H - BOT_H, C_LEFT);
        ctx.fill(wx, wy, wx + WIN_W, wy + TOP_H, C_HEADER);
        ctx.fill(wx, wy + WIN_H - BOT_H, wx + WIN_W, wy + WIN_H, C_HEADER);

        // Borders & separators
        border(ctx, wx, wy, WIN_W, WIN_H, C_BORDER);
        ctx.fill(wx + 1, wy + TOP_H,             wx + WIN_W - 1, wy + TOP_H + 1,         C_SEP);
        ctx.fill(wx + 1, wy + WIN_H - BOT_H,     wx + WIN_W - 1, wy + WIN_H - BOT_H + 1, C_SEP);
        ctx.fill(wx + LEFT_W, wy + TOP_H + 1, wx + LEFT_W + 1, wy + WIN_H - BOT_H - 1, C_SEP);

        // Header
        renderHeader(ctx, wx, wy, mx, my);

        // Left description panel
        renderLeft(ctx, wx, wy);

        // Right content panel labels
        int rx = wx + LEFT_W + 1, rw = WIN_W - LEFT_W - 2, cy = wy + TOP_H;
        switch (section) {
            case 0 -> renderControlsLabels(ctx, rx, cy, rw);
            case 1 -> renderBindsLabels(ctx, rx, cy, rw);
            case 2 -> renderStatusLabels(ctx, rx, cy, rw);
        }

        // Footer
        renderFooter(ctx, wx, wy, mx, my);

        // Custom buttons
        for (Btn b : btns) renderBtn(ctx, b, mx, my);

        // Real widgets
        super.render(ctx, mx, my, delta);
    }

    private void renderHeader(DrawContext ctx, int wx, int wy, int mx, int my) {
        // Mod name (left)
        ctx.drawText(textRenderer, "FarmHelperHypixel", wx + 10, wy + 6, T_DIM, false);

        // Progress dots centered
        int n = SEC_NAMES.length;
        int dotR = 4, gap = 38;
        int totalW = n * (dotR * 2) + (n - 1) * gap;
        int dx0 = wx + (WIN_W - totalW) / 2;
        int dotCY = wy + TOP_H / 2 - 2;

        for (int i = 0; i < n; i++) {
            int cx = dx0 + i * (dotR * 2 + gap) + dotR;

            // Connecting line to next dot
            if (i < n - 1) {
                int lx1 = cx + dotR + 2, lx2 = cx + dotR + gap - 1;
                int lc = i < section ? C_ACTIVE_B : C_BORDER;
                ctx.fill(lx1, dotCY - 1, lx2, dotCY + 2, lc);
            }

            // Circle fill
            boolean active = i == section, done = i < section;
            int cc = active ? T_GREEN : (done ? C_ACTIVE_B : C_BORDER);
            circle(ctx, cx, dotCY, dotR, cc);

            // Number inside
            String num = String.valueOf(i + 1);
            int nc = active ? 0xFF003018 : (done ? 0xFF003018 : C_BG);
            ctx.drawText(textRenderer, num,
                    cx - textRenderer.getWidth(num) / 2,
                    dotCY - 3, nc, false);

            // Section name below dot
            String sn = SEC_NAMES[i];
            int snX = cx - textRenderer.getWidth(sn) / 2;
            int snC = active ? T_GREEN : (done ? T_GREY : T_DIM);
            ctx.drawText(textRenderer, sn, snX, dotCY + dotR + 4, snC, false);
        }
    }

    private void renderLeft(DrawContext ctx, int wx, int wy) {
        int x = wx + 12, y = wy + TOP_H + 16, maxW = LEFT_W - 22;

        // Section title
        ctx.drawText(textRenderer, SEC_NAMES[section], x, y, T_WHITE, false);
        y += 12;
        ctx.fill(x, y, wx + LEFT_W - 10, y + 1, C_SEP);
        y += 6;

        // Wrapped description
        for (String line : wrapText(SEC_DESC[section], maxW)) {
            ctx.drawText(textRenderer, line, x, y, T_GREY, false);
            y += 10;
        }
    }

    private void renderControlsLabels(DrawContext ctx, int rx, int cy, int rw) {
        String[] rows = {"Forward", "Back", "Left", "Right", "Attack"};
        for (int i = 0; i < rows.length; i++) {
            int ry = cy + 12 + i * 26;
            ctx.fill(rx + 4, ry, rx + rw - 4, ry + 18, C_ITEM);
            ctx.drawText(textRenderer, rows[i], rx + 12, ry + 5, T_MAIN, false);
        }
        int ty = cy + 12 + rows.length * 26 + 10;
        ctx.drawText(textRenderer, "Auto-attack:",     rx + 12, ty + 3,      T_GREY, false);
        ctx.drawText(textRenderer, "Low sensitivity:", rx + 12, ty + 3 + 22, T_GREY, false);
    }

    private void renderBindsLabels(DrawContext ctx, int rx, int cy, int rw) {
        List<FarmHelperConfig.CommandBind> binds = config.commandBinds;
        if (binds.isEmpty()) {
            ctx.drawText(textRenderer, "No binds yet.", rx + 12, cy + 16, T_DIM, false);
        }
        for (int i = 0; i < binds.size(); i++) {
            FarmHelperConfig.CommandBind b = binds.get(i);
            int ry = cy + 12 + i * 26;
            ctx.fill(rx + 4, ry, rx + rw - 4, ry + 18, C_ITEM);
            ctx.drawText(textRenderer, "[ " + keyName(b.key) + " ]", rx + 12, ry + 5, T_GREEN, false);
            ctx.drawText(textRenderer, "→  /" + b.command,
                    rx + 12 + textRenderer.getWidth("[ " + keyName(b.key) + " ]") + 4, ry + 5, T_MAIN, false);
        }
    }

    private void renderStatusLabels(DrawContext ctx, int rx, int cy, int rw) {
        // Binding summary below the toggle
        int sy = cy + 56;
        ctx.drawText(textRenderer, "Active bindings", rx + 14, sy, T_DIM, false);
        sy += 12;

        String[][] rows = {
            {"Forward",  keyName(config.forwardKey)},
            {"Back",     keyName(config.backKey)},
            {"Left",     keyName(config.leftKey)},
            {"Right",    keyName(config.rightKey)},
            {"Attack",   keyName(config.attackKey)},
            {"Auto-attack",   config.autoAttack        ? "ON" : "OFF"},
            {"Low sensitivity", config.reduceSensitivity ? "ON" : "OFF"},
        };
        boolean fm = config.farmModeEnabled;
        for (String[] row : rows) {
            ctx.drawText(textRenderer, row[0] + ":", rx + 14, sy, T_DIM, false);
            boolean isToggle = row[1].equals("ON") || row[1].equals("OFF");
            int vc = isToggle ? (row[1].equals("ON") ? T_GREEN : T_RED)
                              : (fm ? T_MAIN : T_DIM);
            ctx.drawText(textRenderer, row[1], rx + 110, sy, vc, false);
            sy += 10;
        }

        if (!config.commandBinds.isEmpty()) {
            ctx.drawText(textRenderer, config.commandBinds.size() + " command bind(s)", rx + 14, sy + 2, T_DIM, false);
        }
    }

    private void renderFooter(DrawContext ctx, int wx, int wy, int mx, int my) {
        int botY = wy + WIN_H - BOT_H;
        boolean fm = config.farmModeEnabled;
        String statusTxt = fm ? "✓ Farm Mode active" : "○ Farm Mode off";
        int sc = fm ? T_GREEN : T_DIM;
        int sx = wx + (WIN_W - textRenderer.getWidth(statusTxt)) / 2;
        ctx.drawText(textRenderer, statusTxt, sx, botY + 12, sc, false);
    }

    private void renderBtn(DrawContext ctx, Btn b, int mx, int my) {
        boolean hov = mx >= b.x && mx <= b.x + b.w && my >= b.y && my <= b.y + b.h;
        int bg = hov ? brighten(b.bg, 22) : b.bg;
        ctx.fill(b.x, b.y, b.x + b.w, b.y + b.h, bg);

        int borderCol = b.activeBorder ? C_ACTIVE_B : C_BORDER;
        border(ctx, b.x, b.y, b.w, b.h, borderCol);

        if (hov && !b.activeBorder) {
            ctx.fill(b.x + 1, b.y + 1, b.x + b.w - 1, b.y + 2, 0x14FFFFFF);
        }

        int tx = b.x + (b.w - textRenderer.getWidth(b.label)) / 2;
        int ty = b.y + (b.h - 7) / 2;
        ctx.drawText(textRenderer, b.label, tx, ty, hov ? brightenText(b.fg) : b.fg, false);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        // Progress dot click (expanded hit area)
        int wx = wx(), wy = wy();
        int n = SEC_NAMES.length, dotR = 4, gap = 38;
        int totalW = n * (dotR * 2) + (n - 1) * gap;
        int dx0 = wx + (WIN_W - totalW) / 2;
        int dotCY = wy + TOP_H / 2 - 2;

        for (int i = 0; i < n; i++) {
            int cx = dx0 + i * (dotR * 2 + gap) + dotR;
            if (Math.abs(click.x() - cx) <= 10 && Math.abs(click.y() - dotCY) <= 10) {
                if (i != section) {
                    navigateTo(i);
                    sound(SoundEvents.ENTITY_ITEM_PICKUP, 1.8f);
                }
                return true;
            }
        }

        // Custom buttons
        for (Btn b : btns) {
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

    // ── Navigation ────────────────────────────────────────────────────────────

    private void goNext() {
        if (section < SEC_NAMES.length - 1) {
            navigateTo(section + 1);
            sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.1f);
        } else {
            sound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f);
            close();
        }
    }

    private void goBack() {
        if (section > 0) {
            navigateTo(section - 1);
            sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.9f);
        }
    }

    private void navigateTo(int idx) {
        section = idx;
        capturingFor = null;
        addingBind = capturingNewBindKey = false;
        newBindKey = 0;
        pendingCmd = "";
        clearAndInit();
    }

    // ── Drawing utilities ─────────────────────────────────────────────────────

    private void border(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x,       y,       x + w,   y + 1,   c);
        ctx.fill(x,       y+h-1,   x + w,   y + h,   c);
        ctx.fill(x,       y,       x + 1,   y + h,   c);
        ctx.fill(x+w-1,   y,       x + w,   y + h,   c);
    }

    private void circle(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt((double) r * r - (double) dy * dy);
            ctx.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private static int brighten(int argb, int amt) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + amt);
        int g = Math.min(255, ((argb >> 8)  & 0xFF) + amt);
        int b = Math.min(255, (argb & 0xFF)          + amt);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int brightenText(int c) {
        if (c == T_GREY) return T_MAIN;
        if (c == T_DIM)  return T_GREY;
        return c;
    }

    private List<String> wrapText(String text, int maxW) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (textRenderer.getWidth(test) > maxW) {
                if (!line.isEmpty()) { lines.add(line.toString()); line = new StringBuilder(); }
                line.append(word);
            } else {
                if (!line.isEmpty()) line.append(" ");
                line.append(word);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private void sound(net.minecraft.sound.SoundEvent se, float pitch) {
        if (client != null) client.getSoundManager().play(PositionedSoundInstance.ui(se, pitch));
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
