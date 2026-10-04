package com.dannyandson.tinyredstone.blocks;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class PanelCellVoxelShape {

    // 1.12.2 has no VoxelShape. The original wrapped two corner points (each component 0.0-1.0)
    // describing an axis-aligned cuboid. We keep the two-corner representation (now using 1.12.2's
    // net.minecraft.util.math.Vec3d, which exposes public .x/.y/.z fields just like 1.16's Vector3d)
    // so existing callers (e.g. PanelTile reading getPoint1().x) compile unchanged. A getAABB()
    // helper is provided for any code that prefers an AxisAlignedBB.
    private final Vec3d point1, point2;

    /**
     * Constructor defining a cuboid shape with 2 points
     * @param point1 vector defining point 1 of the shape (values between 0.0 and 1.0)
     * @param point2 vector defining opposite corner of the shape (values between 0.0 and 1.0)
     */
    public PanelCellVoxelShape(Vec3d point1, Vec3d point2){
        this.point1=point1;
        this.point2=point2;
    }
    public Vec3d getPoint1() {
        return point1;
    }
    public Vec3d getPoint2() {
        return point2;
    }

    /**
     * Convenience accessor returning this shape as a 1.12.2 AxisAlignedBB
     * (since 1.12.2 has no VoxelShape).
     * @return AxisAlignedBB spanning point1 -> point2
     */
    public AxisAlignedBB getAABB() {
        return new AxisAlignedBB(point1.x, point1.y, point1.z, point2.x, point2.y, point2.z);
    }

    /**
     * Tiny component that fills the entire cell
     */
    public static PanelCellVoxelShape FULLCELL = new PanelCellVoxelShape(new Vec3d(0d,0d,0d),new Vec3d(1d,1d,1d));
    public static PanelCellVoxelShape QUARTERCELLSLAB = new PanelCellVoxelShape(new Vec3d(0d,0d,0d),new Vec3d(1d,0.25d,1d));
    public static PanelCellVoxelShape BUTTONSHAPE = new PanelCellVoxelShape(new Vec3d(0.25d,0d,0.25d),new Vec3d(.75d,0.25d,.75d));
    public static PanelCellVoxelShape BUTTONSHAPE_TOP = new PanelCellVoxelShape(new Vec3d(0.25d,0.75d,0.25d),new Vec3d(.75d,1.00d,.75d));
    public static PanelCellVoxelShape BUTTONSHAPE_FRONT = new PanelCellVoxelShape(new Vec3d(0.25d,0.25d,0.25d),new Vec3d(.75d,0.75d,.75d));

}
