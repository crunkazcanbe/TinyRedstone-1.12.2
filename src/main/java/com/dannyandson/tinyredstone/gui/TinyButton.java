package com.dannyandson.tinyredstone.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

/**
 * Beveled chip-style button used across all Tiny Redstone cell menus, replacing the vanilla
 * GuiButton texture. Drawn programmatically via {@link TinyGuiStyle}. Keeps full GuiButton
 * behaviour (id, action dispatch, enabled/visible) — only the look changes.
 *
 * Optional left "icon chip": a small beveled colored tile drawn at the left of the button,
 * with the label vertically centered to its right (matches the approved mockup). A button with
 * no icon chip simply centers its label.
 *
 * A {@code green} flag renders the button as a green highlighted bar (used for the "selected"
 * style accents in the mockup, e.g. the Lock row).
 */
public class TinyButton extends GuiButton {

    private boolean hasIcon = false;
    private int iconColor = 0xFFB0B0B0;
    private boolean green = false;
    private boolean centerLabel = false;

    public TinyButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    public TinyButton setIcon(int argbColor) {
        this.hasIcon = true;
        this.iconColor = argbColor;
        return this;
    }

    public TinyButton setGreen(boolean g) {
        this.green = g;
        return this;
    }

    public TinyButton setCenterLabel(boolean c) {
        this.centerLabel = c;
        return this;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        FontRenderer fr = mc.fontRenderer;
        this.hovered = mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height;

        int base, hi, lo;
        if (green) {
            base = TinyGuiStyle.DONE_GREEN;
            hi = TinyGuiStyle.DONE_GREEN_TOP;
            lo = TinyGuiStyle.DONE_GREEN_DARK;
        } else {
            base = this.hovered ? TinyGuiStyle.BTN_BASE_HOVER : TinyGuiStyle.BTN_BASE;
            hi = TinyGuiStyle.BTN_BEVEL_HI;
            lo = TinyGuiStyle.BTN_BEVEL_LO;
        }
        // pressed-look bevel inverts when the mouse is held over an enabled button
        boolean raised = !(this.hovered && this.enabled && org.lwjgl.input.Mouse.isButtonDown(0));
        TinyGuiStyle.bevelBox(this.x, this.y, this.width, this.height, base, hi, lo, raised);

        int textColor = this.enabled ? (green ? 0xFFFFFFFF : TinyGuiStyle.BTN_TEXT)
                                     : TinyGuiStyle.BTN_TEXT_DISABLED;

        int textLeft;
        if (hasIcon) {
            int chip = 8;
            int chipX = this.x + 4;
            int chipY = this.y + (this.height - chip) / 2;
            // dark recessed seat, then the colored chip
            TinyGuiStyle.bevelBox(chipX, chipY, chip, chip, iconColor, 0x33FFFFFF | (iconColor & 0x00FFFFFF), 0x55000000, true);
            textLeft = chipX + chip + 4;
        } else {
            textLeft = this.x + 4;
        }

        String label = this.displayString;
        int ty = this.y + (this.height - fr.FONT_HEIGHT) / 2 + 1;
        if (centerLabel || !hasIcon) {
            int tx = this.x + (this.width - fr.getStringWidth(label)) / 2;
            // if there is an icon, keep the label clear of it
            if (hasIcon && tx < textLeft) tx = textLeft;
            fr.drawStringWithShadow(label, tx, ty, textColor);
        } else {
            fr.drawStringWithShadow(label, textLeft, ty, textColor);
        }
    }
}
