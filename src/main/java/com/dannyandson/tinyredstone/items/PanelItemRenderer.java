package com.dannyandson.tinyredstone.items;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.api.IPanelCover;
import com.dannyandson.tinyredstone.blocks.PanelTileRenderer;
import com.dannyandson.tinyredstone.blocks.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

public class PanelItemRenderer extends TileEntityItemStackRenderer {

    private static final int FULL_BRIGHT = 0x00F000F0;

    @Override
    public void renderByItem(ItemStack stack) {
        TextureAtlasSprite sprite = RenderHelper.getSprite(PanelTileRenderer.TEXTURE);
        Integer color = EnumDyeColor.GRAY.getColorValue();
        if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("BlockEntityTag")) {
            NBTTagCompound blockEntityTag = stack.getTagCompound().getCompoundTag("BlockEntityTag");
            if (blockEntityTag.hasKey("color")) {
                color = blockEntityTag.getInteger("color");
            }
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0.125, 0);

        if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("BlockEntityTag")) {

            NBTTagCompound itemTag = stack.getTagCompound().getCompoundTag("BlockEntityTag");

            if (itemTag.hasKey("cover")) {
                String coverClass = stack.getTagCompound().getCompoundTag("BlockEntityTag").getString("cover");
                try {
                    IPanelCover cover = (IPanelCover) Class.forName(coverClass).getConstructor().newInstance();
                    cover.readNBT(stack.getTagCompound().getCompoundTag("BlockEntityTag").getCompoundTag("coverData"));
                    GlStateManager.pushMatrix();
                    cover.render(0f, FULL_BRIGHT, color);
                    GlStateManager.popMatrix();
                } catch (Exception exception) {
                    TinyRedstone.LOGGER.error("Exception attempting to construct IPanelCover class for item render: " + coverClass +
                            ": " + exception.getMessage() + " " + exception.getStackTrace()[0].toString());
                }
            } else {

                boolean hasBase = !itemTag.hasKey("hasBase") || itemTag.getBoolean("hasBase");

                if (hasBase)
                    renderBase(sprite,color);

                NBTTagCompound cellsNBT = itemTag.getCompoundTag("cells");
                for (Integer i = 0; i < (hasBase?448:512); i++) {
                    if (cellsNBT.hasKey(i.toString())) {
                        NBTTagCompound cellNBT = cellsNBT.getCompoundTag(i.toString());

                        if (cellNBT.hasKey("data")) {
                            String className = cellNBT.getString("class");
                            try {

                                IPanelCell cell = (IPanelCell) Class.forName(className).getConstructor().newInstance();
                                cell.readNBT(cellNBT.getCompoundTag("data"));
                                EnumFacing cellDirection = EnumFacing.getFront(cellNBT.getInteger("direction"));
                                renderCell(i, cell, cellDirection);

                            } catch (Exception exception) {
                                TinyRedstone.LOGGER.error("Exception attempting to construct IPanelCell class for item render: " + className +
                                        ": " + exception.getMessage() + " " + exception.getStackTrace()[0].toString());
                            }
                        }
                    }
                }
            }
        } else {
            renderBase(sprite,color);
        }

        GlStateManager.popMatrix();

    }

    private void renderBase(TextureAtlasSprite sprite, int color){
        GlStateManager.pushMatrix();
        GlStateManager.rotate(270, 1, 0, 0);
        GlStateManager.translate(0, -1, 0.125);
        drawRectangle(0, 1, 0, 1, sprite, color);

        GlStateManager.rotate(90, 1, 0, 0);
        GlStateManager.translate(0, -0.125, 0);
        drawRectangle(0, 1, 0, .125f, sprite, color);

        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, .125f, sprite, color);

        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, .125f, sprite, color);

        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, .125f, sprite, color);

        GlStateManager.rotate(90, 1, 0, 0);
        GlStateManager.translate(0, -1, 0);
        drawRectangle(0, 1, 0, 1, sprite, color);

        GlStateManager.popMatrix();
    }

    private void renderCell(Integer index, IPanelCell panelCell, EnumFacing cellDirection)
    {
        float scale = 0.125f;
        float t2X = 0.0f;
        float t2Y = -1.0f;
        float t2Z = 0.0f;
        float rotation1 = 270f;
        double cellSize = 1d/8d;

        int level = Math.round((index.floatValue()/64f)-0.5f);
        int row = Math.round(((index.floatValue()%64)/8f)-0.5f);
        int cell = index%8;

        GlStateManager.pushMatrix();

        GlStateManager.translate(cellSize*(double)row, 0.125+(cellSize*(double)level), cellSize*(cell));
        GlStateManager.rotate(rotation1, 1, 0, 0);

        if (cellDirection== EnumFacing.WEST)
        {
            GlStateManager.translate(0,-cellSize,0);
            GlStateManager.rotate(90, 0, 0, 1);
        }
        else if (cellDirection== EnumFacing.SOUTH)
        {
            GlStateManager.translate(cellSize,-cellSize,0);
            GlStateManager.rotate(180, 0, 0, 1);
        }
        else if (cellDirection== EnumFacing.EAST)
        {
            GlStateManager.translate(cellSize,0,0);
            GlStateManager.rotate(270, 0, 0, 1);
        }

        GlStateManager.scale(scale, scale, scale);
        GlStateManager.translate(t2X,t2Y,t2Z);

        panelCell.render(0f, FULL_BRIGHT, 1f);

        GlStateManager.popMatrix();
    }

    private void drawRectangle(float x1, float x2, float y1, float y2, TextureAtlasSprite sprite, int color)
    {
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        if (a == 0) a = 0xFF;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(x1, y1, 0).tex(u0, v0).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, 0).tex(u1, v0).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, 0).tex(u1, v1).color(r, g, b, a).endVertex();
        buffer.pos(x1, y2, 0).tex(u0, v1).color(r, g, b, a).endVertex();
        tessellator.draw();
    }


}
