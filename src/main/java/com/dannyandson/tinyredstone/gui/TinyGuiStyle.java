package com.dannyandson.tinyredstone.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

/**
 * Shared programmatic look-and-feel for all Tiny Redstone cell config menus.
 *
 * Everything is drawn with {@link Gui#drawRect} fills (no external texture sheet) so the
 * menus get a consistent "dark IC-chip panel" style: a beveled dark body with corner screws,
 * a red title bar, beveled buttons with optional icon chips, color swatches and a green DONE bar.
 *
 * Pure 1.12.2 / Java 8 (static helpers, no Java 9+ APIs).
 */
public final class TinyGuiStyle extends Gui {

    private TinyGuiStyle() {}

    private static final TinyGuiStyle DRAW = new TinyGuiStyle();

    // ---- palette (ARGB) ----
    public static final int PANEL_BASE   = 0xFF32343C; // ~(50,52,60)
    public static final int BEVEL_LIGHT  = 0xFF4E525C; // ~(78,82,92) top/left
    public static final int BEVEL_DARK   = 0xFF1C1E24; // ~(28,30,36) bottom/right
    public static final int SCREW        = 0xFF4A4E56; // ~(74,78,86)

    public static final int TITLE_RED      = 0xFFC82020; // ~(200,32,32)
    public static final int TITLE_RED_TOP  = 0xFFF44848; // ~(244,72,72)
    public static final int TITLE_RED_DARK = 0xFF8E1616;

    public static final int DONE_GREEN      = 0xFF50BE58; // ~(80,190,88)
    public static final int DONE_GREEN_TOP  = 0xFF78E082;
    public static final int DONE_GREEN_DARK = 0xFF2F8A3A;

    public static final int BTN_BASE      = 0xFF3C404A;
    public static final int BTN_BASE_HOVER= 0xFF4A4F5A;
    public static final int BTN_BEVEL_HI  = 0xFF5A5F6B;
    public static final int BTN_BEVEL_LO  = 0xFF22242A;
    public static final int BTN_TEXT      = 0xFFE6E6EA;
    public static final int BTN_TEXT_DISABLED = 0xFF8A8C92;

    public static final int TITLE_BAR_H = 16;
    public static final int DONE_BAR_H  = 18;

    // ---- low level ----
    public static void rect(int x1, int y1, int x2, int y2, int color) {
        DRAW.drawRectPublic(x1, y1, x2, y2, color);
    }

    private void drawRectPublic(int x1, int y1, int x2, int y2, int color) {
        drawRect(x1, y1, x2, y2, color);
    }

    /** A filled rectangle with a 1px raised (or sunken) bevel. */
    public static void bevelBox(int x, int y, int w, int h, int base, int hi, int lo, boolean raised) {
        int topLeft = raised ? hi : lo;
        int botRight = raised ? lo : hi;
        rect(x, y, x + w, y + h, base);
        // top + left edges
        rect(x, y, x + w, y + 1, topLeft);
        rect(x, y, x + 1, y + h, topLeft);
        // bottom + right edges
        rect(x, y + h - 1, x + w, y + h, botRight);
        rect(x + w - 1, y, x + w, y + h, botRight);
    }

    /**
     * Draws the whole chip panel (body + bevel + corner screws + red title bar + close x)
     * for a menu occupying (x,y,w,h). Title text is drawn in white in the bar.
     */
    public static void drawPanel(int x, int y, int w, int h, String title) {
        // outer dark drop shadow
        rect(x - 2, y - 2, x + w + 2, y + h + 2, 0x66000000);

        // main beveled body
        bevelBox(x, y, w, h, PANEL_BASE, BEVEL_LIGHT, BEVEL_DARK, true);

        // corner screws
        drawScrew(x + 3, y + 3);
        drawScrew(x + w - 7, y + 3);
        drawScrew(x + 3, y + h - 7);
        drawScrew(x + w - 7, y + h - 7);

        // red title bar
        int barX = x + 2, barY = y + 2, barW = w - 4;
        rect(barX, barY, barX + barW, barY + TITLE_BAR_H, TITLE_RED);
        rect(barX, barY, barX + barW, barY + 1, TITLE_RED_TOP);       // bright top edge
        rect(barX, barY + TITLE_BAR_H - 1, barX + barW, barY + TITLE_BAR_H, TITLE_RED_DARK);

        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        if (title != null && title.length() > 0) {
            fr.drawStringWithShadow(title, barX + 18, barY + (TITLE_BAR_H - fr.FONT_HEIGHT) / 2 + 1, 0xFFFFFFFF);
        }
        // small icon chip on the left of the title bar
        int chipX = barX + 3, chipY = barY + 3;
        bevelBox(chipX, chipY, 10, 10, 0xFF202228, 0xFF3A3D45, 0xFF101216, true);
        // close x tile, top right
        int xs = barX + barW - 13, ys = barY + 3;
        bevelBox(xs, ys, 11, 11, 0xFF202228, 0xFF3A3D45, 0xFF101216, true);
        fr.drawStringWithShadow("x", xs + 3, ys + 2, 0xFFFFFFFF);
    }

    private static void drawScrew(int x, int y) {
        rect(x, y, x + 4, y + 4, SCREW);
        rect(x, y, x + 4, y + 1, BEVEL_LIGHT);
        rect(x, y, x + 1, y + 4, BEVEL_LIGHT);
        rect(x + 3, y, x + 4, y + 4, BEVEL_DARK);
        rect(x, y + 3, x + 4, y + 4, BEVEL_DARK);
        rect(x + 1, y + 1, x + 3, y + 3, 0xFF26282E);
    }

    /** The green DONE bar across the bottom of a panel. */
    public static void drawDoneBar(int x, int y, int w) {
        bevelBox(x, y, w, DONE_BAR_H, DONE_GREEN, DONE_GREEN_TOP, DONE_GREEN_DARK, true);
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        String label = "DONE";
        int tx = x + (w - fr.getStringWidth(label)) / 2;
        int ty = y + (DONE_BAR_H - fr.FONT_HEIGHT) / 2 + 1;
        fr.drawStringWithShadow(label, tx, ty, 0xFFFFFFFF);
    }

    /** Returns true if (mx,my) is over the close-x tile for a panel at (x,y,w). */
    public static boolean isOverClose(int x, int y, int w, int mx, int my) {
        int barX = x + 2, barY = y + 2, barW = w - 4;
        int xs = barX + barW - 13, ys = barY + 3;
        return mx >= xs && mx <= xs + 11 && my >= ys && my <= ys + 11;
    }
}
