package com.dannyandson.tinyredstone.blocks.panelcells;

import com.dannyandson.tinyredstone.blocks.*;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class TransparentBlock extends TinyBlock {
    public static ResourceLocation TEXTURE_TRANSPARENT_BLOCK = new ResourceLocation("minecraft","blocks/glass");
    private TextureAtlasSprite sprite;
    private int color= 16777215;

    /**
     * Drawing the cell on the panel.
     * GL matrix already positioned/scaled by PanelTileRenderer; cell draws at (0,0,0)->(1,1,1).
     */
    @Override
    public void render(float partialTicks, int combinedLight, float alpha) {

        // PORT-TODO: 1.16 looked up a per-block texture override via the dropped codec system.
        //            For v0.1 transparent blocks just use their default texture.
        if (sprite==null)
            sprite = RenderHelper.getSprite(TEXTURE_TRANSPARENT_BLOCK);

        // 1.16 mutated a transient MatrixStack copy; in immediate-mode GL we push/pop to keep
        // the renderer's matrix unchanged after the cell draws (behavior parity).
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180,0,0,1);
        GlStateManager.translate(-1, -1, 1);
        RenderHelper.drawCube(sprite,combinedLight,color,alpha-.01f);
        GlStateManager.popMatrix();
   }

    /**
     * Called when neighboring redstone signal output changes.
     * This can be called multiple times in a tick.
     * Passes PanelCellPos object for this cell which can be used to query PanelTile for PanelCellNeighbor objects - objects wrapping another IPanelCell or a BlockState
     * @param cellPos PanelCellPos object for this cell. Can be used to query paneltile about neighbors
     * @return boolean indicating whether redstone output of this cell has changed
     */
    @Override
    public boolean neighborChanged(PanelCellPos cellPos){
        return false;
    }

    /**
     * Gets redstone output of the given side of the cell
     *
     * @param outputDirection side of the panel toward which output is being queried
     * @return integer 0-15 indicating the strength of redstone signal
     */
    @Override
    public int getWeakRsOutput(Side outputDirection)
    {
        return 0;
    }
    @Override
    public int getStrongRsOutput(Side outputDirection) {
        return 0;
    }

    /**
     * Is this a component that does not change state based on neighbors (such as a redstone block, or potentiometer)?
     *
     * @return true if this cell's state is unaffected by neighbors
     */
    @Override
    public boolean isIndependentState() {
        return true;
    }

    @Override
    public NBTTagCompound writeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("color",color);
        if (this.madeFrom!=null) {
            nbt.setString("made_from_namespace", this.madeFrom.getResourceDomain());
            nbt.setString("made_from_path", this.madeFrom.getResourcePath());
        }
        return nbt;
    }

    @Override
    public void readNBT(NBTTagCompound compoundNBT) {
        this.color=compoundNBT.getInteger("color");
        if (compoundNBT.hasKey("made_from_namespace"))
            this.madeFrom = new ResourceLocation(compoundNBT.getString("made_from_namespace"), compoundNBT.getString("made_from_path").replace("block/","") );
        //TODO: ".replace("block/","")" above added for backward compatibility. Can be removed after a couple releases.
    }

}
