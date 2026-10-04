package com.dannyandson.tinyredstone.api;

import com.dannyandson.tinyredstone.Config;
import com.dannyandson.tinyredstone.blocks.PanelBlock;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Helpful abstract class for panel cell items to inherit.
 * Prevents redstone panels from being broken when hit with a panel cell item.
 * Also referenced by left click event to prevent creative players from breaking panels.
 */
public abstract class AbstractPanelCellItem extends Item {
    public AbstractPanelCellItem() {
        super();
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (Config.ALLOW_WORLD_PLACEMENT) {
            ItemStack itemStackCopy = player.getHeldItem(hand).copy();
            EnumActionResult result = Registration.REDSTONE_PANEL_ITEM.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
            player.setHeldItem(hand, itemStackCopy);

            TileEntity te = world.getTileEntity(pos.offset(facing));
            if (te instanceof PanelTile && player != null) {
                PanelTile panelTile = (PanelTile) te;
                // PORT-TODO: original passed a getPlayerCollisionHitResult RayTraceResult to onBlockActivated;
                // 1.12.2 IBlockState#onBlockActivated takes hitX/hitY/hitZ floats instead. Forwarding the
                // click coordinates from this use; verify PanelBlock signature once blocks are ported.
                Registration.REDSTONE_PANEL_BLOCK.onBlockActivated(world, panelTile.getPos(), panelTile.getBlockState(), player, hand, facing, hitX, hitY, hitZ);
            }
            return result;
        }
        return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }


    /**
     * Called before a block is broken. Return true to prevent default block
     * harvesting.
     * We return true if the block is being hit is a redstone panel to prevent
     * harvesting the block when attempting to remove components.
     *
     * Note: In SMP, this is called on both client and server sides!
     *
     * @param itemstack The current ItemStack
     * @param pos       Block's position in world
     * @param player    The Player that is wielding the item
     * @return True to prevent harvesting, false to continue as normal
     */
    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, EntityPlayer player) {
        return player.world.getBlockState(pos).getBlock() instanceof PanelBlock;
    }

}
