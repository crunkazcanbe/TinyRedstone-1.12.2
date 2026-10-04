package com.dannyandson.tinyredstone.gui;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.items.Blueprint;
import com.dannyandson.tinyredstone.network.BlueprintSync;
import com.dannyandson.tinyredstone.network.ModNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BlueprintGUI extends GuiScreen {

    private static final int WIDTH = 130;
    private static final int HEIGHT = 90;

    private static final int BTN_CLOSE = 0;
    private static final int BTN_IMPORT_EXPORT = 1;

    private final ResourceLocation GUI = new ResourceLocation(TinyRedstone.MODID, "textures/gui/transparent.png");

    private final ItemStack blueprint;
    private boolean dialogOpen = false;
    private GuiButton importExportButton;

    private final List<ModWidget> modWidgets = new ArrayList<ModWidget>();

    protected BlueprintGUI(ItemStack blueprint) {
        this.blueprint = blueprint;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.modWidgets.clear();

        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        int top = relY + TinyGuiStyle.TITLE_BAR_H + 8;

        // 1.12.2 ItemStack.hasTagCompound() replaces 1.16 ItemStack.hasTag().
        if (this.blueprint.hasTagCompound())
            importExportButton = ((TinyButton) new TinyButton(BTN_IMPORT_EXPORT, relX + 20, top, WIDTH - 40, 18, new TextComponentTranslation("tinyredstone.export").getFormattedText())).setIcon(0xFF60A0E0);
        else
            importExportButton = ((TinyButton) new TinyButton(BTN_IMPORT_EXPORT, relX + 20, top, WIDTH - 40, 18, new TextComponentTranslation("tinyredstone.import").getFormattedText())).setIcon(0xFF60A0E0);

        this.buttonList.add(importExportButton);

        // green DONE button along the bottom
        this.buttonList.add(((TinyButton) new TinyButton(BTN_CLOSE, relX + 8, relY + HEIGHT - 20, WIDTH - 16, 16,
                new TextComponentTranslation("tinyredstone.close").getFormattedText())).setGreen(true).setCenterLabel(true));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_CLOSE) {
            close();
        } else if (button.id == BTN_IMPORT_EXPORT) {
            if (this.blueprint.hasTagCompound())
                exportToFile();
            else
                importFromFile();
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

    public void exportToFile() {
        if (!dialogOpen) {
            this.dialogOpen = true;
            // Visually replace the button with a "busy" placeholder while the dialog is open.
            this.buttonList.remove(importExportButton);
            this.modWidgets.add(new ModWidget((this.width - WIDTH) / 2 + 20, (this.height - HEIGHT) / 2 + TinyGuiStyle.TITLE_BAR_H + 8, WIDTH - 40, 18, 0xFF444444).setBeveled(true));

            new Thread(() -> {
                // PORT-TODO: 1.16 used LWJGL3 org.lwjgl.util.tinyfd.TinyFileDialogs (not present in the
                // LWJGL2 shipped with 1.12.2). Replaced with a Swing JFileChooser save dialog, which is
                // the standard 1.12.2-era approach. Behaviour (pick a .json path, write the tag) matches.
                String path = showSaveDialog("blueprint.json");
                this.dialogOpen = false;

                if (path != null) {
                    try {
                        File file = new File(path);
                        if (file.createNewFile()) {
                            FileWriter writer = new FileWriter(path);
                            writer.write(blueprint.getTagCompound().toString());
                            writer.close();
                        }
                    } catch (IOException e) {
                        TinyRedstone.LOGGER.error("IOException attempting to save blueprint json to file: " + e.getLocalizedMessage());
                    }
                }
            }).start();
        }
    }

    public void importFromFile() {
        if (!dialogOpen) {
            this.dialogOpen = true;

            this.buttonList.remove(importExportButton);
            this.modWidgets.add(new ModWidget((this.width - WIDTH) / 2 + 20, (this.height - HEIGHT) / 2 + TinyGuiStyle.TITLE_BAR_H + 8, WIDTH - 40, 18, 0xFF444444).setBeveled(true));

            new Thread(() -> {
                // PORT-TODO: 1.16 used LWJGL3 org.lwjgl.util.tinyfd.TinyFileDialogs open dialog.
                // Replaced with a Swing JFileChooser open dialog filtered to *.json (LWJGL2-safe).
                String path = showOpenDialog();
                this.dialogOpen = false;

                if (path != null) {
                    StringBuilder data = new StringBuilder();
                    try {
                        File file = new File(path);
                        Scanner scanner = new Scanner(file);
                        while (scanner.hasNextLine()) {
                            data.append(scanner.nextLine());
                        }
                        scanner.close();
                    } catch (FileNotFoundException e) {
                        TinyRedstone.LOGGER.error("FileNotFoundException attempting to load blueprint json to file: " + e.getLocalizedMessage());
                    }

                    try {
                        // 1.12.2: JsonToNBT.getTagFromJson(...) throws NBTException (was CommandSyntaxException in 1.16).
                        NBTTagCompound nbt = JsonToNBT.getTagFromJson(data.toString());
                        NBTTagCompound cleanNBT = Blueprint.cleanUpBlueprintNBT(nbt);
                        if (cleanNBT != null) {
                            this.blueprint.setTagCompound(cleanNBT);
                            ModNetworkHandler.sendToServer(new BlueprintSync(cleanNBT));
                        }
                    } catch (NBTException e) {
                        TinyRedstone.LOGGER.error("NBTException reading JSON from user file: " + e.getLocalizedMessage());
                    }
                }
            }).start();
        }
    }

    private String showSaveDialog(String defaultName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(new TextComponentTranslation("tinyredstone.save_file").getUnformattedText());
        chooser.setSelectedFile(new File(defaultName));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON File (*.json)", "json"));
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    private String showOpenDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(new TextComponentTranslation("tinyredstone.choose_file").getUnformattedText());
        chooser.setFileFilter(new FileNameExtensionFilter("JSON File (*.json)", "json"));
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int relX = (this.width - WIDTH) / 2;
        int relY = (this.height - HEIGHT) / 2;
        TinyGuiStyle.drawPanel(relX, relY, WIDTH, HEIGHT,
                new TextComponentTranslation("tinyredstone.gui.blueprint.title").getFormattedText());

        for (ModWidget widget : modWidgets) {
            widget.renderButton(mouseX, mouseY, partialTicks);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public static void open(ItemStack blueprint) {
        Minecraft.getMinecraft().displayGuiScreen(new BlueprintGUI(blueprint));
    }

}
