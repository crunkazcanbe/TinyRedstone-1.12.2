package com.dannyandson.tinyredstone.blocks.panelcells;

import com.dannyandson.tinyredstone.api.IOverlayBlockInfo;
import com.dannyandson.tinyredstone.api.IPanelCellInfoProvider;
import com.dannyandson.tinyredstone.blocks.*;
import com.dannyandson.tinyredstone.gui.NoteBlockGUI;
import com.dannyandson.tinyredstone.network.ModNetworkHandler;
import com.dannyandson.tinyredstone.network.PlaySound;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

public class NoteBlock extends TinyBlock implements IPanelCellInfoProvider {

    public static ResourceLocation TEXTURE_TINY_NOTE_BLOCK = new ResourceLocation("minecraft","blocks/noteblock");
    private static final String[] noteNames = {"F#","G","G#","A","A#","B","C","C#","D","D#","E","F"};

    private boolean powered = false;
    private int pitch = 0;
    private String instrument = "harp";

    @Override
    public void render(float partialTicks, int combinedLight, float alpha) {
        TextureAtlasSprite sprite = RenderHelper.getSprite(TEXTURE_TINY_NOTE_BLOCK);



        GlStateManager.translate(0,0,1.0);
        RenderHelper.drawRectangle(0,1,0,1,sprite,combinedLight,color,alpha);

        GlStateManager.rotate(90,1,0,0);
        GlStateManager.translate(0,-1,0);
        RenderHelper.drawRectangle(0,1,0,1,sprite,combinedLight,color,alpha);

        GlStateManager.rotate(90,0,1,0);
        GlStateManager.translate(0,0,1);
        RenderHelper.drawRectangle(0,1,0,1,sprite,combinedLight,color,alpha);

        GlStateManager.rotate(90,0,1,0);
        GlStateManager.translate(0,0,1);
        RenderHelper.drawRectangle(0,1,0,1,sprite,combinedLight,color,alpha);

        GlStateManager.rotate(90,0,1,0);
        GlStateManager.translate(0,0,1);
        RenderHelper.drawRectangle(0,1,0,1,sprite,combinedLight,color,alpha);

        GlStateManager.rotate(90,1,0,0);
        GlStateManager.translate(0,-1,0);
        RenderHelper.drawRectangle(0,1,0,1,sprite,combinedLight,color,alpha);

    }


    @Override
    public boolean neighborChanged(PanelCellPos cellPos){

        PanelCellNeighbor rightNeighbor = cellPos.getNeighbor(Side.RIGHT),
                leftNeighbor = cellPos.getNeighbor(Side.LEFT),
                backNeighbor = cellPos.getNeighbor(Side.BACK),
                frontNeighbor = cellPos.getNeighbor(Side.FRONT),
                topNeighbor = cellPos.getNeighbor(Side.TOP),
                bottomNeighbor = cellPos.getNeighbor(Side.BOTTOM);

        boolean r = super.neighborChanged(cellPos);

        if (
                (weakSignalStrength + strongSignalStrength > 0) ||
                        ((
                                ((frontNeighbor != null) ? frontNeighbor.getWeakRsOutput() : 0) +
                                        ((rightNeighbor != null) ? rightNeighbor.getWeakRsOutput() : 0) +
                                        ((backNeighbor != null) ? backNeighbor.getWeakRsOutput() : 0) +
                                        ((leftNeighbor != null) ? leftNeighbor.getWeakRsOutput() : 0)+
                                        ((topNeighbor != null) ? topNeighbor.getWeakRsOutput() : 0)+
                                        ((bottomNeighbor != null) ? bottomNeighbor.getWeakRsOutput() : 0)) > 0)
        ) {
            if (!this.powered) {
                this.powered = true;
                playNote(cellPos.getPanelTile());
                return true;
            }
        } else if (this.powered) {
            this.powered = false;
            return true;
        }

        return r;
    }

    @Override
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, EntityPlayer player){
        if (player.getHeldItemMainhand().getItem() == Registration.REDSTONE_WRENCH) {
            if (cellPos.getPanelTile().getLevel().isRemote)
                NoteBlockGUI.open(cellPos.getPanelTile(), cellPos.getIndex(), this);
        }else {
            pitch++;
            if (pitch > 24) pitch = 0;
            playNote(cellPos.getPanelTile());
        }
        return false;
    }

    @Override
    public boolean hasActivation(){return true;}

    @Override
    public NBTTagCompound writeNBT() {
        NBTTagCompound nbt = super.writeNBT();
        nbt.setInteger("pitch",this.pitch);
        nbt.setString("instrument",this.instrument);
        nbt.setBoolean("powered",this.powered);
        return nbt;
    }

    @Override
    public void readNBT(NBTTagCompound compoundNBT) {
        super.readNBT(compoundNBT);
        this.pitch= compoundNBT.getInteger("pitch");
        this.instrument= compoundNBT.getString("instrument");
        this.powered =compoundNBT.getBoolean("powered");
    }

    @Override
    public void addInfo(IOverlayBlockInfo overlayBlockInfo, PanelTile panelTile, PosInPanelCell pos) {
        overlayBlockInfo.addText("Instrument", new TextComponentTranslation("tinyredstone.noteblock." + this.instrument).getUnformattedText() );
        overlayBlockInfo.addText("Note", this.pitch + " (" + noteNames[this.pitch%12] + ")");
    }

    private void playNote(PanelTile panelTile)
    {
        if (!panelTile.getLevel().isRemote)
        {
            BlockPos pos = panelTile.getBlockPos();
            for(EntityPlayer player:panelTile.getLevel().playerEntities){
                if (player.getDistanceSq((double) pos.getX(), (double) pos.getY(), (double) pos.getZ()) < 256d)
                    // PORT-TODO: 1.12.2 vanilla note-block sound events are registered as "block.note.<instrument>"
                    //  (e.g. block.note.harp); the "block.note_block.<instrument>" naming is 1.13+. If the bundled
                    //  sounds.json does not remap these, no sound will play. Kept the 1.16 string for now to preserve behavior 1:1.
                    ModNetworkHandler.sendToClient(
                            new PlaySound(pos,"minecraft", "block.note_block." + instrument, 0.5f, (pitch==0)?0.5f:(float)Math.pow(2f,((pitch-12f)/12f))),
                            (EntityPlayerMP) player);
            }
        }
    }

    public void setInstrument(String instrument)
    {
        this.instrument=instrument;
    }
}
