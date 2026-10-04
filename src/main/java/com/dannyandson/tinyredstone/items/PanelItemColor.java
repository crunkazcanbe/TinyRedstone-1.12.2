package com.dannyandson.tinyredstone.items;

import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class PanelItemColor implements IItemColor {
    @Override
    public int colorMultiplier(ItemStack p_getColor_1_, int p_getColor_2_)
    {
        if (p_getColor_1_.getTagCompound()!=null && p_getColor_1_.getTagCompound().hasKey("BlockEntityTag") ) {
            NBTTagCompound blockEntityTag = p_getColor_1_.getTagCompound().getCompoundTag("BlockEntityTag");
            if (blockEntityTag.hasKey("color")) {
                int color = blockEntityTag.getInteger("color");
                return color;
            }
        }
        return EnumDyeColor.GRAY.getColorValue();
    }
}
