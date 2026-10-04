package com.dannyandson.tinyredstone.items;

import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;

public class TinyBlockItem extends PanelCellItem {

    public TinyBlockItem()
    {
        super(new TinyBlockItemRenderer());
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String thisName = super.getItemStackDisplayName(stack);
        String fromBlockName = null;
        if (stack.hasTagCompound()) {
            NBTTagCompound itemNBT = stack.getTagCompound();
            NBTTagCompound madeFromTag = itemNBT.getCompoundTag("made_from");
            if (madeFromTag.hasKey("namespace")) {
                fromBlockName = (new TextComponentTranslation("block." + madeFromTag.getString("namespace") + "." + madeFromTag.getString("path"))).getFormattedText();
            }
        }
        if (fromBlockName==null){
            if (stack.getItem()== Registration.TINY_SOLID_BLOCK){
                    fromBlockName = new TextComponentTranslation("block.minecraft.white_wool").getFormattedText();
            } else if (stack.getItem()== Registration.TINY_TRANSPARENT_BLOCK){
                fromBlockName = new TextComponentTranslation("block.minecraft.glass").getFormattedText();
            }
        }
        return thisName + " (" + fromBlockName + ")";
    }

}
