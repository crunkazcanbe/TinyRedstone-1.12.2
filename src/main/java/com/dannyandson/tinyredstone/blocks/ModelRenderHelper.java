package com.dannyandson.tinyredstone.blocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Renders a real baked JSON model (block/item model) in immediate mode into the current
 * GL matrix — the scaled-to-1.0 "cell space" set up by {@link PanelTileRenderer}. This is the
 * 3D-model render path (task #231): instead of hand-coding every quad in {@link RenderHelper},
 * a cell can point at a registered {@link ModelResourceLocation} (e.g. an rsgauges switch/gauge
 * or a Logic Gate model) and have its baked quads tessellated here.
 *
 * <p>Pure 1.12.2 / Java 8. The caller is responsible for binding the block atlas
 * ({@code TextureMap.LOCATION_BLOCKS_TEXTURE}) and for any translate/rotate/scale to fit the
 * model into the cell — {@link #renderModel} only emits the quads at the current origin.
 *
 * <p>A vanilla 1-block model spans 0..1 on each axis, which already matches the cell's 1.0
 * footprint, so most models drop straight in; use {@link #renderModelScaled} to shrink a model
 * that is taller than one cell (rsgauges devices are ~6px deep).
 */
@SideOnly(Side.CLIENT)
public final class ModelRenderHelper {

    private ModelRenderHelper() {}

    /** Look up a baked model registered under the given ModelResourceLocation (e.g. "...:foo#inventory"). */
    public static IBakedModel getModel(ModelResourceLocation mrl) {
        return Minecraft.getMinecraft().getBlockRendererDispatcher()
                .getBlockModelShapes().getModelManager().getModel(mrl);
    }

    /** Convenience: look up an item/block model registered as "modid:name#inventory". */
    public static IBakedModel getInventoryModel(String modid, String name) {
        return getModel(new ModelResourceLocation(modid + ":" + name, "inventory"));
    }

    /**
     * Emit every quad of {@code model} at the current GL origin.
     * @param combinedLight packed sky/block light (as PanelTileRenderer computes)
     * @param color         ARGB tint applied to tinted quads (use 0xFFFFFF for none)
     * @param alpha         0..1 overall opacity (used for ghost previews / crashed panels)
     */
    public static void renderModel(IBakedModel model, int combinedLight, int color, float alpha) {
        if (model == null) return;

        int a = (int) (alpha * 255f) & 0xFF;
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

        for (EnumFacing face : EnumFacing.values()) {
            putQuads(buf, model.getQuads(null, face, 0L), r, g, b, a, combinedLight);
        }
        putQuads(buf, model.getQuads(null, null, 0L), r, g, b, a, combinedLight);

        tess.draw();
    }

    /**
     * Render the model shrunk uniformly about the cell's footprint center. rsgauges devices are
     * mounted plates a few pixels deep, so they look right scaled to ~0.5–0.75 of a cell and
     * lifted to sit on the panel surface.
     */
    public static void renderModelScaled(IBakedModel model, int combinedLight, int color, float alpha, float scale) {
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        // shrink about the XZ center so the device stays centred in the cell
        float off = (1f - scale) / 2f;
        net.minecraft.client.renderer.GlStateManager.translate(off, 0f, off);
        net.minecraft.client.renderer.GlStateManager.scale(scale, scale, scale);
        renderModel(model, combinedLight, color, alpha);
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }

    private static void putQuads(BufferBuilder buf, List<BakedQuad> quads,
                                 int r, int g, int b, int a, int light) {
        for (BakedQuad quad : quads) {
            buf.addVertexData(quad.getVertexData());
            // override the lightmap on the 4 verts we just appended (flat per-cell lighting,
            // matching RenderHelper's approach — no per-vertex AO)
            buf.putBrightness4(light, light, light, light);
            if (quad.hasTintIndex()) {
                // tinted quad → apply our color (and alpha)
                buf.putColorRGBA(buf.getColorIndex(4), r, g, b, a);
                buf.putColorRGBA(buf.getColorIndex(3), r, g, b, a);
                buf.putColorRGBA(buf.getColorIndex(2), r, g, b, a);
                buf.putColorRGBA(buf.getColorIndex(1), r, g, b, a);
            } else if (a < 255) {
                // untinted but translucent (ghost/crashed) → keep texture, force alpha
                buf.putColorRGBA(buf.getColorIndex(4), 255, 255, 255, a);
                buf.putColorRGBA(buf.getColorIndex(3), 255, 255, 255, a);
                buf.putColorRGBA(buf.getColorIndex(2), 255, 255, 255, a);
                buf.putColorRGBA(buf.getColorIndex(1), 255, 255, 255, a);
            }
            // else: leave the baked opaque color untouched
        }
    }
}
