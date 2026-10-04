package com.dannyandson.tinyredstone.blocks.panelcells;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelCellSegment;
import com.dannyandson.tinyredstone.blocks.RenderHelper;
import com.dannyandson.tinyredstone.blocks.Side;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GhostRenderer implements IPanelCell {

    PanelCellSegment segmentHovering;
    //pre-calculated variables for segment points
    private static final float s6 = 0.375f;
    private static final float s10 = 0.625f;
    // 0xRRGGBB (RenderHelper uses only the low 24 bits as color; alpha is passed separately)
    private static final int color = (80 << 16) | (0 << 8) | 0;
    public static ResourceLocation TEXTURE_REDSTONE_DUST_SEGMENT_GHOST = new ResourceLocation(TinyRedstone.MODID,"block/panel_redstone_segment_ghost");
    private static TextureAtlasSprite sprite;

    public GhostRenderer(PanelCellSegment segmentHovering){
        this.segmentHovering=segmentHovering;
    }

    @Override
    public void render(float partialTicks, int combinedLight, float alpha) {
        if (sprite==null){
            sprite=RenderHelper.getSprite(TEXTURE_REDSTONE_DUST_SEGMENT_GHOST);
        }
        alpha = .75f;

        // 1.16 mutated a transient MatrixStack copy; in immediate-mode GL we push/pop to keep
        // the renderer's matrix unchanged after the cell draws (behavior parity).
        GlStateManager.pushMatrix();
        GlStateManager.translate(0,0,0.015);

        if (segmentHovering==PanelCellSegment.RIGHT || segmentHovering==PanelCellSegment.CENTER) {
            RenderHelper.drawRectangle(s10,1.01f,0,1,sprite,combinedLight,color,alpha);
        }
        if (segmentHovering==PanelCellSegment.LEFT || segmentHovering==PanelCellSegment.CENTER) {
            RenderHelper.drawRectangle(-.01f,s6,0,1,sprite,combinedLight,color,alpha);
        }
        GlStateManager.rotate(90,0,0,1);
        GlStateManager.translate(0,-1,0);
        if (segmentHovering==PanelCellSegment.FRONT || segmentHovering==PanelCellSegment.CENTER) {
            RenderHelper.drawRectangle(s10,1.01f,0,1,sprite,combinedLight,color,alpha);
        }
        if (segmentHovering==PanelCellSegment.BACK || segmentHovering==PanelCellSegment.CENTER) {
            RenderHelper.drawRectangle(-.01f,s6,0,1,sprite,combinedLight,color,alpha);
        }
        GlStateManager.popMatrix();
    }

    @Override
    public boolean neighborChanged(PanelCellPos cellPos) {
        return false;
    }

    @Override
    public int getWeakRsOutput(Side outputDirection) {
        return 0;
    }

    @Override
    public int getStrongRsOutput(Side outputDirection) {
        return 0;
    }

    @Override
    public NBTTagCompound writeNBT() {
        return new NBTTagCompound();
    }

    @Override
    public void readNBT(NBTTagCompound compoundNBT) {
    }
}
