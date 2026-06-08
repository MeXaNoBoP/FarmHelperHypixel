package ru.mexanobop.farmhelperhypixel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;

public class FarmHelperHud {

    static final int W     = 180;
    static final int MAX_H = 70;

    private static final int BG     = 0xCC000000;
    private static final int BD     = 0xFF1D6247;
    private static final int SEP    = 0xFF133D2C;
    private static final int HDR_BG = 0xFF0B1A10;

    private static final int T_GREEN = 0xFF5BE38B;
    private static final int T_RED   = 0xFFE35B5B;
    private static final int T_WHITE = 0xFFFFFFFF;
    private static final int T_GREY  = 0xFF888888;
    private static final int T_DIM   = 0xFF444444;

    static long farmStartTime = 0;

    private static final Map<String, ItemStack> CROP_ICONS;
    static {
        CROP_ICONS = new HashMap<>();
        CROP_ICONS.put("wheat",       new ItemStack(Items.WHEAT));
        CROP_ICONS.put("carrot",      new ItemStack(Items.CARROT));
        CROP_ICONS.put("potato",      new ItemStack(Items.POTATO));
        CROP_ICONS.put("nether_wart", new ItemStack(Items.NETHER_WART));
        CROP_ICONS.put("sugar_cane",  new ItemStack(Items.SUGAR_CANE));
        CROP_ICONS.put("blue_orchid", new ItemStack(Items.BLUE_ORCHID));
        CROP_ICONS.put("cocoa",       new ItemStack(Items.COCOA_BEANS));
        CROP_ICONS.put("cactus",      new ItemStack(Items.CACTUS));
        CROP_ICONS.put("melon",       new ItemStack(Items.MELON));
        CROP_ICONS.put("pumpkin",     new ItemStack(Items.CARVED_PUMPKIN));
        CROP_ICONS.put("sunflower",   new ItemStack(Items.SUNFLOWER));
    }

    static void render(DrawContext ctx, FarmHelperConfig cfg, MinecraftClient client) {
        if (!cfg.hudEnabled) return;

        TextRenderer tr = client.textRenderer;
        boolean fm = cfg.farmModeEnabled;
        int x = cfg.hudX, y = cfg.hudY;
        long now = System.currentTimeMillis();

        // Active crop
        String cropKey = FarmHelperHypixelClient.activeCropKey;
        FarmHelperConfig.CropStats stats = cropKey != null ? cfg.cropStats.get(cropKey) : null;
        boolean hasCrop = stats != null && stats.lastBreakTime != 0;

        // Dynamic height
        int cropSecH = hasCrop ? 22 : 12;
        int statsSecH = hasCrop ? 22 : 0;
        int H = 2 + 14 + 1 + cropSecH + (hasCrop ? 1 + statsSecH : 0) + 2;

        // Background
        ctx.fill(x, y, x + W, y + H, BG);
        // Header background
        ctx.fill(x + 2, y + 2, x + W - 2, y + 16, HDR_BG);
        // Thick border (2px on all sides)
        thickBorder(ctx, x, y, W, H, BD);

        // ── Header ─────────────────────────────────────────────────────────
        // Status dot 5×5
        int dotY = y + 2 + 4;
        ctx.fill(x + 5, dotY, x + 10, dotY + 5, fm ? T_GREEN : T_RED);
        // Title
        ctx.drawText(tr, "FarmHelper", x + 14, y + 2 + 4, T_WHITE, false);
        // Farm timer right-aligned
        if (fm && farmStartTime > 0) {
            String timer = formatTime(now - farmStartTime);
            ctx.drawText(tr, timer, x + W - 4 - tr.getWidth(timer), y + 2 + 4, T_GREY, false);
        }

        // Separator below header
        int ly = y + 16;
        ctx.fill(x + 2, ly, x + W - 2, ly + 1, SEP);
        ly += 1;

        // ── Crop section ───────────────────────────────────────────────────
        if (hasCrop) {
            // Icon 16×16
            ItemStack icon = CROP_ICONS.getOrDefault(cropKey, ItemStack.EMPTY);
            ctx.drawItem(icon, x + 4, ly + 3);
            // Crop name
            String cropName = t("farmhelperhypixel.crop." + cropKey);
            ctx.drawText(tr, cropName, x + 23, ly + 7, T_GREEN, false);
            ly += 22;

            // Separator
            ctx.fill(x + 2, ly, x + W - 2, ly + 1, SEP);
            ly += 1;

            // Session time
            long sIdle = stats.sessionLastBreakTime == 0
                    ? Long.MAX_VALUE : (now - stats.sessionLastBreakTime);
            long sessionMs = stats.sessionLastBreakTime == 0
                    ? 0 : stats.sessionActiveMs + (sIdle < 5000 ? sIdle : 0);

            // Total time
            long idle      = now - stats.lastBreakTime;
            long totalMs   = stats.activeMs + (idle < 5000 ? idle : 0);

            // Session row (bright — current session)
            statRow(ctx, tr, x, ly + 2,
                    t("farmhelperhypixel.hud.crop.session"),
                    stats.sessionCount, sessionMs, T_GREY, T_GREEN);
            ly += 11;

            // Total row (dim — lifetime)
            statRow(ctx, tr, x, ly + 2,
                    t("farmhelperhypixel.hud.crop.total"),
                    stats.count, totalMs, T_DIM, T_GREY);
        } else {
            // No active crop
            String noData = t("farmhelperhypixel.hud.crop.none");
            ctx.drawText(tr, noData,
                    x + (W - tr.getWidth(noData)) / 2, ly + 3, T_DIM, false);
        }
    }

    private static void statRow(DrawContext ctx, TextRenderer tr,
                                 int x, int y, String label,
                                 int count, long ms, int labelCol, int countCol) {
        ctx.drawText(tr, label, x + 5, y, labelCol, false);
        String cStr = "×" + fmtCount(count);
        ctx.drawText(tr, cStr, x + 5 + tr.getWidth(label) + 3, y, countCol, false);
        String tStr = formatTime(ms);
        ctx.drawText(tr, tStr, x + W - 5 - tr.getWidth(tStr), y, labelCol, false);
    }

    private static String fmtCount(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    private static String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long secs = ms / 1000;
        long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static void thickBorder(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x,     y,     x+w,   y+2,   c);  // top
        ctx.fill(x,     y+h-2, x+w,   y+h,   c);  // bottom
        ctx.fill(x,     y+2,   x+2,   y+h-2, c);  // left
        ctx.fill(x+w-2, y+2,   x+w,   y+h-2, c);  // right
    }

    private static String t(String key) { return I18n.translate(key); }
}
