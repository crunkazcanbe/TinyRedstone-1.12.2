package com.dannyandson.tinyredstone.blocks.panelcovers;

import com.dannyandson.tinyredstone.api.IPanelCover;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

public class TrimCover implements IPanelCover {

    private AxisAlignedBB shape = null;
    private Float xMin = null, xMax = null, yMin = null, yMax = null, zMin = null, zMax = null;
    private TextureAtlasSprite sprite;

    @Override
    public void onPlace(PanelTile panelTile, EntityPlayer player) {
        List<PanelCellPos> cellPosList = panelTile.getCellPositions();
        if (cellPosList.size() == 0) {
            setDefaultBoundaries();
        } else {
            Integer xMin = null, xMax = null, yMin = null, yMax = null, zMin = null, zMax = null;
            boolean hasBase = panelTile.hasBase();
            for (PanelCellPos cellPos : cellPosList) {
                if (xMin == null || cellPos.getRow() < xMin)
                    xMin = cellPos.getRow();
                if (xMax == null || cellPos.getRow() > xMax)
                    xMax = cellPos.getRow();

                int y = !hasBase?cellPos.getLevel(): (cellPos.getLevel() == 0) ? 0 : cellPos.getLevel() + 1;
                if (yMin == null || y < yMin)
                    yMin = y;
                y = !hasBase?cellPos.getLevel():cellPos.getLevel() + 1;
                if (yMax == null || y > yMax)
                    yMax = y;

                if (zMin == null || cellPos.getColumn() < zMin)
                    zMin = cellPos.getColumn();
                if (zMax == null || cellPos.getColumn() > zMax)
                    zMax = cellPos.getColumn();
            }
            this.xMin = xMin / 8f;
            this.xMax = (xMax + 1) / 8f;
            this.yMin = yMin / 8f;
            this.yMax = (yMax + 1) / 8f;
            this.zMin = zMin / 8f;
            this.zMax = (zMax + 1) / 8f;
        }
    }

     /**
     * Drawing the cover on the panel
     */

    @Override
    public void render(float partialTicks, int combinedLight, int color) {
        if (xMin ==null)setDefaultBoundaries();
        if (sprite==null)sprite = RenderHelper.getSprite(DarkCover.TEXTURE_DEFAULT_COVER);

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.pushMatrix();

        //south face (+z)
        GlStateManager.translate(0, 0, zMax);
        DarkCover.drawRectangle(xMin,xMax,yMin,yMax,sprite,color,1f);

        //east face (+x)
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0,0,xMax);
        DarkCover.drawRectangle(0,zMax-zMin,yMin,yMax,sprite,color,1f);

        //north face (-z)
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0,0,zMax-zMin);
        DarkCover.drawRectangle(0,xMax-xMin,0,yMax-yMin,sprite,color,1f);

        //west face (-x)
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0,0,xMax-xMin);
        DarkCover.drawRectangle(0,zMax-zMin,0,yMax-yMin,sprite,color,1f);

        //bottom face (-y)
        GlStateManager.rotate(90, 1, 0, 0);
        GlStateManager.translate(0,xMin-xMax,0);
        DarkCover.drawRectangle(0,zMax-zMin,0,xMax-xMin,sprite,color,1f);

        //top face (+y)
        GlStateManager.rotate(180, 1, 0, 0);
        GlStateManager.translate(0,xMin-xMax,yMax-yMin);
        DarkCover.drawRectangle(0,zMax-zMin,0,xMax-xMin,sprite,color,1f);

        GlStateManager.popMatrix();
    }

    /**
     * Does this cover allows light output?
     *
     * @return true if cells can output light, false if not.
     */
    @Override
    public boolean allowsLightOutput() {
        return false;
    }

    @Override
    public NBTTagCompound writeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setFloat("xMin", xMin);
        tag.setFloat("xMax", xMax);
        tag.setFloat("yMin", yMin);
        tag.setFloat("yMax", yMax);
        tag.setFloat("zMin", zMin);
        tag.setFloat("zMax", zMax);
        return tag;
    }

    @Override
    public void readNBT(NBTTagCompound compoundNBT) {
        xMin = compoundNBT.getFloat("xMin");
        xMax = compoundNBT.getFloat("xMax");
        yMin = compoundNBT.getFloat("yMin");
        yMax = compoundNBT.getFloat("yMax");
        zMin = compoundNBT.getFloat("zMin");
        zMax = compoundNBT.getFloat("zMax");
    }

    public void setDefaultBoundaries(){
        xMin = 0f;
        xMax = 1f;
        yMin = 0f;
        yMax = 1f;
        zMin = 0f;
        zMax = 1f;
    }

    /**
     * Get the shape of the cover for defining the hit box
     *
     * @return AxisAlignedBB object defining the shape (1.12.2 has no VoxelShape)
     */
    @Override
    public AxisAlignedBB getShape() {
        if (this.shape==null){
            if (xMin ==null)setDefaultBoundaries();
            this.shape = new AxisAlignedBB(xMin, yMin, zMin, xMax, yMax, zMax);
        }
        return this.shape;
    }
}
