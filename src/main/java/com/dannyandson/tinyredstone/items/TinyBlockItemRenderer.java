package com.dannyandson.tinyredstone.items;

import com.dannyandson.tinyredstone.blocks.RenderHelper;
import com.dannyandson.tinyredstone.blocks.Side;
import com.dannyandson.tinyredstone.blocks.panelcells.TinyBlock;
import com.dannyandson.tinyredstone.blocks.panelcells.TransparentBlock;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class TinyBlockItemRenderer extends TileEntityItemStackRenderer {

    @Override
    public void renderByItem(ItemStack stack) {

        boolean isTransparent = stack.getItem() == Registration.TINY_TRANSPARENT_BLOCK;
        TextureAtlasSprite sprite_top, sprite_front, sprite_right, sprite_back, sprite_left, sprite_bottom;

        // PORT-TODO: 1.16 looked up a per-block texture override via the dropped codec system.
        //            For v0.1 tiny blocks just use their default texture.
        sprite_top=sprite_front=sprite_right=sprite_back=sprite_left=sprite_bottom = RenderHelper.getSprite(isTransparent ? TransparentBlock.TEXTURE_TRANSPARENT_BLOCK : TinyBlock.TEXTURE_TINY_BLOCK);

        float alpha = isTransparent ? .99f : 1.0f;

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.pushMatrix();

        GlStateManager.rotate(-90, 1, 0, 0);
        GlStateManager.translate(1, 0, 0);
        drawCube(sprite_top, sprite_front, sprite_right, sprite_back, sprite_left, sprite_bottom, 0xFFFFFFFF, alpha);

        GlStateManager.popMatrix();


    }

    /**
     * Draws a 1x1x1 cube at the current GL matrix origin using the block atlas.
     * Mirrors the order/orientation of RenderHelper.drawCube.
     */
    static void drawCube(TextureAtlasSprite sprite_top, TextureAtlasSprite sprite_front, TextureAtlasSprite sprite_right, TextureAtlasSprite sprite_back, TextureAtlasSprite sprite_left, TextureAtlasSprite sprite_bottom, int color, float alpha) {
        GlStateManager.pushMatrix();

        drawRectangle(0, 1, 0, 1, sprite_top, color, alpha);

        GlStateManager.rotate(-90, 1, 0, 0);
        GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, 1, sprite_back, color, alpha);

        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, 1, sprite_left, color, alpha);

        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, 1, sprite_front, color, alpha);

        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, 1, sprite_right, color, alpha);

        GlStateManager.rotate(-90, 1, 0, 0);
        GlStateManager.rotate(-90, 0, 0, 1);
        GlStateManager.translate(-1, 0, 1);
        drawRectangle(0, 1, 0, 1, sprite_bottom, color, alpha);

        GlStateManager.popMatrix();
    }

    static void drawRectangle(float x1, float x2, float y1, float y2, TextureAtlasSprite sprite, int color, float alpha) {
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (alpha * 255f);

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
