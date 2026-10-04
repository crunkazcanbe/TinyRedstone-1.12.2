package com.dannyandson.tinyredstone.blocks.panelcells;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.IOverlayBlockInfo;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.api.IPanelCellInfoProvider;
import com.dannyandson.tinyredstone.blocks.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;

public class Lever implements IPanelCell, IPanelCellInfoProvider {

    public static ResourceLocation TEXTURE_LEVER = new ResourceLocation(TinyRedstone.MODID,"block/lever");
    public static ResourceLocation TEXTURE_LEVER_TOP = new ResourceLocation(TinyRedstone.MODID,"block/lever_top");
    public static ResourceLocation TEXTURE_COBBLESTONE = new ResourceLocation("minecraft","blocks/cobblestone");
    private boolean active = false;
    protected Side baseSide=Side.BOTTOM;
    /**
     * Drawing the cell on the panel
     * (GL matrix already positioned/scaled for this cell by PanelTileRenderer; cell is 1.0 wide, origin (0,0,0))
     */
    @Override
    public void render(float partialTicks, int combinedLight, float alpha) {
        TextureAtlasSprite sprite_cobble = RenderHelper.getSprite(TEXTURE_COBBLESTONE);
        TextureAtlasSprite sprite_lever = RenderHelper.getSprite(TEXTURE_LEVER);
        TextureAtlasSprite sprite_lever_top = RenderHelper.getSprite(TEXTURE_LEVER_TOP);

        GlStateManager.pushMatrix();
        float x1 = 0.3125f, x2 = .6875f, y1 = 0.25f, y2 = 0.75f;
        float w = .375f, d = 0.5f,h=0.1875f;

        if (baseSide==Side.FRONT) {
            GlStateManager.rotate(90, 1, 0, 0);
            GlStateManager.translate(0,0,h-1);
        }
        else if (baseSide==Side.TOP) {
            GlStateManager.rotate(180, 1, 0, 0);
            GlStateManager.translate(0,-1,h-1);
        }
        else
            GlStateManager.translate(0,0,h);
        RenderHelper.drawRectangle(x1,x2,y1,y2,sprite_cobble,combinedLight,alpha);

        GlStateManager.rotate(90, 1, 0, 0);
        GlStateManager.translate(0,-h,-y1);
        RenderHelper.drawRectangle(x1,x2,0,h,sprite_cobble,combinedLight,alpha);

        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0,0,1f-x1);
        RenderHelper.drawRectangle(0,d,0,h,sprite_cobble,combinedLight,alpha);

        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0,0,d);
        RenderHelper.drawRectangle(0,w,0,h,sprite_cobble,combinedLight,alpha);

        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0,0,w);
        RenderHelper.drawRectangle(0,d,0,h,sprite_cobble,combinedLight,alpha);

        GlStateManager.rotate(60, 1, 0, 0);
        GlStateManager.translate(0,0.03125f,0);

        GlStateManager.popMatrix();

        x1 = 0.4375f;
        x2 = 0.5625f;
        y1 = 0;
        y2 = .625f;

        if (baseSide==Side.FRONT) {
            GlStateManager.rotate(90, 1, 0, 0);
            GlStateManager.translate(0,0.40625,(h/2f)-1);
        }
        else if (baseSide==Side.TOP) {
            GlStateManager.rotate(180, 1, 0, 0);
            GlStateManager.translate(0,0.40625-1,(h/2f)-1);
        }
        else
            GlStateManager.translate(0,0.40625,h/2f);
        GlStateManager.rotate((active)?45:135, 1, 0, 0);

        RenderHelper.drawRectangle(x1,x2,y1,y2,sprite_lever,combinedLight,alpha);

        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(-x1,0,x2);
        RenderHelper.drawRectangle(x1,x2,y1,y2,sprite_lever,combinedLight,alpha);

        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(-x1,0,x2);
        RenderHelper.drawRectangle(x1,x2,y1,y2,sprite_lever,combinedLight,alpha);

        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(-x1,0,x2);
        RenderHelper.drawRectangle(x1,x2,y1,y2,sprite_lever,combinedLight,alpha);

        GlStateManager.rotate(-90, 1, 0, 0);
        GlStateManager.translate(0,-x1,y2);
        RenderHelper.drawRectangle(x1,x2,x1,x2,sprite_lever_top,combinedLight,alpha);


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
     * @param outputDirection (1=Front,2=Right,3=Back,4=Left)
     * @return integer 0-15 indicating the strength of redstone signal
     */
    @Override
    public int getWeakRsOutput(Side outputDirection) {
        return (active && outputDirection!=baseSide.getOpposite())?15:0;
    }

    @Override
    public int getStrongRsOutput(Side outputDirection) {
        return (active && outputDirection==baseSide)?15:0;
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
    public boolean needsSolidBase(){return true;}

    @Override
    public boolean canAttachToBaseOnSide(Side side) {
        return true;
    }

    @Override
    public Side getBaseSide(){return this.baseSide;}

    @Override
    public void setBaseSide(Side side){this.baseSide=side;}

    /**
     * Called when the cell is activated. i.e. player right clicked on the cell of the panel tile.
     *
     * @param cellPos The position of the clicked IPanelCell within the panel (this IPanelCell)
     * @param segmentClicked Which of nine segment within the cell were clicked.
     * @param player
     * @return true if a change was made to the cell output
     */
    @Override
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, EntityPlayer player) {
        PanelTile panelTile = cellPos.getPanelTile();
        panelTile.getLevel().playSound(
                panelTile.getPos().getX(), panelTile.getPos().getY(), panelTile.getPos().getZ(),
                SoundEvents.BLOCK_LEVER_CLICK,
                SoundCategory.BLOCKS, 0.25f, 2f, false
        );
        this.active=!this.active;
        return true;
    }

    @Override
    public boolean hasActivation(){return true;}

    @Override
    public NBTTagCompound writeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("active",this.active);
        nbt.setString("baseSide",baseSide.name());
        return nbt;
    }

    @Override
    public void readNBT(NBTTagCompound compoundNBT) {
        this.active=compoundNBT.getBoolean("active");
        if (compoundNBT.getString("baseSide").length()>0)
            this.baseSide=Side.valueOf(compoundNBT.getString("baseSide"));
        else
            baseSide=Side.BOTTOM;
    }

    @Override
    public void addInfo(IOverlayBlockInfo overlayBlockInfo, PanelTile panelTile, PosInPanelCell pos) {
        overlayBlockInfo.addText("State", this.active ? "On" : "Off");
        overlayBlockInfo.setPowerOutput(0);
    }

    @Override
    public PanelCellVoxelShape getShape()
    {
        if (baseSide==Side.BOTTOM)
            return PanelCellVoxelShape.BUTTONSHAPE;
        if (baseSide==Side.TOP)
            return PanelCellVoxelShape.BUTTONSHAPE_TOP;
        return PanelCellVoxelShape.BUTTONSHAPE_FRONT;
    }
}
