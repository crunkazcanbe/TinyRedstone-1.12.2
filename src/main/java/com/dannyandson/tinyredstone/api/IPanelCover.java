package com.dannyandson.tinyredstone.api;

import com.dannyandson.tinyredstone.blocks.PanelTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

public interface IPanelCover {

    default void onPlace(PanelTile panelTile, EntityPlayer player){}

    /**
     * Drawing the cover on the panel.
     * Called from the panel's TileEntitySpecialRenderer which has already pushed/translated/scaled
     * the matrix so the cover draws at (0,0,0)->(1,1,1).
     */
    void render(float partialTicks, int combinedLight, int color);

    /**
     * Does this cover allows light output?
     * @return true if cells can output light, false if not.
     */
    boolean allowsLightOutput();

    /**
     * Get the shape of the cover for defining the hit box
     * @return AxisAlignedBB object defining the shape (1.12.2 has no VoxelShape)
     */
    default AxisAlignedBB getShape() {return new AxisAlignedBB(0, 0, 0, 1, 1, 1);}

    default NBTTagCompound writeNBT(){return null;}

    default void readNBT(NBTTagCompound compoundNBT){}

    /**
     * Gets Compound tag with any NBT data the itemStack for this cover should contain
     * This is used when the item is removed and the itemStack provided to player, also
     * for pick block and info overlays like The One Probe
     * @return CompoundTag with item NBT data
     */
    default NBTTagCompound getItemTag(){return null;}

}
