package com.dannyandson.tinyredstone.blocks.panelcells;

import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelCellSegment;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;

public class StoneButton extends Button {
    public static ResourceLocation TEXTURE_OAK_PLANKS = new ResourceLocation("minecraft","blocks/stone");

    /**
     * Called when the cell is activated. i.e. player right clicked on the cell of the panel tile.
     *
     * @param cellPos The position of the clicked IPanelCell within the panel (this IPanelCell)
     * @param segmentClicked Which of nine segment within the cell were clicked.
     * @param player player who activated (right-clicked) the cell
     * @return true if a change was made to the cell output
     */
    @Override
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, EntityPlayer player){
        if (!active)
        {
            PanelTile panelTile = cellPos.getPanelTile();
            // PORT-TODO: getLevel()/getBlockPos() are 1.16 names; mirrors sibling parent Button.java.
            // In 1.12.2 these become getWorld()/getPos() — convert here once Button.java is converted to keep consistent.
            panelTile.getLevel().playSound(
                    panelTile.getPos().getX(), panelTile.getPos().getY(), panelTile.getPos().getZ(),
                    SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON,
                    SoundCategory.BLOCKS, 0.25f, 2f, false
            );
            this.active=true;
            this.ticksRemaining =20;
            return true;
        }
        return false;
    }

    protected TextureAtlasSprite getSprite()
    {
        return RenderHelper.getSprite(TEXTURE_OAK_PLANKS);
    }


}
