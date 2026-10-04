package com.dannyandson.tinyredstone.blocks.panelcells;

import com.dannyandson.tinyredstone.Config;
import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.IOverlayBlockInfo;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.api.IPanelCellInfoProvider;
import com.dannyandson.tinyredstone.blocks.*;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class Repeater implements IPanelCell, IPanelCellInfoProvider {
    protected boolean input = false;
    protected boolean output = false;
    protected boolean locked = false;
    private int onPending = -1;
    private int offPending = -1;
    protected Integer ticks = 2;
    private int changedTick = -1;

    public static ResourceLocation TEXTURE_REPEATER_ON = new ResourceLocation(TinyRedstone.MODID,"block/panel_repeater_on");
    public static ResourceLocation TEXTURE_REPEATER_OFF = new ResourceLocation(TinyRedstone.MODID,"block/panel_repeater_off");

    /**
     * Drawing the cell on the panel
     * (GL matrix already positioned/scaled for this cell by PanelTileRenderer; cell is 1.0 wide, origin (0,0,0))
     */
    @Override
    public void render(float partialTicks, int combinedLight, float alpha) {
        TextureAtlasSprite sprite = RenderHelper.getSprite(PanelTileRenderer.TEXTURE);
        TextureAtlasSprite sprite_repeater = this.getRepeaterTexture();
        TextureAtlasSprite sprite_torch_head = RenderHelper.getSprite(RedstoneDust.TEXTURE_REDSTONE_DUST_SEGMENT_ON);


        if (!this.output){
            sprite_torch_head = RenderHelper.getSprite(RedstoneDust.TEXTURE_REDSTONE_DUST_SEGMENT_OFF);
        }

        if (locked)
            sprite_torch_head=RenderHelper.getSprite(new ResourceLocation("minecraft","blocks/bedrock"));


        GlStateManager.translate(0,0,0.25);

        //draw base top
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180, 0, 0, 1);
        GlStateManager.translate(-1,-1,0);
        RenderHelper.drawRectangle(0,1,0,1,sprite_repeater,combinedLight,alpha);
        GlStateManager.popMatrix();


        if (ticks>8) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 0.01);
            RenderHelper.drawRectangle(0.25f,0.75f,0.125f,0.25f,sprite_torch_head,combinedLight,alpha);
            GlStateManager.popMatrix();
        }

        GlStateManager.pushMatrix();
        //draw static torch top
        GlStateManager.translate(0,0,0.125);
        RenderHelper.drawRectangle(0.4375f,0.5625f,0.75f,0.875f,sprite_torch_head,combinedLight,alpha);

        //draw moving torch top
        float torch2Y = (ticks<8)? 0.75f - ticks.floatValue()*0.0625f : 0.25f;
        RenderHelper.drawRectangle(0.4375f,0.5625f,torch2Y-0.125f,torch2Y,sprite_torch_head,combinedLight,alpha);

        GlStateManager.popMatrix();

        //draw back side
        GlStateManager.rotate(90, 1, 0, 0);
        GlStateManager.translate(0,-0.25,0);
        RenderHelper.drawRectangle(0,1,0,0.25f,sprite,combinedLight,alpha);

        GlStateManager.pushMatrix();
        //draw static torch side
        GlStateManager.translate(.4375f,.25,-0.75f);
        RenderHelper.drawRectangle(0,0.125f,0,0.125f,sprite_torch_head,combinedLight,alpha);

        //draw moving torch side
        GlStateManager.translate(0,0,.875-torch2Y);
        RenderHelper.drawRectangle(0,0.125f,0,0.125f,sprite_torch_head,combinedLight,alpha);

        GlStateManager.popMatrix();

        //right side
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0,0,1);
        RenderHelper.drawRectangle(0,1,0,0.25f,sprite,combinedLight,alpha);

        GlStateManager.pushMatrix();
        //draw static torch side
        GlStateManager.translate(.75,0.25f,-.4375f);
        RenderHelper.drawRectangle(0,0.125f,0,0.125f,sprite_torch_head,combinedLight,alpha);

        //draw moving torch side
        GlStateManager.translate(torch2Y-.875,0,0);
        RenderHelper.drawRectangle(0,0.125f,0,0.125f,sprite_torch_head,combinedLight,alpha);

        GlStateManager.popMatrix();

        //front side
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0,0,1);
        RenderHelper.drawRectangle(0,1,0,0.25f,sprite,combinedLight,alpha);
        GlStateManager.pushMatrix();
        //draw static torch front
        GlStateManager.translate(.4375f,.25,-0.125f);
        RenderHelper.drawRectangle(0,0.125f,0,0.125f,sprite_torch_head,combinedLight,alpha);

        //draw moving torch front
        GlStateManager.translate(0,0,torch2Y-.875);
        RenderHelper.drawRectangle(0,0.125f,0,0.125f,sprite_torch_head,combinedLight,alpha);

        GlStateManager.popMatrix();


        //left side
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0,0,1);
        RenderHelper.drawRectangle(0,1,0,0.25f,sprite,combinedLight,alpha);

        GlStateManager.pushMatrix();
        //draw static torch side
        GlStateManager.translate(.125,0.25f,-.4375f);
        RenderHelper.drawRectangle(0,0.125f,0,0.125f,sprite_torch_head,combinedLight,alpha);

        //draw moving torch side
        GlStateManager.translate(.875-torch2Y,0,0);
        RenderHelper.drawRectangle(0,0.125f,0,0.125f,sprite_torch_head,combinedLight,alpha);

        GlStateManager.popMatrix();


    }

    protected TextureAtlasSprite getRepeaterTexture()
    {
        if (this.output)
            return RenderHelper.getSprite(TEXTURE_REPEATER_ON);
        return RenderHelper.getSprite(TEXTURE_REPEATER_OFF);

    }

    /**
     * Called when neighboring redstone signal output changes.
     * This can be called multiple times in a tick.
     * Passes PanelCellPos object for this cell which can be used to query PanelTile for PanelCellNeighbor objects - objects wrapping another IPanelCell or a BlockState
     * @param cellPos PanelCellPos object for this cell. Can be used to query paneltile about neighbors
     * @return boolean indicating whether redstone output of this cell has changed
     */
    @Override
    public boolean neighborChanged(PanelCellPos cellPos) {
        if (this.ticks==0)
            checkInputs(cellPos);
        else
            this.changedTick = cellPos.getPanelTile().getRelTickTime();
        return false;
    }

    private void checkInputs(PanelCellPos cellPos){

        PanelCellNeighbor rightNeighbor = cellPos.getNeighbor(Side.RIGHT),
                leftNeighbor = cellPos.getNeighbor(Side.LEFT),
                backNeighbor = cellPos.getNeighbor(Side.BACK);

        if (backNeighbor!=null && backNeighbor.getWeakRsOutput() >0 && !input)
        {
            //input has switched from off to on
            input=true;
            if(offPending==1)
            {
                onPending=ticks;
            }
        }
        else if ((backNeighbor==null || backNeighbor.getWeakRsOutput() ==0 ) && input)
        {
            input=false;
        }

        boolean wasLocked = this.locked;
        // 1.16 Blocks.REPEATER/Blocks.COMPARATOR are single blocks (powered state is a blockstate property);
        // 1.12.2 splits each into UNPOWERED_/POWERED_ block instances, so match both variants here.
        this.locked= (leftNeighbor != null && leftNeighbor.getStrongRsOutput() > 0 &&
                (leftNeighbor.getNeighborIPanelCell() instanceof Repeater || leftNeighbor.getNeighborIPanelCell() instanceof Comparator ||
                        (leftNeighbor.getNeighborBlockState() != null && isVanillaRepeaterOrComparator(leftNeighbor.getNeighborBlockState().getBlock()))
                ))
                ||
                (rightNeighbor != null && rightNeighbor.getStrongRsOutput() > 0 &&
                        (rightNeighbor.getNeighborIPanelCell() instanceof Repeater || rightNeighbor.getNeighborIPanelCell() instanceof Comparator ||
                                (rightNeighbor.getNeighborBlockState() != null && isVanillaRepeaterOrComparator(rightNeighbor.getNeighborBlockState().getBlock()))
                        ));
        if (wasLocked && !locked && output!=input)
            if (input)onPending=ticks;
            else offPending=ticks;

    }

    private static boolean isVanillaRepeaterOrComparator(Block block) {
        return block == Blocks.UNPOWERED_REPEATER || block == Blocks.POWERED_REPEATER
                || block == Blocks.UNPOWERED_COMPARATOR || block == Blocks.POWERED_COMPARATOR;
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
        if (output&&outputDirection== Side.FRONT)
            return 15;
        else
            return 0;
    }

    /**
     * Can this cell be pushed by a piston?
     *
     * @return true if a piston can push this block
     */
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean needsSolidBase(){return true;}

    /**
     * Called each tick.
     *
     * @param cellPos The PanelCellPos of this IPanelCell
     * @return boolean indicating whether redstone output of this cell has changed
     */
    @Override
    public boolean tick(PanelCellPos cellPos) {

        if (this.changedTick>-1 && this.changedTick<cellPos.getPanelTile().getRelTickTime()){
            this.checkInputs(cellPos);
            this.changedTick=-1;
        }

        if (this.input!=this.output){
            if (this.input && this.onPending==-1)
            {
                this.onPending=this.ticks;
            }
            if (!this.input && this.offPending==-1){
                this.offPending=this.ticks;
            }
        }

        if (this.onPending>=0)
            onPending--;
        if (this.offPending>=0)
            offPending--;

        boolean change = false;

        if (this.onPending == 0 && !this.locked) {
            this.output = true;
            if (this.input)
                this.offPending=-1;
            else
                this.offPending=this.ticks;
            change = true;
        }else if (this.offPending==0 && !this.locked && (!input||(this.output && this.onPending>-1)) ){
            if ((this.output && this.onPending>-1))this.onPending=this.ticks;
            this.output=false;
            change = true;
        }

        if (this.changedTick>-1){
            this.checkInputs(cellPos);
            this.changedTick=-1;
        }
        return change;
    }

    /**
     * Called when the cell is activated. i.e. player right clicked on the cell of the panel tile.
     *
     * @param cellPos The position of the clicked IPanelCell within the panel (this IPanelCell)
     * @param segmentClicked Which of nine segment within the cell were clicked.
     * @param player player who activated (right-clicked) the cell
     * @return true if a change was made to the cell output
     */
    @Override
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, EntityPlayer player) {
        if (ticks<8)
        {
            ticks+=2;
            return true;
        }
        else
            ticks=2;
        return false;
    }

    @Override
    public boolean hasActivation(){return true;}

    @Override
    public NBTTagCompound writeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("output",output);
        nbt.setBoolean("input",input);
        nbt.setBoolean("locked",locked);
        nbt.setInteger("offPending",this.offPending);
        nbt.setInteger("onPending",this.onPending);
        nbt.setInteger("changedTick",this.changedTick);

        nbt.setInteger("ticks",this.ticks);

        return nbt;
    }

    @Override
    public void readNBT(NBTTagCompound compoundNBT) {
        this.output = compoundNBT.getBoolean("output");
        this.input = compoundNBT.getBoolean("input");
        this.locked = compoundNBT.getBoolean("locked");
        this.ticks = compoundNBT.getInteger("ticks");
        this.offPending=compoundNBT.getInteger("offPending");
        this.onPending=compoundNBT.getInteger("onPending");
        this.changedTick=compoundNBT.getInteger("changedTick");

    }

    public Integer getTicks() {
        return this.ticks;
    }
    public void setTicks(Integer ticks){
        if (ticks<0)this.ticks=0;
        else if(ticks>Config.SUPER_REPEATER_MAX*2)this.ticks=Config.SUPER_REPEATER_MAX*2;
        else this.ticks=ticks;
    }

    @Override
    public void addInfo(IOverlayBlockInfo overlayBlockInfo, PanelTile panelTile, PosInPanelCell pos) {
        overlayBlockInfo.addText("Delay", this.ticks/2 + " ticks");
        if(this.locked) {
            overlayBlockInfo.addInfo("Locked");
        }
    }

    @Override
    public PanelCellVoxelShape getShape()
    {
        return PanelCellVoxelShape.QUARTERCELLSLAB;
    }
}
