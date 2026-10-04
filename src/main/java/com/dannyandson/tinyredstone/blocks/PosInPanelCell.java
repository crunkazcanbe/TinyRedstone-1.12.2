package com.dannyandson.tinyredstone.blocks;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class PosInPanelCell extends PanelCellPos {
    private final double x;
    private final double z;
    private final double y;

    protected PosInPanelCell(PanelTile panelTile, int row, int level, int cell, double x, double y, double z) {
        super(panelTile,row, cell,level);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public PanelCellSegment getSegment() {
        int segmentRow = Math.round((float)(this.x*3f)-0.5f);
        int segmentColumn = Math.round((float)(this.z*3f)-0.5f);

        if(segmentRow == 0) {
            switch (segmentColumn) {
                case 0: return PanelCellSegment.FRONT_RIGHT;
                case 1: return PanelCellSegment.FRONT;
                case 2: return PanelCellSegment.FRONT_LEFT;
            }
        } else if(segmentRow == 1) {
            switch (segmentColumn) {
                case 0: return PanelCellSegment.RIGHT;
                case 1: return PanelCellSegment.CENTER;
                case 2: return PanelCellSegment.LEFT;
            }
        } else if(segmentRow == 2) {
            switch (segmentColumn) {
                case 0: return PanelCellSegment.BACK_RIGHT;
                case 1: return PanelCellSegment.BACK;
                case 2: return PanelCellSegment.BACK_LEFT;
            }
        }
        return null;
    }

    public static PosInPanelCell fromCoordinates(PanelTile panelTile, PanelCellPos panelCellPos, double x, double y, double z) {
        if(x >= 0.0 && x <= 1.0 && z >= 0.0 && z <= 1.0) {
            double rotatedX;
            double rotatedZ;

            Side direction = panelCellPos.getCellFacing();

            if (direction == Side.FRONT) {
                rotatedX = z;
                rotatedZ = 1.0 - x;
            } else if (direction == Side.RIGHT) {
                rotatedX = 1.0 - x;
                rotatedZ = 1.0 - z;
            } else if (direction == Side.BACK){
                rotatedX = 1.0 - z;
                rotatedZ = x;
            } else {
                rotatedX = x;
                rotatedZ = z;
            }

            return new PosInPanelCell(panelTile, panelCellPos.getRow(),panelCellPos.getLevel(), panelCellPos.getColumn(), rotatedX,y, rotatedZ);
        }
        return null;
    }

    public static PosInPanelCell fromHitVec(PanelTile panelTile, BlockPos pos, RayTraceResult result) {

        // PORT-TODO: 1.16's BlockStateProperties.FACING does not exist in 1.12.2. The panel's facing
        // must come from the property the (user-converted) PanelBlock defines, e.g.
        //   EnumFacing panelFacing = panelTile.getBlockState().getValue(PanelBlock.FACING);
        // The user owns PanelBlock/PanelTile/PanelCellPos; once PanelBlock declares its 1.12.2 FACING
        // PropertyDirection, replace the line below to read it. Using getValue(...) directly here would
        // not compile until that property exists, so this is left as the explicit integration point.
        // Also note: 1.16 BlockRayTraceResult was converted to 1.12.2 RayTraceResult; getDirection()->
        // sideHit (field), getLocation()->hitVec (field), getStepX/Y/Z()->getFrontOffsetX/Y/Z().
        EnumFacing panelFacing = panelTile.getBlockState().getValue(PanelBlock.FACING);
        EnumFacing rayTraceDirection = result.sideHit.getOpposite();
        Vec3d hitVec;
        if (pos.equals(result.getBlockPos()))
            hitVec = result.hitVec.addVector((double)rayTraceDirection.getFrontOffsetX()*.001d,(double)rayTraceDirection.getFrontOffsetY()*.001d,(double)rayTraceDirection.getFrontOffsetZ()*.001d);
        else
            hitVec=result.hitVec;

        double relX,relY,relZ;

        if (panelFacing==EnumFacing.NORTH) {
            relX = hitVec.x - pos.getX();
            relY = hitVec.z-pos.getZ();
            relZ = 1 - (hitVec.y - pos.getY());
        }
        else if (panelFacing==EnumFacing.EAST) {
            relX = hitVec.y - pos.getY();
            relY = 1-(hitVec.x - pos.getX());
            relZ = hitVec.z - pos.getZ();
        }
        else if (panelFacing==EnumFacing.SOUTH) {
            relX = hitVec.x - pos.getX();
            relY = 1-(hitVec.z-pos.getZ());
            relZ = hitVec.y - pos.getY();
        }
        else if (panelFacing==EnumFacing.WEST) {
            relX =1-(hitVec.y - pos.getY());
            relY = hitVec.x-pos.getX();
            relZ = hitVec.z - pos.getZ();
        }
        else if (panelFacing==EnumFacing.UP) {
            relX = hitVec.x - pos.getX();
            relY = 1 - (hitVec.y - pos.getY());
            relZ = 1 - (hitVec.z - pos.getZ());
        }
        else{
            relX = hitVec.x - pos.getX();
            relZ = hitVec.z - pos.getZ();
            relY = hitVec.y - pos.getY();
        }


        if (panelTile.hasBase() && relY<.125 && relY>.0625)
            relY+=.002f;
        if (relX==1.0)relX=.99;
        if (relZ==1.0)relZ=.99;
        if (relY==1.0)relY=.99;

        PanelCellPos panelCellPos = PanelCellPos.fromCoordinates(panelTile, relX, relY,relZ);
        if(panelCellPos == null) return null;

        relX = (relX - (panelCellPos.getRow()/8d))*8d;
        relY = ((relY-1f/8f) - (panelCellPos.getLevel()/8d))*8d;
        relZ = (relZ - (panelCellPos.getColumn()/8d))*8d;

        return fromCoordinates(panelTile, panelCellPos, relX,relY,relZ);
    }
}
