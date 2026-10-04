package com.dannyandson.tinyredstone.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class PanelCoverItem extends PanelCellItem{

    public PanelCoverItem()   {
        super(new PanelCoverItemRenderer());
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return EnumActionResult.PASS;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (stack.hasTagCompound()) {
            String thisName = super.getItemStackDisplayName(stack);
            String fromBlockName = null;
            NBTTagCompound itemNBT = stack.getTagCompound();
            NBTTagCompound madeFromTag = itemNBT.getCompoundTag("made_from");
            if (madeFromTag.hasKey("namespace")) {
                fromBlockName = (new TextComponentTranslation("block." + madeFromTag.getString("namespace") + "." + madeFromTag.getString("path"))).getFormattedText();
            }
            return thisName + " (" + fromBlockName + ")";
        }

        return super.getItemStackDisplayName(stack);
    }


}
