package com.dannyandson.tinyredstone.blocks.panelcells;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.IColorablePanelCell;
import com.dannyandson.tinyredstone.api.IOverlayBlockInfo;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.api.IPanelCellInfoProvider;
import com.dannyandson.tinyredstone.blocks.*;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class TinyBlock implements IPanelCell, IColorablePanelCell, IPanelCellInfoProvider {

    public static final ResourceLocation TEXTURE_GRASS_BLOCK_TOP = new ResourceLocation(TinyRedstone.MODID, "block/grass_block_top");
    public static ResourceLocation TEXTURE_TINY_BLOCK = new ResourceLocation("minecraft","blocks/wool_colored_white");

    protected int weakSignalStrength = 0;
    protected int strongSignalStrength = 0;
    protected int color= EnumDyeColor.WHITE.getColorValue();
    protected ResourceLocation madeFrom;
    protected TextureAtlasSprite sprite_top, sprite_front, sprite_right, sprite_back, sprite_left, sprite_bottom;

    /**
     * Drawing the cell on the panel.
     * GL matrix already positioned/scaled by PanelTileRenderer; cell draws at (0,0,0)->(1,1,1).
     */
    @Override
    public void render(float partialTicks, int combinedLight, float alpha) {
        if (sprite_top==null) {
            // PORT-TODO: 1.16 looked up a per-block texture override via the dropped codec system.
            //            For v0.1 tiny blocks just use their default texture.
            sprite_top=sprite_front=sprite_right=sprite_back=sprite_left=sprite_bottom=RenderHelper.getSprite(TEXTURE_TINY_BLOCK);
        }

        // 1.16 mutated a transient MatrixStack copy; in immediate-mode GL we push/pop to keep
        // the renderer's matrix unchanged after the cell draws (behavior parity).
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180,0,0,1);
        GlStateManager.translate(-1, -1, 1);
        RenderHelper.drawCube(sprite_top, sprite_front, sprite_right, sprite_back, sprite_left, sprite_bottom,combinedLight,color,alpha);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean onPlace(PanelCellPos cellPos, EntityPlayer player) {
        ItemStack stack = ItemStack.EMPTY;
        // PORT-TODO: getActiveHand()/getHeldItem()/getHeldItemMainhand() are the 1.12.2 names for
        // the 1.16 getUsedItemHand()/getItemInHand()/getMainHandItem().
        if (player.getActiveHand() != null)
            stack = player.getHeldItem(player.getActiveHand());
        if (stack == ItemStack.EMPTY)
            stack = player.getHeldItemMainhand();
        if (stack.hasTagCompound()) {
            NBTTagCompound itemNBT = stack.getTagCompound();
            NBTTagCompound madeFromTag = itemNBT.getCompoundTag("made_from");
            if (madeFromTag.hasKey("namespace")) {
                this.madeFrom = new ResourceLocation(madeFromTag.getString("namespace"), madeFromTag.getString("path"));
            }
        }

        return IPanelCell.super.onPlace(cellPos, player);
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

        PanelCellNeighbor rightNeighbor = cellPos.getNeighbor(Side.RIGHT),
                leftNeighbor = cellPos.getNeighbor(Side.LEFT),
                backNeighbor = cellPos.getNeighbor(Side.BACK),
                frontNeighbor = cellPos.getNeighbor(Side.FRONT),
                topNeighbor = cellPos.getNeighbor(Side.TOP),
                bottomNeighbor = cellPos.getNeighbor(Side.BOTTOM);

        int weak=0,strong=0;
        if (frontNeighbor!=null) {
            if (frontNeighbor.powerDrops())
                weak = frontNeighbor.getWeakRsOutput();
            else if (!(frontNeighbor.getNeighborIPanelCell() instanceof TinyBlock))
                strong = frontNeighbor.getStrongRsOutput();
        }
        if (rightNeighbor!=null) {
            if (rightNeighbor.powerDrops())
                weak = Math.max(weak,rightNeighbor.getWeakRsOutput());
            else if (!(rightNeighbor.getNeighborIPanelCell() instanceof TinyBlock))
                strong = Math.max(strong,rightNeighbor.getStrongRsOutput());
        }
        if (backNeighbor!=null) {
            if (backNeighbor.powerDrops())
                weak = Math.max(weak,backNeighbor.getWeakRsOutput());
            else if (!(backNeighbor.getNeighborIPanelCell() instanceof TinyBlock))
                strong = Math.max(strong,backNeighbor.getStrongRsOutput());
        }
        if (leftNeighbor!=null) {
            if (leftNeighbor.powerDrops())
                weak = Math.max(weak,leftNeighbor.getWeakRsOutput());
            else if (!(leftNeighbor.getNeighborIPanelCell() instanceof TinyBlock))
                strong = Math.max(strong,leftNeighbor.getStrongRsOutput());
        }
        if (topNeighbor!=null) {
            if (topNeighbor.powerDrops())
                weak = Math.max(weak,topNeighbor.getWeakRsOutput());
            else if (!(topNeighbor.getNeighborIPanelCell() instanceof TinyBlock))
                strong = Math.max(strong,topNeighbor.getStrongRsOutput());
        }
        if (bottomNeighbor!=null) {
            if (bottomNeighbor.powerDrops())
                weak = Math.max(weak,bottomNeighbor.getWeakRsOutput());
            else if (!(bottomNeighbor.getNeighborIPanelCell() instanceof TinyBlock))
                strong = Math.max(strong,bottomNeighbor.getStrongRsOutput());
        }

        weak=Math.max(weak,strong);

        if (weak!=this.weakSignalStrength ||strong!=this.strongSignalStrength)
        {
            this.weakSignalStrength =weak;
            this.strongSignalStrength=strong;
            return true;
        }

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
        return this.weakSignalStrength;
    }

    @Override
    public int getStrongRsOutput(Side outputDirection) {
        return this.strongSignalStrength;
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
    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public NBTTagCompound writeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("strong",this.strongSignalStrength);
        nbt.setInteger("weak",this.weakSignalStrength);
        nbt.setInteger("color",this.color);
        if (this.madeFrom!=null) {
            nbt.setString("made_from_namespace",this.madeFrom.getResourceDomain());
            nbt.setString("made_from_path",this.madeFrom.getResourcePath());
        }
        return nbt;
    }

    @Override
    public void readNBT(NBTTagCompound compoundNBT) {
        this.strongSignalStrength=compoundNBT.getInteger("strong");
        this.weakSignalStrength=compoundNBT.getInteger("weak");
        this.color=compoundNBT.getInteger("color");
        if (compoundNBT.hasKey("made_from_namespace"))
            this.madeFrom=new ResourceLocation(compoundNBT.getString("made_from_namespace"),compoundNBT.getString("made_from_path"));
    }

    @Override
    public void addInfo(IOverlayBlockInfo overlayBlockInfo, PanelTile panelTile, PosInPanelCell pos) {
        overlayBlockInfo.setPowerOutput(this.weakSignalStrength);
    }

    @Override
    public NBTTagCompound getItemTag() {
        if (this.madeFrom != null) {
            NBTTagCompound madeFromTag = new NBTTagCompound();
            madeFromTag.setString("namespace", this.madeFrom.getResourceDomain());
            madeFromTag.setString("path", this.madeFrom.getResourcePath());
            NBTTagCompound itemTag = new NBTTagCompound();
            itemTag.setTag("made_from", madeFromTag);
            return itemTag;
        }
        return null;
    }
}
