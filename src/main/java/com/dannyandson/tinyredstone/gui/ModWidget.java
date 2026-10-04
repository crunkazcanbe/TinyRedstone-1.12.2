package com.dannyandson.tinyredstone.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12.2 port of the 1.16 {@code ModWidget} (a {@code Widget} subclass).
 * 1.12.2 has no {@code Widget} class, so this extends {@link Gui} (which supplies
 * {@code drawRect}/font helpers) and implements its own bounds + click handling so
 * it can still be added to the Screen's button list the way the original code did.
 */
public class ModWidget extends Gui {

    public enum HAlignment {
        LEFT, CENTER, RIGHT
    }
    public enum VAlignment {
        TOP, MIDDLE, BOTTOM
    }

    public int x;
    public int y;
    public int width;
    public int height;
    public boolean visible = true;

    private ITextComponent message;
    private HAlignment halignment = HAlignment.LEFT;
    private VAlignment valignment = VAlignment.TOP;
    private float scale = 1.0f;
    private int color;
    private int bgcolor = -1;
    private int textWidth;
    private int textHeight;
    private ITextComponent toolTipTextComponent;
    private ModWidget.IPressable pressedAction = null;

    /** When >=0, a small beveled icon chip of this ARGB color is drawn at the widget's left. */
    private int iconColor = -1;
    /** When true a bgcolor fill is drawn as a beveled swatch (raised tile) instead of a flat rect. */
    private boolean beveled = false;

    public ModWidget(int x, int y, int width, int height, ITextComponent title, int textColor, int bgColor)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.message = title;
        this.color = textColor;
        this.bgcolor = bgColor;
        if (title.getUnformattedText().length() > 0) {
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            this.textWidth = fr.getStringWidth(title.getFormattedText());
            this.textHeight = fr.FONT_HEIGHT;
        }
    }

    public ModWidget(int x, int y, int width, int height, ITextComponent title, int textColor)
    {
        this(x, y, width, height, title, textColor, -1);
    }

    public ModWidget(int x, int y, int width, int height, ITextComponent title)
    {
        this(x, y, width, height, title, 0xFFFFFFFF, -1);
    }

    public ModWidget(int x, int y, int width, int height, int bgColor)
    {
        this(x, y, width, height, new TextComponentString(""), 0xFFFFFFFF, bgColor);
    }

    public ModWidget(int x, int y, int width, int height, int bgColor, ModWidget.IPressable pressedAction)
    {
        this(x, y, width, height, new TextComponentString(""), 0xFFFFFFFF, bgColor);
        this.pressedAction = pressedAction;
    }

    public ITextComponent getMessage() {
        return this.message;
    }

    public ModWidget setTextHAlignment(HAlignment alignment) {
        this.halignment = alignment;
        return this;
    }
    public ModWidget setTextVAlignment(VAlignment alignment) {
        this.valignment = alignment;
        return this;
    }

    public ModWidget setToolTip(ITextComponent textComponent)
    {
        this.toolTipTextComponent = textComponent;
        return this;
    }

    /** Draw a small beveled icon chip (ARGB) at the left of this widget. */
    public ModWidget setIcon(int argbColor) {
        this.iconColor = argbColor;
        return this;
    }

    /** Render this widget's bgcolor fill as a raised beveled tile (used for color swatches). */
    public ModWidget setBeveled(boolean b) {
        this.beveled = b;
        return this;
    }

    /**
     * Mirrors the 1.16 {@code clicked(double,double)} bounds check + press dispatch.
     * The owning Screen forwards mouse clicks here (see the GUI mousePressed overrides).
     */
    public boolean clicked(double mouseX, double mouseY) {
        if (pressedAction == null ||
                mouseX < this.x || mouseX > this.x + this.width ||
                mouseY < this.y || mouseY > this.y + this.height
        )
            return false;

        pressedAction.onPress(this);
        return true;
    }

    /**
     * 1.12.2 render entry point. In 1.16 this was {@code renderButton(MatrixStack,...)};
     * here the owning Screen calls it directly from its draw pass.
     */
    public void renderButton(int mouseX, int mouseY, float partialTick) {
        if (visible) {
            int drawX, drawY;
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;

            // --- background fill first (so text/icon draw on top) ---
            if (bgcolor != -1) {
                if (beveled) {
                    int hi = 0x55FFFFFF | (bgcolor & 0x00FFFFFF);
                    int lo = 0x66000000;
                    TinyGuiStyle.bevelBox(x, y, width, height, bgcolor, hi, lo, true);
                } else {
                    drawRect(x, y, x + width, y + height, bgcolor);
                }
            }

            // --- optional icon chip on the left ---
            int leftPad = 0;
            if (iconColor != -1) {
                int chip = Math.min(10, height - 4);
                if (chip < 4) chip = 4;
                int chipX = x + 2;
                int chipY = y + (height - chip) / 2;
                int hi = 0x66FFFFFF | (iconColor & 0x00FFFFFF);
                TinyGuiStyle.bevelBox(chipX, chipY, chip, chip, iconColor, hi, 0x77000000, true);
                leftPad = chip + 4;
            }

            switch (halignment) {
                case LEFT:
                default:
                    drawX = x + leftPad;
                    break;
                case CENTER:
                    drawX = x + leftPad + (int)((width - leftPad - textWidth) / 2 * scale);
                    break;
                case RIGHT:
                    drawX = x + (int)((width - textWidth) * scale);
                    break;
            }
            switch (valignment) {
                case TOP:
                default:
                    drawY = y;
                    break;
                case MIDDLE:
                    drawY = y + (int)((height - textHeight) / 2 * scale);
                    break;
                case BOTTOM:
                    drawY = y + (int)((height - textHeight) * scale);
                    break;
            }

            String text = getMessage().getFormattedText();
            if (text.length() > 0) {
                if (scale != 1.0f) {
                    net.minecraft.client.renderer.GlStateManager.pushMatrix();
                    net.minecraft.client.renderer.GlStateManager.scale(scale, scale, scale);
                    net.minecraft.client.renderer.GlStateManager.translate(drawX, y, 0);
                    fr.drawStringWithShadow(text, drawX, drawY, this.color);
                    net.minecraft.client.renderer.GlStateManager.popMatrix();
                } else {
                    fr.drawStringWithShadow(text, drawX, drawY, this.color);
                }
            }

            if (this.toolTipTextComponent != null && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height)
                this.renderHoverToolTip(mouseX, mouseY);
        }
    }

    public void renderHoverToolTip(int mouseX, int mouseY) {
        if (this.toolTipTextComponent != null) {
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            int width = fr.getStringWidth(this.toolTipTextComponent.getFormattedText());
            int height = fr.FONT_HEIGHT;

            drawRect(mouseX, mouseY + 10, mouseX + width + 4, mouseY + 10 + height + 4, 0xCC000000);
            drawRect(mouseX + 1, mouseY + 11, mouseX + width + 3, mouseY + 10 + height + 3, 0x66EEEEEE);
            fr.drawString(this.toolTipTextComponent.getFormattedText(), mouseX + 3, mouseY + 13, 0xFFFEFEFE);
        }
    }

    @SideOnly(Side.CLIENT)
    public interface IPressable {
        void onPress(ModWidget modWidget);
    }

}
