package com.dannyandson.tinyredstone.blocks;

import com.dannyandson.tinyredstone.TinyRedstone;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Project-Red-style raised wire pieces for the tiny redstone dust cell, modelled in Blockbench
 * (blockbench/tiny_redstone_wire.bbmodel -> models/block/wire/*.json). The JSON models are not
 * referenced by any blockstate, so they are baked here by hand and drawn by {@link ModelRenderHelper}.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = TinyRedstone.MODID, value = Side.CLIENT)
public final class WireModels {

    public static final ResourceLocation TEXTURE = new ResourceLocation(TinyRedstone.MODID, "block/tiny_redstone_wire");

    public static IBakedModel CENTER, ARM_EAST, ARM_WEST, CLIMB;

    private WireModels() {}

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(TEXTURE);
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        CENTER = bake("center");
        ARM_EAST = bake("arm_east");
        ARM_WEST = bake("arm_west");
        CLIMB = bake("climb");
    }

    private static IBakedModel bake(String name) {
        try {
            IModel model = ModelLoaderRegistry.getModel(new ResourceLocation(TinyRedstone.MODID, "block/wire/" + name));
            return model.bake(model.getDefaultState(), DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter());
        } catch (Exception e) {
            TinyRedstone.LOGGER.error("Could not bake tiny redstone wire model " + name, e);
            return null; // RedstoneDust falls back to its old flat rendering
        }
    }

    public static boolean ready() {
        return CENTER != null && ARM_EAST != null && ARM_WEST != null && CLIMB != null;
    }

    /**
     * Draw a wire model in the cell's flat drawing frame (the XY plane RenderHelper.drawRectangle
     * uses, +Z = up off the surface). JSON models are Y-up, so map JSON Y -> frame Z and
     * JSON Z -> frame (1 - Y); JSON north (z=0) therefore lands on frame y=1.
     */
    public static void draw(IBakedModel model, int combinedLight, int color, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 1, 0);
        GlStateManager.rotate(90, 1, 0, 0);
        ModelRenderHelper.renderModel(model, combinedLight, color, alpha);
        GlStateManager.popMatrix();
    }
}
