package com.dannyandson.tinyredstone.blocks.panelcovers;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.IPanelCover;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.RenderHelper;
import com.dannyandson.tinyredstone.blocks.Side;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class DarkCover implements IPanelCover {

    public static ResourceLocation TEXTURE_DEFAULT_COVER = new ResourceLocation(TinyRedstone.MODID,"block/dark_cover");
    protected ResourceLocation madeFrom;
    protected TextureAtlasSprite sprite_top, sprite_front, sprite_right, sprite_back, sprite_left, sprite_bottom;

    private float x1 = 0, x2 = 1, y1 = 0, y2 = 1;

    /**
     * Drawing the cover on the panel.
     * Called from the panel's TileEntitySpecialRenderer which has already pushed/translated/scaled
     * the matrix so the cover draws at (0,0,0)->(1,1,1).
     */
    @Override
    public void render(float partialTicks, int combinedLight, int color) {

        if (sprite_top == null) {
            // PORT-TODO: 1.16 looked up a per-block texture override via the dropped codec system.
            //            For v0.1 covers just use their default texture.
            sprite_top = sprite_front = sprite_right = sprite_back = sprite_left = sprite_bottom = RenderHelper.getSprite(getDefaultResourceLocation());
        }

        int drawColor = color;

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, y2, 1);
        GlStateManager.rotate(270, 1, 0, 0);
        drawCube(sprite_top, sprite_front, sprite_right, sprite_back, sprite_left, sprite_bottom, drawColor, 1f);
        GlStateManager.popMatrix();

    }

    /**
     * Draws a 1x1x1 cube at the current GL matrix origin using the block atlas.
     * Mirrors the order/orientation of RenderHelper.drawCube.
     */
    protected static void drawCube(TextureAtlasSprite sprite_top, TextureAtlasSprite sprite_front, TextureAtlasSprite sprite_right, TextureAtlasSprite sprite_back, TextureAtlasSprite sprite_left, TextureAtlasSprite sprite_bottom, int color, float alpha) {
        GlStateManager.pushMatrix();

        //top
        drawRectangle(0, 1, 0, 1, sprite_top, color, alpha);

        //back
        GlStateManager.rotate(-90, 1, 0, 0);
        GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, 1, sprite_back, color, alpha);

        //left
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, 1, sprite_left, color, alpha);

        //front
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, 1, sprite_front, color, alpha);

        //right
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0, 0, 1);
        drawRectangle(0, 1, 0, 1, sprite_right, color, alpha);

        //bottom
        GlStateManager.rotate(-90, 1, 0, 0);
        GlStateManager.rotate(-90, 0, 0, 1);
        GlStateManager.translate(-1, 0, 1);
        drawRectangle(0, 1, 0, 1, sprite_bottom, color, alpha);

        GlStateManager.popMatrix();
    }

    /**
     * Draws a textured rectangle in the XY plane (z=0) at the current GL matrix.
     */
    protected static void drawRectangle(float x1, float x2, float y1, float y2, TextureAtlasSprite sprite, int color, float alpha) {
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

    protected ResourceLocation getDefaultResourceLocation(){
        return TEXTURE_DEFAULT_COVER;
    }

    @Override
    public void onPlace(PanelTile panelTile, EntityPlayer player) {
        ItemStack stack = ItemStack.EMPTY;
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
        NBTTagCompound nbt = new NBTTagCompound();
        if (this.madeFrom!=null) {
            nbt.setString("made_from_namespace",this.madeFrom.getResourceDomain());
            nbt.setString("made_from_path",this.madeFrom.getResourcePath());
        }
        return nbt;
    }

    @Override
    public void readNBT(NBTTagCompound compoundNBT) {
        if (compoundNBT.hasKey("made_from_namespace"))
            this.madeFrom=new ResourceLocation(compoundNBT.getString("made_from_namespace"),compoundNBT.getString("made_from_path"));
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
