package com.dannyandson.tinyredstone.items;

import com.dannyandson.tinyredstone.api.AbstractPanelCellItem;
import com.dannyandson.tinyredstone.setup.ModSetup;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.Style;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class PanelCellItem extends AbstractPanelCellItem {
    public PanelCellItem() {
        super();
        setCreativeTab(ModSetup.ITEM_GROUP);
    }

    public PanelCellItem(TileEntityItemStackRenderer renderer){
        super();
        setCreativeTab(ModSetup.ITEM_GROUP);
        if (renderer != null)
            setTileEntityItemStackRenderer(renderer);
    }

    @Override
    public  void  addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flags) {
        if (GuiScreen.isShiftKeyDown()) {
            list.add(new TextComponentTranslation("message.item.redstone_panel_cell")
                    .setStyle(new Style().setColor(TextFormatting.GRAY)).getFormattedText());
            list.add(new TextComponentTranslation("message." + this.getUnlocalizedName())
                    .setStyle(new Style().setColor(TextFormatting.RED)).getFormattedText());
        } else
            list.add(new TextComponentTranslation("tinyredstone.tooltip.press_shift")
                    .setStyle(new Style().setColor(TextFormatting.DARK_GRAY)).getFormattedText());
    }

}
