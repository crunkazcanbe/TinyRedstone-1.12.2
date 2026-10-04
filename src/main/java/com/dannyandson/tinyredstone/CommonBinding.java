package com.dannyandson.tinyredstone;

import com.dannyandson.tinyredstone.api.AbstractPanelCellItem;
import com.dannyandson.tinyredstone.blocks.PanelBlock;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.RotationLock;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = TinyRedstone.MODID)
public class CommonBinding {
    @SubscribeEvent
    public static void onPlayerLogoff(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) {
            RotationLock.removeLock(false);
        } else {
            RotationLock.removeServerLock(player);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        //allow creative players to remove cells by left clicking with wrench or cell item
        if (event.getEntityPlayer().isCreative() &&
                event.getEntityPlayer().world.getBlockState(event.getPos()).getBlock() instanceof PanelBlock &&
                (
                        event.getEntityPlayer().getHeldItemMainhand().getItem() == Registration.REDSTONE_WRENCH ||
                                event.getEntityPlayer().getHeldItemMainhand().getItem() instanceof AbstractPanelCellItem
                )) {
            IBlockState blockState = event.getEntityPlayer().world.getBlockState(event.getPos());
            PanelBlock panelBlock = (PanelBlock) blockState.getBlock();
            panelBlock.onBlockClicked(event.getEntityPlayer().world, event.getPos(), event.getEntityPlayer());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getEntityPlayer().isSneaking() && PanelBlock.isPanelCellItem(event.getItemStack().getItem()))
        {
            TileEntity te = event.getEntityPlayer().world.getTileEntity(event.getPos());
            if (te instanceof PanelTile)
            {
                // 1.12.2 onBlockActivated takes the hit vector as three floats; derive them from the event hit vec.
                Vec3d hitVec = event.getHitVec();
                EnumFacing face = event.getFace();
                float hitX = (float) (hitVec.x - event.getPos().getX());
                float hitY = (float) (hitVec.y - event.getPos().getY());
                float hitZ = (float) (hitVec.z - event.getPos().getZ());
                IBlockState panelState = event.getEntityPlayer().world.getBlockState(event.getPos());
                Registration.REDSTONE_PANEL_BLOCK.onBlockActivated(
                        event.getEntityPlayer().world, event.getPos(), panelState,
                        event.getEntityPlayer(), event.getHand(), face, hitX, hitY, hitZ);
                event.setCanceled(true);
            }
        }
    }
}
