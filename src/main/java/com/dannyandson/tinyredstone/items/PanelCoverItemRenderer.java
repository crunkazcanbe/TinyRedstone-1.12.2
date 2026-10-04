package com.dannyandson.tinyredstone.items;

import com.dannyandson.tinyredstone.blocks.RenderHelper;
import com.dannyandson.tinyredstone.blocks.Side;
import com.dannyandson.tinyredstone.blocks.panelcovers.DarkCover;
import com.dannyandson.tinyredstone.blocks.panelcovers.LightCover;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class PanelCoverItemRenderer extends TileEntityItemStackRenderer {

    @Override
    public void renderByItem(ItemStack stack) {

        boolean isTransparent = stack.getItem() == Registration.PANEL_COVER_LIGHT;
        TextureAtlasSprite sprite_top, sprite_front, sprite_right, sprite_back, sprite_left, sprite_bottom;

        // PORT-TODO: 1.16 looked up a per-block texture override via the dropped codec system.
        //            For v0.1 covers just use their default texture.
        sprite_top = sprite_front = sprite_right = sprite_back = sprite_left = sprite_bottom = RenderHelper.getSprite(isTransparent ? LightCover.TEXTURE_LIGHT_COVER : DarkCover.TEXTURE_DEFAULT_COVER);

        float alpha = isTransparent ? .99f : 1.0f;

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.pushMatrix();

        GlStateManager.rotate(-90, 1, 0, 0);
        GlStateManager.translate(1, 0, 0);
        TinyBlockItemRenderer.drawCube(sprite_top, sprite_front, sprite_right, sprite_back, sprite_left, sprite_bottom, 0xFFFFFFFF, alpha);

        GlStateManager.popMatrix();

    }
}
