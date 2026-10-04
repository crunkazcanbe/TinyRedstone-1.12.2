package com.dannyandson.tinyredstone.blocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * 1.12.2 immediate-mode port of Tiny Redstone's RenderHelper.
 *
 * <p>1.16 passed a MatrixStack + IVertexBuilder into every draw call and baked the matrix into the
 * vertices. 1.12.2 has neither — the OpenGL matrix is global, set up by {@code PanelTileRenderer}
 * (which has already pushed/translated/scaled to this cell's origin so the cell is 1.0 wide). So the
 * draw methods here drop the matrixStack/builder params and just emit quads in immediate mode with
 * {@link Tessellator}; GL transforms (translate/rotate/scale) apply automatically. {@code combinedOverlay}
 * is dropped (1.15+ only). Cells call these exactly as before, minus those params.
 */
@SideOnly(Side.CLIENT)
public class RenderHelper {

    // ---- cubes -------------------------------------------------------------
    public static void drawCube(TextureAtlasSprite sprite, int combinedLight, int color, float alpha) {
        drawCube(sprite, sprite, sprite, sprite, sprite, sprite, combinedLight, color, alpha);
    }

    public static void drawCube(TextureAtlasSprite top, TextureAtlasSprite front, TextureAtlasSprite right,
                                TextureAtlasSprite back, TextureAtlasSprite left, TextureAtlasSprite bottom,
                                int combinedLight, int color, float alpha) {
        GlStateManager.pushMatrix();
        drawRectangle(0, 1, 0, 1, top, combinedLight, color, alpha);
        // back
        GlStateManager.rotate(-90F, 1F, 0F, 0F); GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, 1, back, combinedLight, color, alpha);
        // left
        GlStateManager.rotate(90F, 0F, 1F, 0F); GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, 1, left, combinedLight, color, alpha);
        // front
        GlStateManager.rotate(90F, 0F, 1F, 0F); GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, 1, front, combinedLight, color, alpha);
        // right
        GlStateManager.rotate(90F, 0F, 1F, 0F); GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, 1, right, combinedLight, color, alpha);
        // bottom
        GlStateManager.rotate(-90F, 1F, 0F, 0F); GlStateManager.rotate(-90F, 0F, 0F, 1F);
        GlStateManager.translate(-1, 0, 1);
        drawRectangle(0, 1, 0, 1, bottom, combinedLight, color, alpha);
        GlStateManager.popMatrix();
    }

    // ---- textured rectangles ----------------------------------------------
    public static void drawRectangle(float x1, float x2, float y1, float y2, TextureAtlasSprite sprite,
                                     int combinedLight, float alpha) {
        drawRectangle(x1, x2, y1, y2, sprite, combinedLight, 0xFFFFFF, alpha);
    }

    public static void drawRectangle(float x1, float x2, float y1, float y2, TextureAtlasSprite sprite,
                                     int combinedLight, int color, float alpha) {
        drawRectangle(x1, x2, y1, y2,
                sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(),
                combinedLight, color, alpha);
    }

    public static void drawRectangle(float x1, float x2, float y1, float y2,
                                     float u0, float u1, float v0, float v1,
                                     int combinedLight, int color, float alpha) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF, a = (int) (alpha * 255f);
        int sky = (combinedLight >> 16) & 0xFFFF, block = combinedLight & 0xFFFF;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        // 1.12.2 has POSITION_TEX_LMAP_COLOR (lightmap before color), not 1.16's POSITION_TEX_COLOR_LMAP.
        // Builder calls must follow the format's element order: pos -> tex -> lightmap -> color.
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_LMAP_COLOR);
        buf.pos(x1, y1, 0).tex(u0, v0).lightmap(sky, block).color(r, g, b, a).endVertex();
        buf.pos(x2, y1, 0).tex(u1, v0).lightmap(sky, block).color(r, g, b, a).endVertex();
        buf.pos(x2, y2, 0).tex(u1, v1).lightmap(sky, block).color(r, g, b, a).endVertex();
        buf.pos(x1, y2, 0).tex(u0, v1).lightmap(sky, block).color(r, g, b, a).endVertex();
        tess.draw();
    }

    // ---- untextured triangles (color only) --------------------------------
    public static void drawTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int color, float alpha) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF, a = (int) (alpha * 255f);
        GlStateManager.disableTexture2D();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        buf.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        buf.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        buf.pos(x3, y3, 0).color(r, g, b, a).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();
    }

    public static void drawTriangleRectangle(float x1, float x2, float y1, float y2, int color, float alpha) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF, a = (int) (alpha * 255f);
        GlStateManager.disableTexture2D();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        buf.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        buf.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        buf.pos(x2, y1, 0).color(r, g, b, a).endVertex();
        buf.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        buf.pos(x1, y2, 0).color(r, g, b, a).endVertex();
        buf.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();
    }

    // ---- sprite lookup (block atlas) --------------------------------------
    public static TextureAtlasSprite getSprite(ResourceLocation resourceLocation) {
        return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(resourceLocation.toString());
    }
}
