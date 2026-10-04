package com.dannyandson.tinyredstone.blocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

public class PanelTileColor implements IBlockColor {

    @Override
    public int colorMultiplier(IBlockState state, @Nullable IBlockAccess world, @Nullable BlockPos pos, int tint) {
        if (world != null) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof PanelTile) {
                PanelTile panelTile = (PanelTile) te;
                return panelTile.getColor();
            }
        }
        return -1;
    }
}
