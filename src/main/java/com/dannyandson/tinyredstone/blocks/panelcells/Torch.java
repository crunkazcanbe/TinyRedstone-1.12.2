package com.dannyandson.tinyredstone.blocks.panelcells;

import com.dannyandson.tinyredstone.Config;
import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.*;
import net.minecraft.block.BlockDirectional;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedList;

public class Torch implements IPanelCell
{
    private boolean output = true;
    private int changePending = 0;
    private final LinkedList<Boolean> changeHx = new LinkedList<>();
    private boolean burnout = false;
    private boolean upright = false;
    private Side baseSide=Side.BOTTOM;

    private int changedTick = -1;

    public static ResourceLocation TEXTURE_TORCH_ON = new ResourceLocation(TinyRedstone.MODID,"block/redstone_torch");
    public static ResourceLocation TEXTURE_TORCH_OFF = new ResourceLocation(TinyRedstone.MODID,"block/redstone_torch_off");
    public static ResourceLocation TEXTURE_TORCH_TOP_ON = new ResourceLocation(TinyRedstone.MODID,"block/redstone_torch_top");
    public static ResourceLocation TEXTURE_TORCH_TOP_OFF = new ResourceLocation(TinyRedstone.MODID,"block/redstone_torch_top_off");


    // Self-contained colors (no texture dependency — the bundled redstone_torch sprites are
    // non-power-of-two (2x10/2x2) and fail to stitch, so the atlas returned the black/purple
    // missing-texture. Build the torch entirely from solid-color cuboid geometry instead.)
    private static final int COLOR_STICK  = 0x6E5435; // brown wooden stick
    private static final int COLOR_TIP_ON = 0xD42A1F; // bright lit redstone tip
    private static final int COLOR_TIP_OFF= 0x611410; // dim unlit redstone tip

    /**
     * Drawing the cell on the panel
     * (GL matrix already positioned/scaled for this cell by PanelTileRenderer; cell is 1.0 wide, origin (0,0,0))
     *
     * <p>Builds a self-contained 3D redstone torch from cuboid geometry: a thin brown stick topped
     * by a glowing redstone tip. The tip glows full-bright when the torch output is on. No texture
     * atlas dependency (the bundled torch sprites are broken/non-POT) and no Project Red dependency.
     */
    // A guaranteed-stitched solid vanilla sprite we tint to get clean, lightmap-aware colours.
    private static final ResourceLocation SOLID = new ResourceLocation("minecraft", "blocks/wool_colored_white");

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void render(float partialTicks, int combinedLight, float alpha) {

        boolean on = this.getWeakRsOutput(Side.FRONT) > 0;
        TextureAtlasSprite solid = RenderHelper.getSprite(SOLID);

        if (this.upright) {
            GlStateManager.rotate(90, 1, 0, 0);
            GlStateManager.translate(0, 0, -0.375f);
        }
        else if (this.baseSide==Side.FRONT) {
            GlStateManager.rotate(180, 0, 0, 1);
            GlStateManager.translate(-1, -1.125f, 0.125f);
            GlStateManager.rotate(60, 1, 0, 0);
        }else {
            GlStateManager.rotate(60, 1, 0, 0);
            GlStateManager.translate(0, 0.03125f, 0);
        }

        // Geometry runs up the +Y axis (cell origin at base). Stick is a thin square column;
        // the lit tip is a slightly fatter cube near the top.
        float s1 = 0.4375f, s2 = 0.5625f;   // stick footprint (1/8 wide, centred)
        float stickTop = 0.625f;            // height the bare stick reaches
        float t1 = 0.40625f, t2 = 0.59375f; // tip footprint (a touch wider than the stick)
        float tipBottom = 0.5625f, tipTop = 0.8125f;

        // ---- stick (brown column) ----
        drawColumn(solid, s1, s2, 0f, stickTop, COLOR_STICK, combinedLight, alpha);

        // ---- glowing tip (redstone) ----
        int tipColor = on ? COLOR_TIP_ON : COLOR_TIP_OFF;
        int tipLight = on ? 15728880 : combinedLight; // full-bright lightmap when lit
        drawColumn(solid, t1, t2, tipBottom, tipTop, tipColor, tipLight, alpha);
    }

    /**
     * Draw a self-contained axis-aligned box centred on the cell, running up the +Y axis from
     * {@code yBot} to {@code yTop} with a square {@code [h1,h2]} horizontal footprint. Pure colour,
     * no texture. Restores the GL matrix when done.
     */
    private void drawColumn(TextureAtlasSprite solid, float h1, float h2, float yBot, float yTop,
                            int color, int light, float alpha) {
        GlStateManager.pushMatrix();
        // four vertical walls — emit on the z=0 plane, then translate the box into place
        GlStateManager.translate(0, 0, h1);
        RenderHelper.drawRectangle(h1, h2, yBot, yTop, solid, light, color, alpha);
        GlStateManager.translate(0, 0, h2 - h1);
        RenderHelper.drawRectangle(h1, h2, yBot, yTop, solid, light, color, alpha);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        // left / right walls (rotate so the quad faces along X)
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(-h2, 0, h1);
        RenderHelper.drawRectangle(h1, h2, yBot, yTop, solid, light, color, alpha);
        GlStateManager.translate(0, 0, h2 - h1);
        RenderHelper.drawRectangle(h1, h2, yBot, yTop, solid, light, color, alpha);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        // top cap (lay flat at y=yTop)
        GlStateManager.rotate(-90, 1, 0, 0);
        GlStateManager.translate(0, -h1, yTop);
        RenderHelper.drawRectangle(h1, h2, h1, h2, solid, light, color, alpha);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean onPlace(PanelCellPos cellPos, EntityPlayer player) {
        if(RotationLock.getServerRotationLock(player)==null) {
            // PORT-TODO: relies on PanelTile.getBlockState() (project-wide convention; PanelTile already
            //   uses it ~12x) and panel facing being BlockDirectional.FACING. If PanelBlock declares its
            //   own FACING PropertyDirection, switch to that property here.
            EnumFacing panelFacing = cellPos.getPanelTile().getBlockState().getValue(BlockDirectional.FACING);
            Vec3d look = player.getLookVec();
            double playerToPanel;
            switch (panelFacing) {
                case NORTH:
                    playerToPanel = -look.z;
                    break;
                case SOUTH:
                    playerToPanel = look.z;
                    break;
                case WEST:
                    playerToPanel = -look.x;
                    break;
                case EAST:
                    playerToPanel = look.x;
                    break;
                case UP:
                    playerToPanel = look.y;
                    break;
                default:
                    playerToPanel = -look.y;
            }
            if (playerToPanel > 0.95)
                this.upright = true;
        }
        neighborChanged(cellPos);
        return true;
    }

    /**
     * Called when neighboring redstone signal output changes.
     * This can be called multiple times in a tick.
     * Passes PanelCellPos object for this cell which can be used to query PanelTile for PanelCellNeighbor objects - objects wrapping another IPanelCell or a BlockState
     * @param cellPos PanelCellPos object for this cell. Can be used to query paneltile about neighbors
     * @return boolean indicating whether redstone output of this cell has changed
     */
    @Override
    public boolean neighborChanged(PanelCellPos cellPos) {
        this.changedTick = cellPos.getPanelTile().getRelTickTime();
        return false;
    }

    private void checkInputs(PanelCellPos cellPos){

        PanelCellNeighbor inputNeighbor = cellPos.getNeighbor((baseSide==Side.FRONT)?Side.FRONT:(upright)?Side.BOTTOM:Side.BACK);

        boolean output = (inputNeighbor==null || inputNeighbor.getWeakRsOutput() ==0);

        if (burnout && output)
        {
            int changes=0;
            for (Boolean b : changeHx)
            {
                if (b)changes++;
            }
            if (changes<=16) burnout=false;
            this.changePending=2;
        }

        if (!burnout && output!=this.output)
        {
            this.output=output;
            this.changePending=2;
        }
    }

    /**
     * Gets redstone output of the given side of the cell
     *
     * @param outputDirection direction from which the output is being read
     * @return integer 0-15 indicating the strength of redstone signal
     */
    @Override
    public int getWeakRsOutput(Side outputDirection)
    {
        if (!burnout &&
                ((baseSide==Side.FRONT && outputDirection!=Side.FRONT) || (!upright && outputDirection!= Side.BACK )|| (upright && outputDirection!=Side.BOTTOM)) &&
                ((output&&changePending==0)||(!output&&changePending>0))
        )
            return 15;
        else
            return 0;
    }
    @Override
    public int getStrongRsOutput(Side outputDirection) {
        if (outputDirection==Side.TOP && !burnout && ((output&&changePending==0)||(!output&&changePending>0)))
            return 15;
        return 0;
    }

    /**
     * If this cell outputs light, return the level here. Otherwise, return 0.
     *
     * @return Light level to output 0-15
     */
    @Override
    public int lightOutput() {
        return (Config.TORCH_LIGHT && this.getWeakRsOutput(Side.FRONT)>0)?1:0;
    }

    /**
     * Called at the beginning of each tick if isTicking() returned true on last call.
     *
     * @return boolean indicating whether redstone output of this cell has changed
     */
    @Override
    public boolean tick(PanelCellPos cellPos) {

        if (this.changedTick>-1 && this.changedTick<cellPos.getPanelTile().getRelTickTime()){
            this.checkInputs(cellPos);
            this.changedTick=-1;
        }

        boolean changed = false;

        changeHx.add(changePending==2);
        if (changeHx.size()>60) changeHx.pop();
        int changes = 0;
        for (Boolean b : changeHx)
        {
            if (b)changes++;
        }
        if (changes>16) burnout=true;

        if (changePending > 1) {
            changePending--;
        } else if (changePending == 1) {
            changePending--;
            changed = true;
        }

        if (this.changedTick>-1){
            this.checkInputs(cellPos);
            this.changedTick=-1;
        }

        return changed;

    }

    @Override
    public boolean needsSolidBase(){return true;}

    @Override
    public boolean canAttachToBaseOnSide(Side side) {
        return side!=Side.TOP;
    }

    @Override
    public Side getBaseSide(){return this.baseSide;}

    @Override
    public void setBaseSide(Side side){
        this.baseSide=side;
        if (side==Side.FRONT)
            this.upright=false;
    }

    @Override
    public NBTTagCompound writeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("output",output);
        nbt.setInteger("changePending",changePending);
        nbt.setBoolean("burnout",burnout);
        nbt.setBoolean("upright",upright);
        nbt.setString("baseSide",baseSide.name());

        StringBuilder changeHxString = new StringBuilder();
        for (Object b : changeHx.toArray())
        {
            changeHxString.append(((Boolean) b) ? "1" : "0");
        }
        nbt.setString("changeHx", changeHxString.toString());
        nbt.setInteger("changedTick",this.changedTick);

        return nbt;
    }

    @Override
    public void readNBT(NBTTagCompound compoundNBT) {
        this.output = compoundNBT.getBoolean("output");
        this.changePending = compoundNBT.getInteger("changePending");
        this.burnout = compoundNBT.getBoolean("burnout");
        this.upright = compoundNBT.getBoolean("upright");
        if (compoundNBT.getString("baseSide").length()>0)
            this.baseSide=Side.valueOf(compoundNBT.getString("baseSide"));
        else
            baseSide=Side.BOTTOM;

        String changeHxString = compoundNBT.getString("changeHx");
        for (Byte b : changeHxString.getBytes())
        {
            changeHx.add(b==49);
            if (changeHx.size()>60)changeHx.pop();
        }
        this.changedTick=compoundNBT.getInteger("changedTick");

    }

    @Override
    public PanelCellVoxelShape getShape()
    {
        return new PanelCellVoxelShape(new Vec3d(.25d,0d,.25d),new Vec3d(.75d,1d,.75d));
    }

}
