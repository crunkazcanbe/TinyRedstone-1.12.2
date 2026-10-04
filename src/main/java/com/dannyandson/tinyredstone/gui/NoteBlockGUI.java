package com.dannyandson.tinyredstone.gui;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.panelcells.NoteBlock;
import com.dannyandson.tinyredstone.network.ModNetworkHandler;
import com.dannyandson.tinyredstone.network.NoteBlockInstrumentSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoteBlockGUI extends GuiScreen {

    private static final int WIDTH = 250;
    private static final int HEIGHT = 145;

    private static final int BTN_CLOSE = 99;

    private final PanelTile panelTile;
    private final Integer cellIndex;
    private final NoteBlock tinyNoteBlock;

    private final List<ModWidget> modWidgets = new ArrayList<ModWidget>();
    // Maps each instrument button's id to the instrument string it sets.
    private final Map<Integer, String> instrumentButtons = new HashMap<Integer, String>();

    private final ResourceLocation GUI = new ResourceLocation(TinyRedstone.MODID, "textures/gui/transparent.png");

    protected NoteBlockGUI(PanelTile panelTile, Integer cellIndex, NoteBlock tinyNoteBlock) {
        this.panelTile = panelTile;
        this.cellIndex = cellIndex;
        this.tinyNoteBlock = tinyNoteBlock;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.modWidgets.clear();
        this.instrumentButtons.clear();

        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        int top = relY + TinyGuiStyle.TITLE_BAR_H + 6;

        int r0 = top, r1 = top + 22, r2 = top + 44, r3 = top + 66;
        addInstrumentButton(relX + 5,   r0, "tinyredstone.noteblock.bass", "bass");
        addInstrumentButton(relX + 65,  r0, "tinyredstone.noteblock.snare", "snare");
        addInstrumentButton(relX + 125, r0, "tinyredstone.noteblock.hat", "hat");
        addInstrumentButton(relX + 185, r0, "tinyredstone.noteblock.basedrum", "basedrum");

        addInstrumentButton(relX + 5,   r1, "tinyredstone.noteblock.bell", "bell");
        addInstrumentButton(relX + 65,  r1, "tinyredstone.noteblock.flute", "flute");
        addInstrumentButton(relX + 125, r1, "tinyredstone.noteblock.chime", "chime");
        addInstrumentButton(relX + 185, r1, "tinyredstone.noteblock.guitar", "guitar");

        addInstrumentButton(relX + 5,   r2, "tinyredstone.noteblock.xylophone", "xylophone");
        addInstrumentButton(relX + 65,  r2, "tinyredstone.noteblock.iron_xylophone", "iron_xylophone");
        addInstrumentButton(relX + 125, r2, "tinyredstone.noteblock.cow_bell", "cow_bell");
        addInstrumentButton(relX + 185, r2, "tinyredstone.noteblock.didgeridoo", "didgeridoo");

        addInstrumentButton(relX + 5,   r3, "tinyredstone.noteblock.bit", "bit");
        addInstrumentButton(relX + 65,  r3, "tinyredstone.noteblock.banjo", "banjo");
        addInstrumentButton(relX + 125, r3, "tinyredstone.noteblock.pling", "pling");
        addInstrumentButton(relX + 185, r3, "tinyredstone.noteblock.harp", "harp");

        // green DONE button along the bottom
        this.buttonList.add(((TinyButton) new TinyButton(BTN_CLOSE, relX + 8, relY + HEIGHT - 20, WIDTH - 16, 16,
                new TextComponentTranslation("tinyredstone.close").getFormattedText())).setGreen(true).setCenterLabel(true));
    }

    private void addInstrumentButton(int x, int y, String langKey, String instrument) {
        int id = this.buttonList.size();
        this.buttonList.add(((TinyButton) new TinyButton(id, x, y, 58, 18, new TextComponentTranslation(langKey).getFormattedText()))
                .setIcon(0xFFB060D0));
        this.instrumentButtons.put(id, instrument);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_CLOSE) {
            close();
        } else if (instrumentButtons.containsKey(button.id)) {
            setInstrument(instrumentButtons.get(button.id));
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

    private void setInstrument(String instrument) {
        this.tinyNoteBlock.setInstrument(instrument);
        ModNetworkHandler.sendToServer(new NoteBlockInstrumentSync(panelTile.getPos(), cellIndex, instrument));
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
                new TextComponentTranslation("tinyredstone.gui.noteblock.title").getFormattedText());

        for (ModWidget widget : modWidgets) {
            widget.renderButton(mouseX, mouseY, partialTicks);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public static void open(PanelTile panelTile, Integer cellIndex, NoteBlock tinyNoteBlock) {
        Minecraft.getMinecraft().displayGuiScreen(new NoteBlockGUI(panelTile, cellIndex, tinyNoteBlock));
    }
}
