package com.dannyandson.tinyredstone.gui;

import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.panelcells.GateType;
import com.dannyandson.tinyredstone.blocks.panelcells.LogicGate;
import com.dannyandson.tinyredstone.network.LogicGateConfigSync;
import com.dannyandson.tinyredstone.network.ModNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Logic Gate's morphing options menu (task #235). Right-click a gate with the colored wrench.
 * A fixed-size panel with a SCROLLING body (mouse wheel): a grid of all 34 gate types (hover shows
 * the full name as a tooltip; current = green) then the full per-gate option set — output power,
 * invert output, per-input invert, interval, max, pulse length, comparator mode, rotate, reset.
 * Title bar + Done stay fixed; everything between scrolls. Changes apply locally + sync to server.
 */
public class LogicGateGUI extends GuiScreen {

    private static final int WIDTH = 250;
    private static final int HEIGHT = 200;
    private static final int COLS = 5, BTN_W = 46, BTN_H = 13;

    private static final int ID_POW_M = 200, ID_POW_P = 201, ID_INV_OUT = 202,
            ID_INV_B = 203, ID_INV_L = 204, ID_INV_R = 205,
            ID_INT_M = 206, ID_INT_P = 207, ID_MAX_M = 208, ID_MAX_P = 209,
            ID_PUL_M = 210, ID_PUL_P = 211, ID_SUB = 212,
            ID_ROT_CW = 213, ID_ROT_CCW = 214, ID_RESET = 215, ID_CLOSE = 220;

    private final PanelTile panelTile;
    private final Integer cellIndex;
    private final LogicGate gate;

    private final List<ModWidget> modWidgets = new ArrayList<ModWidget>();

    private GateType uiType;
    private int uiInterval, uiMax, uiStr, uiPulse, uiRot;
    private boolean uiInvO, uiIB, uiIL, uiIR, uiSub;

    private int scroll = 0, maxScroll = 0;
    private int viewTop, viewBottom;

    protected LogicGateGUI(PanelTile panelTile, Integer cellIndex, LogicGate gate) {
        this.panelTile = panelTile;
        this.cellIndex = cellIndex;
        this.gate = gate;
    }

    private void load() {
        uiType = gate.getGateType();
        uiInterval = gate.getInterval(); uiMax = gate.getMaxCount(); uiStr = gate.getOutputStrength();
        uiPulse = gate.getPulseLength(); uiRot = gate.getRotation();
        uiInvO = gate.isInvertOutput(); uiIB = gate.isInvBack(); uiIL = gate.isInvLeft();
        uiIR = gate.isInvRight(); uiSub = gate.isSubtractMode();
    }

    private void commit() {
        gate.applyConfig(uiType, uiInterval, uiMax, uiStr, uiInvO, uiIB, uiIL, uiIR, uiPulse, uiSub, uiRot);
        ModNetworkHandler.sendToServer(new LogicGateConfigSync(panelTile.getPos(), cellIndex, gate));
        initGui();
    }

    /** add a scrollable button at content-y `cy`; hidden+disabled if it falls outside the viewport */
    private TinyButton add(int id, int x, int cy, int w, int h, String s, boolean green, boolean enabled) {
        int y = viewTop + cy - scroll;
        TinyButton b = (TinyButton) new TinyButton(id, x, y, w, h, s).setCenterLabel(true);
        if (green) b.setGreen(true);
        b.enabled = enabled;
        if (y < viewTop || y + h > viewBottom) { b.visible = false; b.enabled = false; }
        this.buttonList.add(b);
        return b;
    }

    private void addLabel(int x, int cy, int w, String text) {
        int y = viewTop + cy - scroll;
        if (y < viewTop || y + 12 > viewBottom) return;
        modWidgets.add(new ModWidget(x, y, w, 12, new TextComponentString(text))
                .setTextHAlignment(ModWidget.HAlignment.LEFT).setTextVAlignment(ModWidget.VAlignment.MIDDLE));
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.modWidgets.clear();
        load();

        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        viewTop = relY + TinyGuiStyle.TITLE_BAR_H + 4;
        viewBottom = relY + HEIGHT - 24;

        int lx = relX + 6;
        GateType[] types = GateType.values();

        // ---- content laid out in content-space (cy), then offset by scroll ----
        int cy = 0;
        for (int i = 0; i < types.length; i++) {
            GateType t = types[i];
            int x = lx + (i % COLS) * (BTN_W + 2);
            int rowY = cy + (i / COLS) * (BTN_H + 1);
            String s = t.title.length() > 7 ? t.title.substring(0, 7) : t.title;
            add(i, x, rowY, BTN_W, BTN_H, s, t == uiType, true);
        }
        int rows = (types.length + COLS - 1) / COLS;
        cy += rows * (BTN_H + 1) + 6;
        int step = 18;

        // Only the options that apply to the SELECTED gate are shown.

        // Output power + invert output apply to every gate
        addLabel(lx, cy + 4, 170, "Output power: " + uiStr);
        add(ID_POW_M, relX + 178, cy, 28, 16, "-", false, true);
        add(ID_POW_P, relX + 214, cy, 28, 16, "+", false, true);
        cy += step;

        add(ID_INV_OUT, lx, cy, 150, 16, "Invert output: " + (uiInvO ? "ON" : "off"), uiInvO, true);
        cy += step;

        // Invert input — only the sides this gate actually reads
        if (uiType.usesBack || uiType.usesLeft || uiType.usesRight) {
            if (uiType.usesBack)  add(ID_INV_B, lx,       cy, 76, 16, "Inv Back", uiIB, true);
            if (uiType.usesLeft)  add(ID_INV_L, lx + 80,  cy, 76, 16, "Inv Left", uiIL, true);
            if (uiType.usesRight) add(ID_INV_R, lx + 160, cy, 76, 16, "Inv Right", uiIR, true);
            cy += step;
        }

        // Interval — timer / sequencer / state cell only
        if (uiType.usesInterval()) {
            addLabel(lx, cy + 4, 170, "Interval: " + uiInterval + "t");
            add(ID_INT_M, relX + 178, cy, 28, 16, "-", false, true);
            add(ID_INT_P, relX + 214, cy, 28, 16, "+", false, true);
            cy += step;
        }

        // Max count — counter only
        if (uiType.usesMaxCount()) {
            addLabel(lx, cy + 4, 170, "Max count: " + uiMax);
            add(ID_MAX_M, relX + 178, cy, 28, 16, "-", false, true);
            add(ID_MAX_P, relX + 214, cy, 28, 16, "+", false, true);
            cy += step;
        }

        // Pulse length — pulse former / timer / synchronizer only
        if (uiType.usesPulse()) {
            addLabel(lx, cy + 4, 170, "Pulse length: " + uiPulse + "t");
            add(ID_PUL_M, relX + 178, cy, 28, 16, "-", false, true);
            add(ID_PUL_P, relX + 214, cy, 28, 16, "+", false, true);
            cy += step;
        }

        // Compare mode — comparator only
        if (uiType.usesSubtract()) {
            add(ID_SUB, lx, cy, 150, 16, "Compare mode: " + (uiSub ? "subtract" : "compare"), uiSub, true);
            cy += step;
        }

        // Rotate + Reset apply to every gate
        add(ID_ROT_CW,  lx,       cy, 76, 16, "Rotate CW", false, true);
        add(ID_ROT_CCW, lx + 80,  cy, 76, 16, "Rotate CCW", false, true);
        add(ID_RESET,   lx + 160, cy, 76, 16, "Reset", false, true);
        cy += step;

        // total content height -> clamp scroll
        int contentH = cy;
        int viewH = viewBottom - viewTop;
        maxScroll = Math.max(0, contentH - viewH);
        if (scroll > maxScroll) { scroll = maxScroll; initGui(); return; }

        // fixed Done bar (not scrolled)
        this.buttonList.add(((TinyButton) new TinyButton(ID_CLOSE, relX + 8, relY + HEIGHT - 20, WIDTH - 16, 16, "Done")
                .setGreen(true).setCenterLabel(true)));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (wheel > 0 ? 14 : -14)));
            initGui();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int id = button.id;
        GateType[] types = GateType.values();
        if (id == ID_CLOSE) { close(); return; }
        else if (id >= 0 && id < types.length) uiType = types[id];
        else if (id == ID_POW_M) uiStr = Math.max(1, uiStr - 1);
        else if (id == ID_POW_P) uiStr = Math.min(15, uiStr + 1);
        else if (id == ID_INV_OUT) uiInvO = !uiInvO;
        else if (id == ID_INV_B) uiIB = !uiIB;
        else if (id == ID_INV_L) uiIL = !uiIL;
        else if (id == ID_INV_R) uiIR = !uiIR;
        else if (id == ID_INT_M) uiInterval = Math.max(1, uiInterval - 2);
        else if (id == ID_INT_P) uiInterval = uiInterval + 2;
        else if (id == ID_MAX_M) uiMax = Math.max(1, uiMax - 1);
        else if (id == ID_MAX_P) uiMax = uiMax + 1;
        else if (id == ID_PUL_M) uiPulse = Math.max(1, uiPulse - 1);
        else if (id == ID_PUL_P) uiPulse = uiPulse + 1;
        else if (id == ID_SUB) uiSub = !uiSub;
        else if (id == ID_ROT_CW) uiRot = (uiRot + 1) % 4;
        else if (id == ID_ROT_CCW) uiRot = (uiRot + 3) % 4;
        else if (id == ID_RESET) { /* commit re-applies + resets runtime */ }
        commit();
    }

    private void close() { this.mc.displayGuiScreen(null); }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        if (TinyGuiStyle.isOverClose(relX, relY, WIDTH, mouseX, mouseY)) { close(); return; }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        TinyGuiStyle.drawPanel(relX, relY, WIDTH, HEIGHT, "Logic Gate — " + uiType.title);
        for (ModWidget w : modWidgets) w.renderButton(mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);

        // scroll hint
        if (maxScroll > 0) {
            this.drawString(this.fontRenderer, "▲▼ scroll", relX + WIDTH - 60, relY + HEIGHT - 32, 0xFFAAAAAA);
        }

        // hover tooltip: full gate name for the gate-grid button under the cursor
        GateType[] types = GateType.values();
        for (GuiButton b : this.buttonList) {
            if (b.id >= 0 && b.id < types.length && b.visible
                    && mouseX >= b.x && mouseX < b.x + b.width && mouseY >= b.y && mouseY < b.y + b.height) {
                this.drawHoveringText(types[b.id].title, mouseX, mouseY);
                break;
            }
        }
    }

    public static void open(PanelTile panelTile, Integer cellIndex, LogicGate gate) {
        Minecraft.getMinecraft().displayGuiScreen(new LogicGateGUI(panelTile, cellIndex, gate));
    }
}
