package com.dannyandson.tinyredstone.gui;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.network.ClearPanelSync;
import com.dannyandson.tinyredstone.network.ModNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClearPanelGUI extends GuiScreen {

    private static final int WIDTH = 220;
    private static final int HEIGHT = 78;

    private static final int BTN_YES = 0;
    private static final int BTN_CANCEL = 1;

    private final ResourceLocation GUI = new ResourceLocation(TinyRedstone.MODID, "textures/gui/transparent.png");

    private final PanelTile panelTile;
    // 1.12.2 GuiScreen.buttonList only holds GuiButton; ModWidgets are tracked separately.
    private final List<ModWidget> modWidgets = new ArrayList<ModWidget>();

    protected ClearPanelGUI(PanelTile panelTile) {
        this.panelTile = panelTile;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.modWidgets.clear();

        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        int top = relY + TinyGuiStyle.TITLE_BAR_H + 6;

        modWidgets.add(new ModWidget(relX, top, WIDTH, 14, new TextComponentTranslation("tinyredstone.gui.clearpanel.msg"))
                .setTextHAlignment(ModWidget.HAlignment.CENTER));

        this.buttonList.add(((TinyButton) new TinyButton(BTN_YES, relX + 20, top + 22, 80, 18, new TextComponentTranslation("tinyredstone.yes").getFormattedText())).setGreen(true).setCenterLabel(true));
        this.buttonList.add(((TinyButton) new TinyButton(BTN_CANCEL, relX + 120, top + 22, 80, 18, new TextComponentTranslation("tinyredstone.cancel").getFormattedText())).setCenterLabel(true));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_YES) {
            removeCells();
        } else if (button.id == BTN_CANCEL) {
            close();
        }
    }

    private void removeCells() {
        ModNetworkHandler.sendToServer(new ClearPanelSync(panelTile.getPos()));
        close();
    }

    private void close() {
        this.mc.displayGuiScreen(null);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        if (TinyGuiStyle.isOverClose(relX, relY, WIDTH, mouseX, mouseY)) {
            close();
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        TinyGuiStyle.drawPanel(relX, relY, WIDTH, HEIGHT,
                new TextComponentTranslation("tinyredstone.gui.clearpanel.title").getFormattedText());

        for (ModWidget widget : modWidgets) {
            widget.renderButton(mouseX, mouseY, partialTicks);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public static void open(PanelTile panelTile) {
        Minecraft.getMinecraft().displayGuiScreen(new ClearPanelGUI(panelTile));
    }
}
