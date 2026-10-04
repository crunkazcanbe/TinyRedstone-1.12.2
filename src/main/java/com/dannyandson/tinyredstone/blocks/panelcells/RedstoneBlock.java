package com.dannyandson.tinyredstone.blocks.panelcells;

import com.dannyandson.tinyredstone.api.IOverlayBlockInfo;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.api.IPanelCellInfoProvider;
import com.dannyandson.tinyredstone.blocks.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class RedstoneBlock  implements IPanelCell, IPanelCellInfoProvider {
    public static ResourceLocation TEXTURE_REDSTONE_BLOCK = new ResourceLocation("minecraft","blocks/redstone_block");

    /**
     * Drawing the cell on the panel
     *
     * @param matrixStack     positioned for this cell
     *                        scaled to 1/8 block size such that length and width of cell are 1.0
     *                        starting point is (0,0,0)
     */
    @Override
    public void render(float partialTicks, int combinedLight, float alpha) {
        TextureAtlasSprite sprite = RenderHelper.getSprite(TEXTURE_REDSTONE_BLOCK);

        GlStateManager.translate(0,0,1.0);
        RenderHelper.drawRectangle(0,1,0,1,sprite,combinedLight,alpha);

        GlStateManager.rotate(90,1,0,0);
        GlStateManager.translate(0,-1,0);
        RenderHelper.drawRectangle(0,1,0,1,sprite,combinedLight,alpha);

        GlStateManager.rotate(90,0,1,0);
        GlStateManager.translate(0,0,1);
        RenderHelper.drawRectangle(0,1,0,1,sprite,combinedLight,alpha);

        GlStateManager.rotate(90,0,1,0);
        GlStateManager.translate(0,0,1);
        RenderHelper.drawRectangle(0,1,0,1,sprite,combinedLight,alpha);

        GlStateManager.rotate(90,0,1,0);
        GlStateManager.translate(0,0,1);
        RenderHelper.drawRectangle(0,1,0,1,sprite,combinedLight,alpha);

        GlStateManager.rotate(90,1,0,0);
        GlStateManager.translate(0,-1,0);
        RenderHelper.drawRectangle(0,1,0,1,sprite,combinedLight,alpha);

    }

    @Override
    public boolean onPlace(PanelCellPos cellPos, EntityPlayer player) {
        return true;
    }

    /**
     * Called when neighboring redstone signal output changes.
     * This can be called multiple times in a tick.
     * Passes PanelCellPos object for this cell which can be used to query PanelTile for PanelCellNeighbor objects - objects wrapping another IPanelCell or a BlockState
     * @param cellPos PanelCellPos object for this cell. Can be used to query paneltile about neighbors
     * @return boolean indicating whether redstone output of this cell has changed
     */
    @Override
    public boolean neighborChanged(PanelCellPos cellPos)
    {
        return false;
    }

    /**
     * Gets redstone output of the given side of the cell
     *
     * @return integer 0-15 indicating the strength of redstone signal
     */
    @Override
    public int getWeakRsOutput(Side outputDirection)
    {
        return getStrongRsOutput(outputDirection);
    }
    @Override
    public int getStrongRsOutput(Side outputDirection) {
        return 15;
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

    /**
     * Can this cell be pushed by a piston?
     *
     * @return true if a piston can push this block
     */
    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public NBTTagCompound writeNBT() {
        return new NBTTagCompound();
    }

    @Override
    public void readNBT(NBTTagCompound compoundNBT) {

    }

    @Override
    public void addInfo(IOverlayBlockInfo overlayBlockInfo, PanelTile panelTile, PosInPanelCell pos) {
        overlayBlockInfo.setPowerOutput(15);
    }
}
