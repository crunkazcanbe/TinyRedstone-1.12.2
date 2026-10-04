package com.dannyandson.tinyredstone.gui;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.panelcells.Repeater;
import com.dannyandson.tinyredstone.network.ModNetworkHandler;
import com.dannyandson.tinyredstone.network.RepeaterTickSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepeaterCellGUI extends GuiScreen {

    private static final int WIDTH = 190;
    private static final int HEIGHT = 110;

    private static final int BTN_CLOSE = 100;

    private final PanelTile panelTile;
    private final Integer cellIndex;
    private final Repeater repeaterCell;
    private ModWidget tickCount;

    private final List<ModWidget> modWidgets = new ArrayList<ModWidget>();
    // Maps a "change ticks" button id to the delta it applies.
    private final Map<Integer, Integer> changeButtons = new HashMap<Integer, Integer>();

    private final ResourceLocation GUI = new ResourceLocation(TinyRedstone.MODID, "textures/gui/transparent.png");

    protected RepeaterCellGUI(PanelTile panelTile, Integer cellIndex, Repeater repeaterCell) {
        this.panelTile = panelTile;
        this.cellIndex = cellIndex;
        this.repeaterCell = repeaterCell;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.modWidgets.clear();
        this.changeButtons.clear();

        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        int top = relY + TinyGuiStyle.TITLE_BAR_H + 4; // below red title bar
        Integer redstoneTicks = repeaterCell.getTicks() / 2;

        Float tSeconds = redstoneTicks.floatValue() / 10f;
        this.tickCount = new ModWidget(relX, top + 40, WIDTH, 14, new TextComponentString(redstoneTicks.toString() + " ticks (" + tSeconds.toString() + " seconds)"))
                .setTextHAlignment(ModWidget.HAlignment.CENTER).setTextVAlignment(ModWidget.VAlignment.MIDDLE);

        // "Delay" caption + tick-change buttons
        modWidgets.add(new ModWidget(relX + 6, top + 2, WIDTH - 2, 12, new TextComponentTranslation("tinyredstone.gui.repeater.msg"))
                .setTextHAlignment(ModWidget.HAlignment.LEFT));

        addChangeButton(relX + 8,   top + 16, "---", -200);
        addChangeButton(relX + 38,  top + 16, "--", -20);
        addChangeButton(relX + 68,  top + 16, "-", -2);

        addChangeButton(relX + 98,  top + 16, "+", 2);
        addChangeButton(relX + 128, top + 16, "++", 20);
        addChangeButton(relX + 158, top + 16, "+++", 200);

        modWidgets.add(this.tickCount);

        // green DONE button along the bottom
        this.buttonList.add(((TinyButton) new TinyButton(BTN_CLOSE, relX + 8, relY + HEIGHT - 20, WIDTH - 16, 16,
                new TextComponentTranslation("tinyredstone.close").getFormattedText())).setGreen(true).setCenterLabel(true));
    }

    private void addChangeButton(int x, int y, String label, int change) {
        int id = this.buttonList.size();
        this.buttonList.add(((TinyButton) new TinyButton(id, x, y, 24, 18, label)).setCenterLabel(true));
        this.changeButtons.put(id, change);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_CLOSE) {
            close();
        } else if (changeButtons.containsKey(button.id)) {
            changeTicks(changeButtons.get(button.id));
        }
    }

    /**
     * 1.12.2 has no mouseScrolled override; mouse wheel arrives via handleMouseInput()
     * as Mouse.getEventDWheel() (positive = scroll up). Original behaviour: scroll over the
     * panel changes ticks by scroll*2 (x10 with shift).
     */
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            if (
                    mouseX > (this.width - WIDTH) / 2 &&
                            mouseX < (this.width + WIDTH) / 2 &&
                            mouseY > (this.height - HEIGHT) / 2 &&
                            mouseY < (this.height + HEIGHT) / 2
            ) {
                Double dScroll = (wheel > 0 ? 1d : -1d) * 2;
                if (isShiftKeyDown())
                    dScroll *= 10;
                changeTicks(dScroll.intValue());
            }
        }
    }

    private void close() {
        this.mc.displayGuiScreen(null);
    }

    private void changeTicks(int change) {
        repeaterCell.setTicks(repeaterCell.getTicks() + change);

        ModNetworkHandler.sendToServer(new RepeaterTickSync(panelTile.getPos(), cellIndex, repeaterCell.getTicks()));

        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        int top = relY + TinyGuiStyle.TITLE_BAR_H + 4;

        Integer redstoneTicks = repeaterCell.getTicks() / 2;
        this.modWidgets.remove(this.tickCount);
        Float tSeconds = redstoneTicks.floatValue() / 10f;
        this.tickCount = new ModWidget(relX, top + 40, WIDTH, 14, new TextComponentString(redstoneTicks.toString() + " ticks (" + tSeconds.toString() + " seconds)"))
                .setTextHAlignment(ModWidget.HAlignment.CENTER).setTextVAlignment(ModWidget.VAlignment.MIDDLE);
        modWidgets.add(this.tickCount);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
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
                new TextComponentTranslation("tinyredstone.gui.repeater.title").getFormattedText());

        for (ModWidget widget : modWidgets) {
            widget.renderButton(mouseX, mouseY, partialTicks);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public static void open(PanelTile panelTile, Integer cellIndex, Repeater repeaterCell) {
        Minecraft.getMinecraft().displayGuiScreen(new RepeaterCellGUI(panelTile, cellIndex, repeaterCell));
    }

}
