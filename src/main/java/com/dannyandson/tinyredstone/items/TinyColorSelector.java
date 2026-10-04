package com.dannyandson.tinyredstone.items;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class TinyColorSelector extends RedstoneWrench {

    @Override
    public  void  addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flags)
    {
        list.add(new TextComponentTranslation("message.item.tiny_color_selector").getFormattedText());
    }
}
