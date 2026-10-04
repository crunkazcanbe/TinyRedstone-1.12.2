package com.dannyandson.tinyredstone.items;

import com.dannyandson.tinyredstone.blocks.PanelBlock;
import com.dannyandson.tinyredstone.setup.ModSetup;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class RedstoneWrench extends Item {
    public RedstoneWrench() {
        super();
        setMaxStackSize(1);
        setCreativeTab(ModSetup.ITEM_GROUP);
    }

    @Override
    @Nonnull
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            if (player != null && player.isSneaking()) {
                // Make sure the block gets activated if it is a PanelBlock
                IBlockState state = world.getBlockState(pos);
                Block block = state.getBlock();
                if (block instanceof PanelBlock) {
                    if (block.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ))
                        return EnumActionResult.SUCCESS;
                }
            }
        }

        return EnumActionResult.SUCCESS;
    }

    @Override
    public  void  addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flags)
    {
        list.add(new TextComponentTranslation("message.item.redstone_wrench").getFormattedText());
    }

    public boolean canPlayerBreakBlockWhileHolding(IBlockState state, World worldIn, BlockPos pos, EntityPlayer player) {
        return !(state.getBlock() instanceof PanelBlock);
    }

    public RayTraceResult getBlockRayTraceResult(World world, EntityPlayer player)
    {
        // PORT-TODO: 1.16 used Item.getPlayerPOVHitResult(world, player, RayTraceContext.FluidMode.ANY).
        // 1.12.2 has no direct equivalent helper; using the entity look ray trace (no fluid handling).
        return player.rayTrace(5.0D, 1.0F);
    }
}
