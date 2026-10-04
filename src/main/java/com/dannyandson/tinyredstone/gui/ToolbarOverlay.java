package com.dannyandson.tinyredstone.gui;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.AbstractPanelCellItem;
import com.dannyandson.tinyredstone.blocks.RenderHelper;
import com.dannyandson.tinyredstone.blocks.RotationLock;
import com.dannyandson.tinyredstone.blocks.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = TinyRedstone.MODID)
public class ToolbarOverlay {

    public static ResourceLocation TEXTURE_ROTATION_LOCK = new ResourceLocation(TinyRedstone.MODID, "block/rotation_lock");

    @SubscribeEvent
    public static void onRenderGUI(final RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR) {
            final Minecraft mcInstance = Minecraft.getMinecraft();
            final EntityPlayerSP player = mcInstance.player;

            if (player != null && !player.isSpectator()) {
                final int currentSlot = player.inventory.currentItem;
                final ItemStack stack = player.inventory.mainInventory.get(currentSlot);
                if (stack.getItem() instanceof AbstractPanelCellItem) {
                    final ScaledResolution window = event.getResolution();
                    final Side rotationLock = RotationLock.getRotationLock();

                    if (rotationLock != null) {
                        // top-left corner of selected slot + 1/2 border + 1 padding
                        final int x = (window.getScaledWidth() / 2 - 180 / 2 + currentSlot * 20) + 2 + 1;
                        final int y = (window.getScaledHeight() - 20) + 1 + 1;

                        // 1.12.2 block atlas; getSprite resolves within it (sibling RenderHelper).
                        mcInstance.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                        TextureAtlasSprite sprite = RenderHelper.getSprite(TEXTURE_ROTATION_LOCK);

                        GlStateManager.enableBlend();
                        // 1.16 mcInstance.gui.blit(matrixStack,x,y,0,5,5,sprite) ->
                        // 1.12.2 Gui.drawTexturedModalRect(x,y,sprite,width,height). The 1.16 (0,5)
                        // were unused u/v args for the sprite overload; only the 5x5 size carries over.
                        mcInstance.ingameGUI.drawTexturedModalRect(x, y, sprite, 5, 5);
                        GlStateManager.disableBlend();
                    }
                }
            }
        }
    }
}
