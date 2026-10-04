package com.dannyandson.tinyredstone.gui;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.IColorablePanelCell;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.network.ModNetworkHandler;
import com.dannyandson.tinyredstone.network.TinyBlockColorSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TinyBlockGUI extends GuiScreen {

    private static final int WIDTH = 170;
    private static final int HEIGHT = 100;

    private static final int BTN_CLOSE = 0;

    private final PanelTile panelTile;
    private final Integer cellIndex;
    private final IColorablePanelCell iColorablePanelCell;

    private final List<ModWidget> modWidgets = new ArrayList<ModWidget>();

    private final ResourceLocation GUI = new ResourceLocation(TinyRedstone.MODID, "textures/gui/transparent.png");

    protected TinyBlockGUI(PanelTile panelTile, Integer cellIndex, IColorablePanelCell iColorablePanelCell) {
        this.panelTile = panelTile;
        this.cellIndex = cellIndex;
        this.iColorablePanelCell = iColorablePanelCell;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.modWidgets.clear();

        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        int top = relY + TinyGuiStyle.TITLE_BAR_H + 4;

        // "Select a color" caption
        modWidgets.add(new ModWidget(relX + 6, top, WIDTH - 2, 12, new TextComponentTranslation("tinyredstone.gui.tinyblock.msg"))
                .setTextHAlignment(ModWidget.HAlignment.LEFT));

        // 1.16 DyeColor -> 1.12.2 EnumDyeColor (LIGHT_GRAY -> SILVER). See addColorSwatch PORT-TODO.
        int sw = top + 14;
        addColorSwatch(relX + 5,   sw, EnumDyeColor.WHITE);
        addColorSwatch(relX + 25,  sw, EnumDyeColor.BLACK);
        addColorSwatch(relX + 45,  sw, EnumDyeColor.RED);
        addColorSwatch(relX + 65,  sw, EnumDyeColor.GREEN);
        addColorSwatch(relX + 85,  sw, EnumDyeColor.BROWN);
        addColorSwatch(relX + 105, sw, EnumDyeColor.BLUE);
        addColorSwatch(relX + 125, sw, EnumDyeColor.PURPLE);
        addColorSwatch(relX + 145, sw, EnumDyeColor.CYAN);

        addColorSwatch(relX + 5,   sw + 22, EnumDyeColor.SILVER); // 1.12.2 name for LIGHT_GRAY
        addColorSwatch(relX + 25,  sw + 22, EnumDyeColor.GRAY);
        addColorSwatch(relX + 45,  sw + 22, EnumDyeColor.PINK);
        addColorSwatch(relX + 65,  sw + 22, EnumDyeColor.LIME);
        addColorSwatch(relX + 85,  sw + 22, EnumDyeColor.YELLOW);
        addColorSwatch(relX + 105, sw + 22, EnumDyeColor.LIGHT_BLUE);
        addColorSwatch(relX + 125, sw + 22, EnumDyeColor.MAGENTA);
        addColorSwatch(relX + 145, sw + 22, EnumDyeColor.ORANGE);

        // green DONE button along the bottom
        this.buttonList.add(((TinyButton) new TinyButton(BTN_CLOSE, relX + 8, relY + HEIGHT - 20, WIDTH - 16, 16,
                new TextComponentTranslation("tinyredstone.close").getFormattedText())).setGreen(true).setCenterLabel(true));
    }

    private void addColorSwatch(int x, int y, EnumDyeColor dyeColor) {
        // PORT-TODO: 1.16 used DyeColor.getTextColor() for the swatch bg and getColorValue() for the
        // applied color (slightly different values). 1.12.2 EnumDyeColor only exposes getColorValue(),
        // so both the swatch fill and the applied color use it (matches sibling code's getColorValue()).
        final int rgb = dyeColor.getColorValue();
        modWidgets.add(new ModWidget(x, y, 18, 18, rgb + 0xFF000000, widget -> setColor(rgb)).setBeveled(true));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_CLOSE) {
            close();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        if (TinyGuiStyle.isOverClose(relX, relY, WIDTH, mouseX, mouseY)) {
            close();
            return;
        }
        // Forward clicks to ModWidget swatches (they carry their own press actions).
        for (ModWidget widget : modWidgets) {
            if (widget.clicked(mouseX, mouseY)) {
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void close() {
        this.mc.displayGuiScreen(null);
    }

    private void setColor(int color) {
        this.iColorablePanelCell.setColor(color);
        ModNetworkHandler.sendToServer(new TinyBlockColorSync(panelTile.getPos(), cellIndex, color));
        this.close();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        TinyGuiStyle.drawPanel(relX, relY, WIDTH, HEIGHT,
                new TextComponentTranslation("tinyredstone.gui.tinyblock.title").getFormattedText());

        for (ModWidget widget : modWidgets) {
            widget.renderButton(mouseX, mouseY, partialTicks);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public static void open(PanelTile panelTile, Integer cellIndex, IColorablePanelCell iColorablePanelCell) {
        Minecraft.getMinecraft().displayGuiScreen(new TinyBlockGUI(panelTile, cellIndex, iColorablePanelCell));
    }
}
