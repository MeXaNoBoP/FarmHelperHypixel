package ru.mexanobop.farmhelperhypixel;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class FarmHelperScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int WIN_W  = 504;
    private static final int WIN_H  = 332;
    private static final int TOP_H  = 44;
    private static final int BOT_H  = 32;
    private static final int LEFT_W = 176;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_BG       = 0xFF0C0C0C;
    private static final int C_LEFT     = 0xFF0F0F0F;
    private static final int C_SEP      = 0xFF1E1E1E;
    private static final int C_BORDER   = 0xFF232323;
    private static final int C_ITEM     = 0xFF161616;
    private static final int C_ACTIVE   = 0xFF0C3526;
    private static final int C_ACTIVE_B = 0xFF1D6247;
    private static final int C_BTN      = 0xFF181818;
    private static final int C_HEADER   = 0xFF080808;

    // ── Text ──────────────────────────────────────────────────────────────────
    private static final int T_WHITE = 0xFFFFFFFF;
    private static final int T_MAIN  = 0xFFE8E8E8;
    private static final int T_GREY  = 0xFF999999;
    private static final int T_DIM   = 0xFF555555;
    private static final int T_GREEN = 0xFF5BE38B;
    private static final int T_RED   = 0xFFE35B5B;

    // ── Section keys (4 sections) ─────────────────────────────────────────────
    private static final String[] SEC_KEYS = {
        "farmhelperhypixel.section.controls",
        "farmhelperhypixel.section.commands",
        "farmhelperhypixel.section.inversion",
        "farmhelperhypixel.section.farmmode"
    };
    private static final String[] DESC_KEYS = {
        "farmhelperhypixel.desc.controls",
        "farmhelperhypixel.desc.commands",
        "farmhelperhypixel.desc.inversion",
        "farmhelperhypixel.desc.farmmode"
    };

    // ── Persistent state ──────────────────────────────────────────────────────
    private static int lastSection = 0;

    // ── Per-open state ────────────────────────────────────────────────────────
    private final FarmHelperConfig config;
    private int     section             = lastSection;
    private int     windowX             = -1;
    private int     windowY             = -1;
    private boolean dragging            = false;
    private int     dragOffX, dragOffY;
    private long    openTime            = 0;

    // Controls
    private String  capturingFor        = null;

    // Commands
    private boolean addingBind          = false;
    private boolean capturingNewBindKey = false;
    private int     newBindKey          = 0;
    private String  pendingCmd          = "";
    private TextFieldWidget cmdField    = null;

    // Inversion
    private boolean addingInversion     = false;
    private boolean capturingInvKey1    = false;
    private boolean capturingInvKey2    = false;
    private int     newInvKey1          = 0;
    private int     newInvKey2          = 0;

    // Buttons store RELATIVE positions (relative to window top-left)
    private record Btn(int rx, int ry, int w, int h, String label, int bg, int fg, boolean ab, Runnable action) {}
    private final List<Btn> btns = new ArrayList<>();

    public FarmHelperScreen(FarmHelperConfig config) {
        super(Text.literal("FarmHelperHypixel"));
        this.config = config;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        if (openTime == 0) {
            openTime = System.currentTimeMillis();
            client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.6f));
        }
        if (windowX < 0) {
            windowX = (width  - WIN_W) / 2;
            windowY = (height - WIN_H) / 2;
        }
        clearChildren();
        btns.clear();
        cmdField = null;
        buildButtons();
    }

    private void buildButtons() {
        // All coordinates are RELATIVE to window (0,0)
        int rpx = LEFT_W + 1;
        int rpw = WIN_W - LEFT_W - 2;
        int cy  = TOP_H;
        int by  = WIN_H - BOT_H;

        switch (section) {
            case 0 -> buildControlBtns(rpx, cy, rpw);
            case 1 -> buildBindBtns(rpx, cy, rpw);
            case 2 -> buildInversionBtns(rpx, cy, rpw);
            case 3 -> buildFarmModeBtns(rpx, cy, rpw);
        }

        if (section > 0)
            btn(10, by + 8, 62, 16, t("farmhelperhypixel.btn.back"), C_BTN, T_GREY, false, this::goBack);

        boolean last = section == SEC_KEYS.length - 1;
        btn(WIN_W - 84, by + 8, 74, 16,
                last ? t("farmhelperhypixel.btn.done") : t("farmhelperhypixel.btn.next"),
                last ? C_ACTIVE : C_BTN,
                last ? T_GREEN : T_MAIN,
                last, this::goNext);
    }

    private void buildControlBtns(int rpx, int cy, int rpw) {
        String[] actions = {"forward", "back", "left", "right", "attack"};
        for (int i = 0; i < actions.length; i++) {
            final String act = actions[i];
            boolean cap = act.equals(capturingFor);
            int[] keys = {config.forwardKey, config.backKey, config.leftKey, config.rightKey, config.attackKey};
            String lbl = cap ? t("farmhelperhypixel.btn.presskey") : keyName(keys[i]);
            btn(rpx + rpw - 102, cy + 12 + i * 26, 96, 16,
                    lbl, cap ? C_ACTIVE : C_ITEM, cap ? T_GREEN : T_MAIN, cap,
                    () -> { capturingFor = act; sound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f); clearAndInit(); });
        }

        int ty = cy + 12 + 5 * 26 + 10;
        boolean aa = config.autoAttack;
        btn(rpx + rpw - 56, ty, 50, 14, aa ? t("farmhelperhypixel.btn.on") : t("farmhelperhypixel.btn.off"),
                aa ? C_ACTIVE : C_ITEM, aa ? T_GREEN : T_RED, aa,
                () -> { config.autoAttack = !config.autoAttack; config.save();
                        sound(config.autoAttack ? SoundEvents.ENTITY_ITEM_PICKUP : SoundEvents.BLOCK_LEVER_CLICK,
                              config.autoAttack ? 1.6f : 0.8f); clearAndInit(); });

        boolean rs = config.reduceSensitivity;
        btn(rpx + rpw - 56, ty + 22, 50, 14, rs ? t("farmhelperhypixel.btn.on") : t("farmhelperhypixel.btn.off"),
                rs ? C_ACTIVE : C_ITEM, rs ? T_GREEN : T_RED, rs,
                () -> { config.reduceSensitivity = !config.reduceSensitivity; config.save();
                        sound(config.reduceSensitivity ? SoundEvents.ENTITY_ITEM_PICKUP : SoundEvents.BLOCK_LEVER_CLICK,
                              config.reduceSensitivity ? 1.6f : 0.8f); clearAndInit(); });
    }

    private void buildBindBtns(int rpx, int cy, int rpw) {
        List<FarmHelperConfig.CommandBind> binds = config.commandBinds;
        for (int i = 0; i < binds.size(); i++) {
            final int idx = i;
            btn(rpx + rpw - 24, cy + 12 + i * 26, 18, 16, "✕", C_ITEM, T_RED, false,
                () -> { config.commandBinds.remove(idx); config.save();
                        sound(SoundEvents.BLOCK_LEVER_CLICK, 0.7f); clearAndInit(); });
        }
        int ay = cy + 12 + binds.size() * 26 + 8;
        if (addingBind) {
            boolean capK = capturingNewBindKey;
            String kl = capK ? t("farmhelperhypixel.btn.presskey")
                             : (newBindKey == 0 ? t("farmhelperhypixel.btn.setkey1") : keyName(newBindKey));
            btn(rpx + 6, ay, 96, 16, kl,
                    (capK || newBindKey != 0) ? C_ACTIVE : C_ITEM,
                    (capK || newBindKey != 0) ? T_GREEN : T_GREY,
                    newBindKey != 0 && !capK,
                    () -> { capturingNewBindKey = true; sound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f); clearAndInit(); });

            int fieldX = windowX + rpx + 110;
            int fieldY = windowY + ay;
            cmdField = new TextFieldWidget(textRenderer, fieldX, fieldY, rpw - 142, 16, Text.empty());
            cmdField.setPlaceholder(Text.literal("/command…"));
            cmdField.setText(pendingCmd);
            cmdField.setChangedListener(t -> pendingCmd = t);
            addDrawableChild(cmdField);
            if (newBindKey != 0) setFocused(cmdField);

            btn(rpx + rpw - 24, ay, 18, 16, "✓", C_ACTIVE, T_GREEN, true, () -> {
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
            btn(rpx + 6, ay, 96, 16, t("farmhelperhypixel.btn.addbind"), C_ITEM, T_GREY, false,
                () -> { addingBind = true; sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.2f); clearAndInit(); });
        }
    }

    private void buildInversionBtns(int rpx, int cy, int rpw) {
        List<FarmHelperConfig.InversionBind> invs = config.inversionBinds;
        for (int i = 0; i < invs.size(); i++) {
            final int idx = i;
            btn(rpx + rpw - 24, cy + 12 + i * 26, 18, 16, "✕", C_ITEM, T_RED, false,
                () -> { config.inversionBinds.remove(idx); config.save();
                        FarmHelperHypixelClient.releaseInversions();
                        sound(SoundEvents.BLOCK_LEVER_CLICK, 0.7f); clearAndInit(); });
        }
        int ay = cy + 12 + invs.size() * 26 + 8;
        if (addingInversion) {
            boolean capK1 = capturingInvKey1, capK2 = capturingInvKey2;
            String l1 = capK1 ? t("farmhelperhypixel.btn.presskey")
                              : (newInvKey1 == 0 ? t("farmhelperhypixel.btn.setkey1") : keyName(newInvKey1));
            String l2 = capK2 ? t("farmhelperhypixel.btn.presskey")
                              : (newInvKey2 == 0 ? t("farmhelperhypixel.btn.setkey2") : keyName(newInvKey2));

            btn(rpx + 6, ay, 100, 16, l1,
                    (capK1 || newInvKey1 != 0) ? C_ACTIVE : C_ITEM,
                    (capK1 || newInvKey1 != 0) ? T_GREEN : T_GREY,
                    newInvKey1 != 0 && !capK1,
                    () -> { capturingInvKey1 = true; capturingInvKey2 = false;
                            sound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f); clearAndInit(); });

            btn(rpx + 118, ay, 100, 16, l2,
                    (capK2 || newInvKey2 != 0) ? C_ACTIVE : C_ITEM,
                    (capK2 || newInvKey2 != 0) ? T_GREEN : T_GREY,
                    newInvKey2 != 0 && !capK2,
                    () -> { capturingInvKey2 = true; capturingInvKey1 = false;
                            sound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f); clearAndInit(); });

            btn(rpx + rpw - 24, ay, 18, 16, "✓", C_ACTIVE, T_GREEN, true, () -> {
                if (newInvKey1 != 0 && newInvKey2 != 0) {
                    config.inversionBinds.add(new FarmHelperConfig.InversionBind(newInvKey1, newInvKey2));
                    config.save();
                    sound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f);
                    addingInversion = capturingInvKey1 = capturingInvKey2 = false;
                    newInvKey1 = newInvKey2 = 0;
                    clearAndInit();
                }
            });
        } else {
            btn(rpx + 6, ay, 110, 16, t("farmhelperhypixel.btn.addinversion"), C_ITEM, T_GREY, false,
                () -> { addingInversion = true; sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.2f); clearAndInit(); });
        }
    }

    private void buildFarmModeBtns(int rpx, int cy, int rpw) {
        boolean fm = config.farmModeEnabled;
        btn(rpx + 10, cy + 18, rpw - 20, 28,
                fm ? t("farmhelperhypixel.farmmode.on") : t("farmhelperhypixel.farmmode.off"),
                fm ? C_ACTIVE : C_ITEM, fm ? T_GREEN : T_RED, fm,
                () -> { config.farmModeEnabled = !config.farmModeEnabled; config.save();
                        sound(config.farmModeEnabled ? SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP : SoundEvents.BLOCK_LEVER_CLICK,
                              config.farmModeEnabled ? 1.6f : 0.8f); clearAndInit(); });
    }

    private void btn(int rx, int ry, int w, int h, String label, int bg, int fg, boolean ab, Runnable action) {
        btns.add(new Btn(rx, ry, w, h, label, bg, fg, ab, action));
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int wx = windowX, wy = windowY;

        // Window panels
        ctx.fill(wx, wy, wx + WIN_W, wy + WIN_H, C_BG);
        ctx.fill(wx, wy + TOP_H, wx + LEFT_W, wy + WIN_H - BOT_H, C_LEFT);
        ctx.fill(wx, wy, wx + WIN_W, wy + TOP_H, C_HEADER);
        ctx.fill(wx, wy + WIN_H - BOT_H, wx + WIN_W, wy + WIN_H, C_HEADER);

        // Borders
        border(ctx, wx, wy, WIN_W, WIN_H, C_BORDER);
        ctx.fill(wx + 1, wy + TOP_H, wx + WIN_W - 1, wy + TOP_H + 1, C_SEP);
        ctx.fill(wx + 1, wy + WIN_H - BOT_H, wx + WIN_W - 1, wy + WIN_H - BOT_H + 1, C_SEP);
        ctx.fill(wx + LEFT_W, wy + TOP_H + 1, wx + LEFT_W + 1, wy + WIN_H - BOT_H - 1, C_SEP);

        // Sections
        renderHeader(ctx, wx, wy, mx, my);
        renderLeft(ctx, wx, wy);
        int rpx = wx + LEFT_W + 1, rpw = WIN_W - LEFT_W - 2, cy = wy + TOP_H;
        switch (section) {
            case 0 -> renderControlsLabels(ctx, rpx, cy, rpw);
            case 1 -> renderBindsLabels(ctx, rpx, cy, rpw);
            case 2 -> renderInversionLabels(ctx, rpx, cy, rpw);
            case 3 -> renderFarmModeLabels(ctx, rpx, cy, rpw);
        }
        renderFooter(ctx, wx, wy, mx, my);

        // Custom buttons
        for (Btn b : btns) renderBtn(ctx, b, mx, my);

        // Real widgets (TextFieldWidget etc.)
        super.render(ctx, mx, my, delta);
    }

    private void renderHeader(DrawContext ctx, int wx, int wy, int mx, int my) {
        ctx.drawText(textRenderer, t("farmhelperhypixel.screen.title"), wx + 10, wy + 6, T_DIM, false);

        int n = SEC_KEYS.length, dotR = 4, gap = 28;
        int totalW = n * (dotR * 2) + (n - 1) * gap;
        int dx0 = wx + (WIN_W - totalW) / 2;
        int dotCY = wy + TOP_H / 2 - 2;

        for (int i = 0; i < n; i++) {
            int cx = dx0 + i * (dotR * 2 + gap) + dotR;
            if (i < n - 1) {
                int lc = i < section ? C_ACTIVE_B : C_BORDER;
                ctx.fill(cx + dotR + 2, dotCY - 1, cx + dotR + gap - 1, dotCY + 2, lc);
            }
            boolean active = i == section, done = i < section;
            circle(ctx, cx, dotCY, dotR, active ? T_GREEN : (done ? C_ACTIVE_B : C_BORDER));
            String num = String.valueOf(i + 1);
            ctx.drawText(textRenderer, num, cx - textRenderer.getWidth(num) / 2, dotCY - 3,
                    active ? 0xFF002010 : (done ? 0xFF002010 : C_BG), false);
            String sn = t(SEC_KEYS[i]);
            ctx.drawText(textRenderer, sn, cx - textRenderer.getWidth(sn) / 2, dotCY + dotR + 5,
                    active ? T_GREEN : (done ? T_GREY : T_DIM), false);
        }
    }

    private void renderLeft(DrawContext ctx, int wx, int wy) {
        int x = wx + 12, y = wy + TOP_H + 16, maxW = LEFT_W - 22;
        ctx.drawText(textRenderer, t(SEC_KEYS[section]), x, y, T_WHITE, false);
        y += 12;
        ctx.fill(x, y, wx + LEFT_W - 10, y + 1, C_SEP);
        y += 6;
        for (String line : wrapText(t(DESC_KEYS[section]), maxW)) {
            ctx.drawText(textRenderer, line, x, y, T_GREY, false);
            y += 10;
        }
    }

    private void renderControlsLabels(DrawContext ctx, int rpx, int cy, int rpw) {
        String[] rowKeys = {
            "farmhelperhypixel.key.forward", "farmhelperhypixel.key.back",
            "farmhelperhypixel.key.left",    "farmhelperhypixel.key.right",
            "farmhelperhypixel.key.attack"
        };
        for (int i = 0; i < rowKeys.length; i++) {
            int ry = cy + 12 + i * 26;
            ctx.fill(rpx + 4, ry, rpx + rpw - 4, ry + 18, C_ITEM);
            ctx.drawText(textRenderer, t(rowKeys[i]), rpx + 12, ry + 5, T_MAIN, false);
        }
        int ty = cy + 12 + 5 * 26 + 10;
        ctx.drawText(textRenderer, t("farmhelperhypixel.key.autoattack"), rpx + 12, ty + 3, T_GREY, false);
        ctx.drawText(textRenderer, t("farmhelperhypixel.key.lowsens"),    rpx + 12, ty + 3 + 22, T_GREY, false);
    }

    private void renderBindsLabels(DrawContext ctx, int rpx, int cy, int rpw) {
        List<FarmHelperConfig.CommandBind> binds = config.commandBinds;
        if (binds.isEmpty() && !addingBind)
            ctx.drawText(textRenderer, t("farmhelperhypixel.empty.binds"), rpx + 12, cy + 16, T_DIM, false);
        for (int i = 0; i < binds.size(); i++) {
            FarmHelperConfig.CommandBind b = binds.get(i);
            int ry = cy + 12 + i * 26;
            ctx.fill(rpx + 4, ry, rpx + rpw - 4, ry + 18, C_ITEM);
            String k = "[ " + keyName(b.key) + " ]";
            ctx.drawText(textRenderer, k, rpx + 12, ry + 5, T_GREEN, false);
            ctx.drawText(textRenderer, "→  /" + b.command,
                    rpx + 12 + textRenderer.getWidth(k) + 4, ry + 5, T_MAIN, false);
        }
    }

    private void renderInversionLabels(DrawContext ctx, int rpx, int cy, int rpw) {
        List<FarmHelperConfig.InversionBind> invs = config.inversionBinds;
        if (invs.isEmpty() && !addingInversion)
            ctx.drawText(textRenderer, t("farmhelperhypixel.empty.inversions"), rpx + 12, cy + 16, T_DIM, false);
        for (int i = 0; i < invs.size(); i++) {
            FarmHelperConfig.InversionBind inv = invs.get(i);
            int ry = cy + 12 + i * 26;
            ctx.fill(rpx + 4, ry, rpx + rpw - 4, ry + 18, C_ITEM);
            ctx.drawText(textRenderer, keyName(inv.key1), rpx + 12, ry + 5, T_GREEN, false);
            int w1 = textRenderer.getWidth(keyName(inv.key1));
            ctx.drawText(textRenderer, " ↔ ", rpx + 12 + w1, ry + 5, T_DIM, false);
            ctx.drawText(textRenderer, keyName(inv.key2),
                    rpx + 12 + w1 + textRenderer.getWidth(" ↔ "), ry + 5, T_GREEN, false);
        }
        if (addingInversion) {
            int ay = cy + 12 + invs.size() * 26 + 8;
            // ↔ separator between the two key buttons
            ctx.drawText(textRenderer, "↔", rpx + 110, ay + 4, T_DIM, false);
        }
    }

    private void renderFarmModeLabels(DrawContext ctx, int rpx, int cy, int rpw) {
        int sy = cy + 58;
        ctx.drawText(textRenderer, t("farmhelperhypixel.status.bindings"), rpx + 14, sy, T_DIM, false);
        sy += 12;
        boolean fm = config.farmModeEnabled;
        String[][] rows = {
            {t("farmhelperhypixel.key.forward"), keyName(config.forwardKey)},
            {t("farmhelperhypixel.key.back"),    keyName(config.backKey)},
            {t("farmhelperhypixel.key.left"),    keyName(config.leftKey)},
            {t("farmhelperhypixel.key.right"),   keyName(config.rightKey)},
            {t("farmhelperhypixel.key.attack"),  keyName(config.attackKey)},
            {t("farmhelperhypixel.key.autoattack"), config.autoAttack        ? t("farmhelperhypixel.btn.on") : t("farmhelperhypixel.btn.off")},
            {t("farmhelperhypixel.key.lowsens"),    config.reduceSensitivity ? t("farmhelperhypixel.btn.on") : t("farmhelperhypixel.btn.off")},
        };
        for (String[] row : rows) {
            ctx.drawText(textRenderer, row[0], rpx + 14, sy, T_DIM, false);
            boolean toggled = row[1].equals(t("farmhelperhypixel.btn.on")) || row[1].equals(t("farmhelperhypixel.btn.off"));
            boolean isOn = row[1].equals(t("farmhelperhypixel.btn.on"));
            int vc = toggled ? (isOn ? T_GREEN : T_RED) : (fm ? T_MAIN : T_DIM);
            ctx.drawText(textRenderer, row[1], rpx + 110, sy, vc, false);
            sy += 10;
        }
        if (!config.commandBinds.isEmpty())
            ctx.drawText(textRenderer, config.commandBinds.size() + " " + t("farmhelperhypixel.status.commandbinds"),
                    rpx + 14, sy + 2, T_DIM, false);
    }

    private void renderFooter(DrawContext ctx, int wx, int wy, int mx, int my) {
        boolean fm = config.farmModeEnabled;
        String s = fm ? t("farmhelperhypixel.status.active") : t("farmhelperhypixel.status.off");
        ctx.drawText(textRenderer, s,
                wx + (WIN_W - textRenderer.getWidth(s)) / 2,
                wy + WIN_H - BOT_H + 12,
                fm ? T_GREEN : T_DIM, false);
    }

    private void renderBtn(DrawContext ctx, Btn b, int mx, int my) {
        int ax = windowX + b.rx, ay = windowY + b.ry;
        boolean hov = mx >= ax && mx <= ax + b.w && my >= ay && my <= ay + b.h;
        int bg = hov ? brighten(b.bg, 22) : b.bg;
        ctx.fill(ax, ay, ax + b.w, ay + b.h, bg);
        int bc = b.ab ? C_ACTIVE_B : C_BORDER;
        border(ctx, ax, ay, b.w, b.h, bc);
        if (hov && !b.ab) ctx.fill(ax + 1, ay + 1, ax + b.w - 1, ay + 2, 0x14FFFFFF);
        int tx = ax + (b.w - textRenderer.getWidth(b.label)) / 2;
        int ty = ay + (b.h - 7) / 2;
        ctx.drawText(textRenderer, b.label, tx, ty, hov ? brightenText(b.fg) : b.fg, false);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int mx = (int) click.x(), my = (int) click.y();

        // Progress dot clicks
        int n = SEC_KEYS.length, dotR = 4, gap = 28;
        int totalW = n * (dotR * 2) + (n - 1) * gap;
        int dx0 = windowX + (WIN_W - totalW) / 2;
        int dotCY = windowY + TOP_H / 2 - 2;
        for (int i = 0; i < n; i++) {
            int cx = dx0 + i * (dotR * 2 + gap) + dotR;
            if (Math.abs(mx - cx) <= 10 && Math.abs(my - dotCY) <= 10) {
                if (i != section) { navigateTo(i); sound(SoundEvents.ENTITY_ITEM_PICKUP, 1.8f); }
                return true;
            }
        }

        // Custom buttons (relative → absolute)
        for (Btn b : btns) {
            int ax = windowX + b.rx, ay = windowY + b.ry;
            if (mx >= ax && mx <= ax + b.w && my >= ay && my <= ay + b.h) {
                b.action().run();
                return true;
            }
        }

        // Start drag if clicking header
        if (mx >= windowX && mx <= windowX + WIN_W && my >= windowY && my <= windowY + TOP_H) {
            dragging = true;
            dragOffX = mx - windowX;
            dragOffY = my - windowY;
            return true;
        }

        // Consume clicks inside window to prevent game interaction
        if (mx >= windowX && mx <= windowX + WIN_W && my >= windowY && my <= windowY + WIN_H)
            return true;

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (dragging && click.button() == 0) {
            windowX = Math.max(0, Math.min(width  - WIN_W, (int) click.x() - dragOffX));
            windowY = Math.max(0, Math.min(height - WIN_H, (int) click.y() - dragOffY));
            if (cmdField != null) {
                int rpx = LEFT_W + 1;
                int bindCount = config.commandBinds.size();
                cmdField.setX(windowX + rpx + 110);
                cmdField.setY(windowY + TOP_H + 12 + bindCount * 26 + 8);
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging) {
            dragging = false;
            clearAndInit();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int k = input.key();

        if (capturingFor != null) {
            if (k != GLFW.GLFW_KEY_ESCAPE) {
                switch (capturingFor) {
                    case "forward" -> config.forwardKey = k;
                    case "back"    -> config.backKey    = k;
                    case "left"    -> config.leftKey    = k;
                    case "right"   -> config.rightKey   = k;
                    case "attack"  -> config.attackKey  = k;
                }
                config.save();
                sound(SoundEvents.ENTITY_ITEM_PICKUP, 1.9f);
            }
            capturingFor = null;
            clearAndInit();
            return true;
        }

        if (capturingNewBindKey) {
            if (k != GLFW.GLFW_KEY_ESCAPE) {
                newBindKey = k;
                sound(SoundEvents.ENTITY_ITEM_PICKUP, 1.9f);
            }
            capturingNewBindKey = false;
            clearAndInit();
            return true;
        }

        if (capturingInvKey1) {
            if (k != GLFW.GLFW_KEY_ESCAPE) {
                newInvKey1 = k;
                capturingInvKey1 = false;
                if (newInvKey2 == 0) capturingInvKey2 = true;
                sound(SoundEvents.ENTITY_ITEM_PICKUP, 1.9f);
            } else {
                capturingInvKey1 = false;
            }
            clearAndInit();
            return true;
        }

        if (capturingInvKey2) {
            if (k != GLFW.GLFW_KEY_ESCAPE) {
                newInvKey2 = k;
                sound(SoundEvents.ENTITY_ITEM_PICKUP, 1.9f);
            }
            capturingInvKey2 = false;
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
        if (section < SEC_KEYS.length - 1) {
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
        lastSection = idx;
        capturingFor = null;
        addingBind = capturingNewBindKey = false;
        addingInversion = capturingInvKey1 = capturingInvKey2 = false;
        newBindKey = 0; pendingCmd = "";
        newInvKey1 = 0; newInvKey2 = 0;
        clearAndInit();
    }

    // ── Drawing utilities ─────────────────────────────────────────────────────

    private void border(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x,     y,     x + w, y + 1, c);
        ctx.fill(x,     y+h-1, x + w, y + h, c);
        ctx.fill(x,     y,     x + 1, y + h, c);
        ctx.fill(x+w-1, y,     x + w, y + h, c);
    }

    private void circle(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt((double) r * r - (double) dy * dy);
            ctx.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private static int brighten(int argb, int amt) {
        return 0xFF000000
            | (Math.min(255, ((argb >> 16) & 0xFF) + amt) << 16)
            | (Math.min(255, ((argb >> 8)  & 0xFF) + amt) << 8)
            |  Math.min(255, (argb & 0xFF) + amt);
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

    private static String t(String key) {
        return I18n.translate(key);
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
