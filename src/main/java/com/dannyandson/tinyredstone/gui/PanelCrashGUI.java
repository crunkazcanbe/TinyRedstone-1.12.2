package com.dannyandson.tinyredstone.gui;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.network.CrashFlagResetSync;
import com.dannyandson.tinyredstone.network.ModNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class PanelCrashGUI extends GuiScreen {

    private final ResourceLocation GUI = new ResourceLocation(TinyRedstone.MODID, "textures/gui/transparent.png");

    private static final int WIDTH = 250;
    private static final int HEIGHT = 100;

    private static final int BTN_ENABLE = 0;
    private static final int BTN_CLOSE = 1;

    private final PanelTile panelTile;
    private final List<ModWidget> modWidgets = new ArrayList<ModWidget>();

    protected PanelCrashGUI(PanelTile panelTile) {
        this.panelTile = panelTile;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.modWidgets.clear();

        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        int top = relY + TinyGuiStyle.TITLE_BAR_H + 6;

        TextComponentTranslation crashTextComponent = new TextComponentTranslation((panelTile.isCrashed()) ? "tinyredstone.gui.crash.msg" : "tinyredstone.gui.overflow.msg");
        int lineY = top;
        for (String line : crashTextComponent.getFormattedText().split("\n", 5)) {
            modWidgets.add(new ModWidget(relX + 6, lineY, WIDTH - 2, 10, new TextComponentString(line)));
            lineY += 10;
        }

        this.buttonList.add(((TinyButton) new TinyButton(BTN_ENABLE, relX + 50, relY + HEIGHT - 22, 70, 18, new TextComponentTranslation("tinyredstone.enable").getFormattedText())).setGreen(true).setCenterLabel(true));
        this.buttonList.add(((TinyButton) new TinyButton(BTN_CLOSE, relX + 130, relY + HEIGHT - 22, 70, 18, new TextComponentTranslation("tinyredstone.close").getFormattedText())).setCenterLabel(true));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_ENABLE) {
            enable();
        } else if (button.id == BTN_CLOSE) {
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
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void close() {
        this.mc.displayGuiScreen(null);
    }

    private void enable() {
        panelTile.resetCrashFlag();
        panelTile.resetOverflownFlag();
        ModNetworkHandler.sendToServer(new CrashFlagResetSync(panelTile.getPos()));
        this.close();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        TinyGuiStyle.drawPanel(relX, relY, WIDTH, HEIGHT,
                new TextComponentTranslation("tinyredstone.gui.crash.title").getFormattedText());

        for (ModWidget widget : modWidgets) {
            widget.renderButton(mouseX, mouseY, partialTicks);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public static void open(PanelTile panelTile) {
        Minecraft.getMinecraft().displayGuiScreen(new PanelCrashGUI(panelTile));
    }

}
