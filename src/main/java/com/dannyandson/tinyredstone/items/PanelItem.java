package com.dannyandson.tinyredstone.items;

import com.dannyandson.tinyredstone.setup.ModSetup;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class PanelItem extends ItemBlock {



    public PanelItem()
    {
        super(Registration.REDSTONE_PANEL_BLOCK);
        setCreativeTab(ModSetup.ITEM_GROUP);
        setTileEntityItemStackRenderer(new PanelItemRenderer());
    }

    @Override
    public  void  addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flags)
    {
        list.add(new TextComponentTranslation("message.item.redstone_panel").getFormattedText());
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (stack.getTagCompound()!=null && stack.getTagCompound().hasKey("BlockEntityTag") ) {
            NBTTagCompound itemTag = stack.getTagCompound().getCompoundTag("BlockEntityTag");
            if(itemTag.hasKey("hasBase") && !itemTag.getBoolean("hasBase"))
                return new TextComponentTranslation("block.tinyredstone.tiny_container").getFormattedText();
        }

        return super.getItemStackDisplayName(stack);
    }

}
