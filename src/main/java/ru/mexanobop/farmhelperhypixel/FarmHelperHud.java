package ru.mexanobop.farmhelperhypixel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

public class FarmHelperHud {

    static final int W   = 130;
    static final int MAX_H = 82; // max possible height (5 lines)
    private static final int BG = 0xCC000000;   // semi-transparent black
    private static final int BD = 0xFF1D6247;   // green border
    private static final int T_GREEN  = 0xFF5BE38B;
    private static final int T_RED    = 0xFFE35B5B;
    private static final int T_WHITE  = 0xFFFFFFFF;
    private static final int T_GREY   = 0xFF888888;
    private static final int T_DIM    = 0xFF444444;

    // Farm mode start time (ms), updated by the client
    static long farmStartTime = 0;

    static void render(DrawContext ctx, FarmHelperConfig cfg, MinecraftClient client) {
        if (!cfg.hudEnabled) return;

        var tr = client.textRenderer;
        boolean fm = cfg.farmModeEnabled;

        // Build lines
        String statusLine = fm ? t("farmhelperhypixel.hud.status.on") : t("farmhelperhypixel.hud.status.off");
        String timeLine   = fm ? formatTime(System.currentTimeMillis() - farmStartTime) : "--:--:--";
        String sensLine   = cfg.reduceSensitivity ? t("farmhelperhypixel.hud.sens.low") : t("farmhelperhypixel.hud.sens.normal");
        String bindsLine  = cfg.commandBinds.isEmpty()
                ? t("farmhelperhypixel.hud.nobinds")
                : cfg.commandBinds.size() + " " + t("farmhelperhypixel.hud.binds");
        String invLine    = cfg.inversionBinds.isEmpty()
                ? ""
                : cfg.inversionBinds.size() + " " + t("farmhelperhypixel.hud.inversions");

        int lineH  = 10;
        int pad    = 4;
        int lines  = invLine.isEmpty() ? 4 : 5;
        int H      = pad + 12 + pad / 2 + lines * lineH + pad; // title + separator + lines + bottom pad

        int x = cfg.hudX, y = cfg.hudY;

        // Background
        ctx.fill(x, y, x + W, y + H, BG);

        // Border
        border(ctx, x, y, W, H, BD);

        // Title bar separator
        ctx.fill(x + 1, y + 14, x + W - 1, y + 15, 0xFF133D2C);

        // Title
        String title = "FarmHelper";
        ctx.drawText(tr, title, x + (W - tr.getWidth(title)) / 2, y + 4, T_WHITE, false);

        // Status indicator dot
        int dotX = x + 4, dotY = y + 5;
        ctx.fill(dotX, dotY, dotX + 4, dotY + 4, fm ? T_GREEN : T_RED);

        // Content lines
        int ly = y + 18;

        // Farm mode status
        ctx.drawText(tr, statusLine, x + pad, ly, fm ? T_GREEN : T_RED, false);
        ly += lineH;

        // Time
        ctx.drawText(tr, t("farmhelperhypixel.hud.time") + " " + timeLine, x + pad, ly, T_GREY, false);
        ly += lineH;

        // Sensitivity
        ctx.drawText(tr, sensLine, x + pad, ly, T_GREY, false);
        ly += lineH;

        // Command binds count
        ctx.drawText(tr, bindsLine, x + pad, ly, T_DIM, false);
        ly += lineH;

        // Inversion count (if any)
        if (!invLine.isEmpty()) {
            ctx.drawText(tr, invLine, x + pad, ly, T_DIM, false);
        }
    }

    private static String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long secs = ms / 1000;
        long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x,     y,     x + w, y + 1, c);
        ctx.fill(x,     y+h-1, x + w, y + h, c);
        ctx.fill(x,     y,     x + 1, y + h, c);
        ctx.fill(x+w-1, y,     x + w, y + h, c);
    }

    private static String t(String key) { return I18n.translate(key); }
}
