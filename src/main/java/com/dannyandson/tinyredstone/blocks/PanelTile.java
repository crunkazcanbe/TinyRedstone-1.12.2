package com.dannyandson.tinyredstone.blocks;

import com.dannyandson.tinyredstone.Config;
import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.IObservingPanelCell;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.api.IPanelCover;
import com.dannyandson.tinyredstone.blocks.panelcells.*;
import com.dannyandson.tinyredstone.network.ModNetworkHandler;
import com.dannyandson.tinyredstone.network.PlaySound;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.*;
// 1.12.2 has no VoxelShape/VoxelShapes (those are 1.14+). The panel's collision/selection shape is a single
// AxisAlignedBB here; multi-box unions are collapsed to the encompassing bounding box (see PanelBlock).

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.*;

public class PanelTile extends TileEntity implements ITickable {

    //panel data (saved)
    private Map<Integer, IPanelCell> cells = new HashMap<>();
    private Map<Integer, Side> cellDirections = new HashMap<>();
    protected Map<Side, Integer> strongPowerToNeighbors = new HashMap<>();
    protected Map<Side, Integer> weakPowerToNeighbors = new HashMap<>();
    protected Map<Side, Integer> wirePowerToNeighbors = new HashMap<>();

    // PORT-TODO: 1.16 DyeColor.getColorValue() returned a packed RGB int; EnumDyeColor.getColorValue() is the
    // closest 1.12.2 equivalent (returns the dye's color value). Verify it yields the same gray used by the model.
    protected Integer Color = EnumDyeColor.GRAY.getColorValue();
    private Integer lightOutput = 0;
    protected boolean flagLightUpdate = false;
    private boolean flagCrashed = false;
    private boolean flagOverflow = false;
    protected IPanelCover panelCover = null;

    //other state fields (not saved)
    private boolean flagUpdate = false;
    private boolean flagSync = true;
    protected PanelCellGhostPos panelCellGhostPos;
    protected PanelCellPos panelCellHovering;
    private AxisAlignedBB voxelShape = null;
    protected static boolean checkWireSignals = true;
    private int relTickTime = 0;

    //for backward compat. Remove on next major update (2.x.x).
    private boolean fixLegacyFacing = false;


    public PanelTile() {
        super();
    }

    /**
     * 1.16 used this.level / getLevel(). Keep a getLevel() wrapper so sibling code that still calls
     * getLevel() works against the 1.12.2 world field.
     */
    public net.minecraft.world.World getLevel() {
        return this.world;
    }

    /**
     * 1.16 TileEntity had getBlockState()/getBlockPos(). 1.12.2 TileEntity has only getPos() and
     * stores no cached blockstate, so provide wrappers that read the live state from the world.
     */
    public net.minecraft.block.state.IBlockState getBlockState() {
        return this.world.getBlockState(this.pos);
    }

    public net.minecraft.util.math.BlockPos getBlockPos() {
        return this.pos;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel()!=null && !getLevel().isRemote)
            flagUpdate=true;
    }

    //Tell Minecraft to render this block whenever any of the block space is within view.
    //By default, it only renders when the base model is within view.
    @Override
    public AxisAlignedBB getRenderBoundingBox()
    {
        return new AxisAlignedBB(getPos());
    }

    /* When the world loads from disk, the server needs to send the TileEntity information to the client
    //  it uses getUpdatePacket(), getUpdateTag(), onDataPacket(), and handleUpdateTag() to do this:
    //  getUpdatePacket() and onDataPacket() are used for one-at-a-time TileEntity updates
    //  getUpdateTag() and handleUpdateTag() are used by vanilla to collate together into a single chunk update packet
    */

    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        this.writeToNBT(nbtTagCompound);
        int tileEntityType = -1;  // arbitrary number; only used for vanilla TileEntities.
        return new SPacketUpdateTileEntity(this.pos, tileEntityType, nbtTagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        if (this.world.isRemote) {
            this.cells.clear();
            this.cellDirections.clear();
        }
        this.readFromNBT(pkt.getNbtCompound());   // read from the nbt in the packet
    }

    /* Creates a tag containing the TileEntity information, used by vanilla to transmit from server to client*/
    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    /* Populates this TileEntity with information from the tag, used by vanilla to transmit from server to client*/
    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        if (this.world.isRemote) {
            this.cells.clear();
            this.cellDirections.clear();
        }
        this.readFromNBT(tag);
    }



    public NBTTagCompound saveToNbt(NBTTagCompound compoundNBT) {

        NBTTagCompound cellsNBT = new NBTTagCompound();

        for (Integer key : this.cells.keySet()) {
            IPanelCell cell = this.cells.get(key);
            NBTTagCompound cellNBT = new NBTTagCompound();

            cellNBT.setString("class", cell.getClass().getCanonicalName());
            cellNBT.setString("facing", this.cellDirections.get(key).name());
            cellNBT.setTag("data", cell.writeNBT());

            cellsNBT.setTag(key.toString(), cellNBT);
        }

        compoundNBT.setTag("cells", cellsNBT);
        if(this.Color!=EnumDyeColor.GRAY.getColorValue())
            compoundNBT.setInteger("color", this.Color);
        if (panelCover!=null) {
            compoundNBT.setString("cover", panelCover.getClass().getCanonicalName());
            NBTTagCompound coverData = panelCover.writeNBT();
            if (coverData!=null)
                compoundNBT.setTag("coverData",coverData);
        }

        return compoundNBT;
    }

    /* This is where you save any data that you don't want to lose when the tile entity unloads*/
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound parentNBTTagCompound) {
        try {
            if (this.strongPowerToNeighbors.size()==5) {
                NBTTagCompound strongPowerToNeighbors = new NBTTagCompound();
                strongPowerToNeighbors.setInteger(Side.FRONT.ordinal() + "", this.strongPowerToNeighbors.get(Side.FRONT));
                strongPowerToNeighbors.setInteger(Side.RIGHT.ordinal() + "", this.strongPowerToNeighbors.get(Side.RIGHT));
                strongPowerToNeighbors.setInteger(Side.BACK.ordinal() + "",  this.strongPowerToNeighbors.get(Side.BACK));
                strongPowerToNeighbors.setInteger(Side.LEFT.ordinal() + "",  this.strongPowerToNeighbors.get(Side.LEFT));
                strongPowerToNeighbors.setInteger(Side.TOP.ordinal() + "",  this.strongPowerToNeighbors.get(Side.TOP));
                parentNBTTagCompound.setTag("strong_power_outgoing", strongPowerToNeighbors);
                if (!hasBase())
                    strongPowerToNeighbors.setInteger(Side.BOTTOM.ordinal() + "",  this.strongPowerToNeighbors.get(Side.BOTTOM));
            }
            if (this.weakPowerToNeighbors.size()==5) {
                NBTTagCompound weakPowerToNeighbors = new NBTTagCompound();
                weakPowerToNeighbors.setInteger(Side.FRONT.ordinal() + "", this.weakPowerToNeighbors.get(Side.FRONT));
                weakPowerToNeighbors.setInteger(Side.RIGHT.ordinal() + "", this.weakPowerToNeighbors.get(Side.RIGHT));
                weakPowerToNeighbors.setInteger(Side.BACK.ordinal() + "",  this.weakPowerToNeighbors.get(Side.BACK));
                weakPowerToNeighbors.setInteger(Side.LEFT.ordinal() + "",  this.weakPowerToNeighbors.get(Side.LEFT));
                weakPowerToNeighbors.setInteger(Side.TOP.ordinal() + "",  this.weakPowerToNeighbors.get(Side.TOP));
                if (!hasBase())
                    weakPowerToNeighbors.setInteger(Side.BOTTOM.ordinal() + "",  this.weakPowerToNeighbors.get(Side.BOTTOM));
                parentNBTTagCompound.setTag("weak_power_outgoing", weakPowerToNeighbors);
            }
            if (this.wirePowerToNeighbors.size()==5) {
                NBTTagCompound wirePowerToNeighbors = new NBTTagCompound();
                wirePowerToNeighbors.setInteger(Side.FRONT.ordinal() + "", this.wirePowerToNeighbors.get(Side.FRONT));
                wirePowerToNeighbors.setInteger(Side.RIGHT.ordinal() + "", this.wirePowerToNeighbors.get(Side.RIGHT));
                wirePowerToNeighbors.setInteger(Side.BACK.ordinal() + "",  this.wirePowerToNeighbors.get(Side.BACK));
                wirePowerToNeighbors.setInteger(Side.LEFT.ordinal() + "",  this.wirePowerToNeighbors.get(Side.LEFT));
                wirePowerToNeighbors.setInteger(Side.TOP.ordinal() + "",  this.wirePowerToNeighbors.get(Side.TOP));
                parentNBTTagCompound.setTag("wire_power_outgoing", wirePowerToNeighbors);
            }

            parentNBTTagCompound.setInteger("lightOutput",this.lightOutput);
            parentNBTTagCompound.setBoolean("flagLightUpdate",this.flagLightUpdate);
            parentNBTTagCompound.setBoolean("flagCrashed",this.flagCrashed);
            parentNBTTagCompound.setBoolean("flagOverflow",this.flagOverflow);

        } catch (NullPointerException exception) {
            TinyRedstone.LOGGER.error("Exception thrown when attempting to save power inputs and outputs: " + exception.toString() + ((exception.getStackTrace().length>0)?exception.getStackTrace()[0].toString():""));
        }

        return super.writeToNBT(this.saveToNbt(parentNBTTagCompound));
    }

    // This is where you load the data that you saved in writeToNBT
    @Override
    public void readFromNBT(NBTTagCompound parentNBTTagCompound) {
        int previousLightOutput = this.lightOutput;

        super.readFromNBT(parentNBTTagCompound);

        // important rule: never trust the data you read from NBT, make sure it can't cause a crash

        this.loadCellsFromNBT(parentNBTTagCompound,true);

        NBTTagCompound strongPowerToNeighbors = parentNBTTagCompound.getCompoundTag("strong_power_outgoing");
        if (!strongPowerToNeighbors.hasNoTags()) {
            this.strongPowerToNeighbors.put(Side.FRONT, strongPowerToNeighbors.getInteger(Side.FRONT.ordinal() + ""));
            this.strongPowerToNeighbors.put(Side.RIGHT, strongPowerToNeighbors.getInteger(Side.RIGHT.ordinal() + ""));
            this.strongPowerToNeighbors.put(Side.BACK,  strongPowerToNeighbors.getInteger(Side.BACK.ordinal() + ""));
            this.strongPowerToNeighbors.put(Side.LEFT,  strongPowerToNeighbors.getInteger(Side.LEFT.ordinal() + ""));
            this.strongPowerToNeighbors.put(Side.TOP,  strongPowerToNeighbors.getInteger(Side.TOP.ordinal() + ""));
            if (strongPowerToNeighbors.hasKey(Side.BOTTOM.ordinal() + ""))
                this.strongPowerToNeighbors.put(Side.BOTTOM,  strongPowerToNeighbors.getInteger(Side.BOTTOM.ordinal() + ""));
        }
        NBTTagCompound weakPowerToNeighbors = parentNBTTagCompound.getCompoundTag("weak_power_outgoing");
        if (!weakPowerToNeighbors.hasNoTags()) {
            this.weakPowerToNeighbors.put(Side.FRONT, weakPowerToNeighbors.getInteger(Side.FRONT.ordinal() + ""));
            this.weakPowerToNeighbors.put(Side.RIGHT, weakPowerToNeighbors.getInteger(Side.RIGHT.ordinal() + ""));
            this.weakPowerToNeighbors.put(Side.BACK,  weakPowerToNeighbors.getInteger(Side.BACK.ordinal() + ""));
            this.weakPowerToNeighbors.put(Side.LEFT,  weakPowerToNeighbors.getInteger(Side.LEFT.ordinal() + ""));
            this.weakPowerToNeighbors.put(Side.TOP,  weakPowerToNeighbors.getInteger(Side.TOP.ordinal() + ""));
            if (weakPowerToNeighbors.hasKey(Side.BOTTOM.ordinal() + ""))
                this.weakPowerToNeighbors.put(Side.BOTTOM,  weakPowerToNeighbors.getInteger(Side.BOTTOM.ordinal() + ""));
        }
        NBTTagCompound wirePowerToNeighbors = parentNBTTagCompound.getCompoundTag("wire_power_outgoing");
        if (!wirePowerToNeighbors.hasNoTags()) {
            this.wirePowerToNeighbors.put(Side.FRONT, wirePowerToNeighbors.getInteger(Side.FRONT.ordinal() + ""));
            this.wirePowerToNeighbors.put(Side.RIGHT, wirePowerToNeighbors.getInteger(Side.RIGHT.ordinal() + ""));
            this.wirePowerToNeighbors.put(Side.BACK,  wirePowerToNeighbors.getInteger(Side.BACK.ordinal() + ""));
            this.wirePowerToNeighbors.put(Side.LEFT,  wirePowerToNeighbors.getInteger(Side.LEFT.ordinal() + ""));
            this.wirePowerToNeighbors.put(Side.TOP,  wirePowerToNeighbors.getInteger(Side.TOP.ordinal() + ""));
        }

        this.lightOutput = parentNBTTagCompound.getInteger("lightOutput");
        this.flagLightUpdate = parentNBTTagCompound.getBoolean("flagLightUpdate");
        this.flagCrashed = parentNBTTagCompound.getBoolean("flagCrashed");
        this.flagOverflow = parentNBTTagCompound.getBoolean("flagOverflow");

        if (parentNBTTagCompound.hasKey("color")) {
            int color = parentNBTTagCompound.getInteger("color");
            if (this.Color != color) {
                this.Color = color;
                if (this.world!=null)
                    this.world.notifyBlockUpdate(pos,getBlockState(),getBlockState(),0);
            }
        }

        String coverClass = parentNBTTagCompound.getString("cover");
        if (coverClass.length()>0)
        {
            try {
                panelCover= (IPanelCover) Class.forName(coverClass).getConstructor().newInstance();
                if (parentNBTTagCompound.hasKey("coverData")){
                    panelCover.readNBT(parentNBTTagCompound.getCompoundTag("coverData"));
                }
            } catch (Exception exception) {
                TinyRedstone.LOGGER.error("Exception attempting to construct IPanelCover class " + coverClass +
                        ": " + exception.getMessage() + " " + ((exception.getStackTrace().length>0)?exception.getStackTrace()[0].toString():""));
            }
        }
        else
        {
            panelCover=null;
        }

        // PORT-TODO: 1.16 used world.getLightEngine().checkBlock(pos) for the new lighting engine.
        // 1.12.2 has no LightEngine; request a light recheck via the world's lightUpdates instead.
        if(this.lightOutput != previousLightOutput && this.world != null) {
            this.world.checkLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, pos);
        }

        if (world!=null && !world.isRemote) {
            flagUpdate = true;
        }


    }

    public void loadCellsFromNBT(NBTTagCompound parentNBTTagCompound,boolean fixFacing)
    {
        NBTTagCompound cellsNBT = parentNBTTagCompound.getCompoundTag("cells");

        for (String index : cellsNBT.getKeySet()) {
            NBTTagCompound cellNBT = cellsNBT.getCompoundTag(index);
            if (cellNBT.hasKey("data")) {
                String className = cellNBT.getString("class");
                try {
                    IPanelCell cell = (IPanelCell) Class.forName(className).getConstructor().newInstance();
                    cell.readNBT(cellNBT.getCompoundTag("data"));
                    Integer i = Integer.parseInt(index);
                    this.cells.put(i, cell);


                    if (cellNBT.hasKey("direction"))
                    {
                        //backward compatibility
                        EnumFacing direction = EnumFacing.getFront(cellNBT.getInteger("direction"));
                        if (direction==EnumFacing.NORTH) this.cellDirections.put(i,Side.FRONT);
                        else if (direction==EnumFacing.EAST) this.cellDirections.put(i,Side.RIGHT);
                        else if (direction==EnumFacing.SOUTH) this.cellDirections.put(i,Side.BACK);
                        else if (direction==EnumFacing.WEST) this.cellDirections.put(i,Side.LEFT);

                        //for panels placed before the direction update, set facing to DOWN
                        if (fixFacing)
                            this.fixLegacyFacing = true;

                    }
                    else
                        this.cellDirections.put(i, Side.valueOf(cellNBT.getString("facing")));
                } catch (Exception exception) {
                    TinyRedstone.LOGGER.error("Exception attempting to construct IPanelCell class " + className +
                            ": " + exception.getMessage() + " " + ((exception.getStackTrace().length>0)?exception.getStackTrace()[0].toString():""));
                }
            }
        }
        flagVoxelShapeUpdate();
    }

    /**
     * Don't render the object if the player is too far away
     *
     * @return the maximum distance squared at which the TER should render
     */
    @Override
    public double getMaxRenderDistanceSquared() {
        final int MAXIMUM_DISTANCE_IN_BLOCKS = 16;
        return MAXIMUM_DISTANCE_IN_BLOCKS * MAXIMUM_DISTANCE_IN_BLOCKS;
    }

    public void removeOutOfRange(EntityPlayer player) {
        if (hasBase()) {
            List<Integer> indices = new ArrayList<>(cells.keySet());
            for (int i : indices) {
                if (i > 447)
                    ((PanelBlock) this.getBlockState().getBlock()).removeCell(PanelCellPos.fromIndex(this, (Integer) i), player);
            }
        }
    }

    @Override
    public void update() {
        try {
            if (!flagCrashed) {
                if (world.isRemote) {
                    if (this.isCovered()) {
                        this.panelCellGhostPos = null;
                        this.panelCellHovering = null;
                    } else {
                        PanelCellGhostPos gPos = PanelTileRenderer.getPlayerLookingAtCell(this);
                        if (gPos != null)
                            gPos.getPanelTile().panelCellGhostPos = gPos;
                        else
                            this.panelCellGhostPos = null;
                    }
                }else{
                    boolean dirty = false;
                    this.relTickTime = (this.relTickTime+1)%(1048576);
                    //backward compatibility. Remove on major update (2.x.x).
                    if (fixLegacyFacing) {
                        // PORT-TODO: 1.16 used defaultBlockState().setValue(FACING, DOWN) + setBlockAndUpdate.
                        // 1.12.2 equivalent: getDefaultState().withProperty(FACING, DOWN) + setBlockState with update flag 3.
                        world.setBlockState(pos, Registration.REDSTONE_PANEL_BLOCK.getDefaultState().withProperty(net.minecraft.block.BlockDirectional.FACING, EnumFacing.DOWN), 3);
                        fixLegacyFacing = false;
                        dirty = true;
                    }

                    //call the tick() method in all our cells and grab any updated pistons
                    List<Integer> pistons = null;
                    Set<Integer> keys = new TreeSet<Integer>(this.cells.keySet());
                    for (Integer index : keys) {
                        PanelCellPos cellPos = PanelCellPos.fromIndex(this, index);
                        IPanelCell panelCell = this.cells.get(index);
                        boolean update = panelCell.tick(cellPos);
                        if (update) {
                            if (panelCell instanceof Piston) {
                                if (pistons == null)
                                    pistons = new ArrayList<>();
                                pistons.add(index);
                            } else {
                                if (panelCell instanceof Button) {
                                    for (EntityPlayer player : this.getLevel().playerEntities) {
                                        if (player.getDistanceSq((double) getPos().getX(), (double) getPos().getY(), (double) getPos().getZ()) < 64d)
                                            ModNetworkHandler.sendToClient(
                                                    new PlaySound(getPos(), "minecraft", panelCell instanceof StoneButton ? "block.stone_button.click_off" : "block.wooden_button.click_off", 0.25f, 2f),
                                                    (EntityPlayerMP) player);
                                    }
                                }
                                updateNeighborCells(cellPos);
                            }
                            dirty = true;
                        }

                    }

                    //if any pistons updated state, try to update them
                    if (pistons != null) {
                        for (Integer index : pistons) {
                            updatePiston(index);
                        }
                    }

                    if (this.flagLightUpdate) {
                        this.flagLightUpdate = false;
                        // PORT-TODO: see readFromNBT note — 1.16 LightEngine.checkBlock replaced with 1.12.2 checkLightFor.
                        this.world.checkLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, pos);
                    }


                    if (flagUpdate) {
                        updateSide(Side.FRONT);
                        updateSide(Side.RIGHT);
                        updateSide(Side.BACK);
                        updateSide(Side.LEFT);
                        updateSide(Side.TOP);
                        updateSide(Side.BOTTOM);
                    }
                    if (dirty || flagUpdate) {
                        markDirty();
                        updateOutputs();
                    }

                    if (flagSync || dirty || flagUpdate) {
                        sync();
                        flagSync = false;
                        flagUpdate = false;
                    }

                    if (cells.size()==0 && !hasBase()) {
                        world.destroyBlock(pos, true);
                    }

                }
            }
        } catch (Exception e) {
            this.handleCrash(e);
        }
    }

    public int getRelTickTime() {
        return relTickTime;
    }

    private void updatePiston(int index) {
        IPanelCell panelCell = cells.get(index);
        if (panelCell instanceof Piston) {
            PanelTile panelTile = this;
            PanelCellPos pistonPos = PanelCellPos.fromIndex(this,index);

            //get the side of the panel that the piston head is facing (back of tiny piston cell)
            Side movingToward = pistonPos.getCellFacing().getOpposite();
            PanelCellPos moverPos = pistonPos.offset(movingToward);

            if (!((Piston) panelCell).isExtended() && panelCell instanceof StickyPiston && moverPos!=null && moverPos.getIPanelCell()==null) {
                moverPos = moverPos.offset(movingToward);
                movingToward = movingToward.getOpposite();
            }

            for(EntityPlayer player:this.getLevel().playerEntities){
                if (player.getDistanceSq((double) getPos().getX(), (double) getPos().getY(), (double) getPos().getZ()) < 64d)
                    ModNetworkHandler.sendToClient(
                            new PlaySound(getPos(),"minecraft",(((Piston) panelCell).isExtended()) ? "block.piston.extend" : "block.piston.contract", 0.25f, 2f),
                            (EntityPlayerMP) player);
            }

            if (moverPos != null) {
                PanelTile moverPanelTile = moverPos.getPanelTile();
                if (moverPanelTile.cells.containsKey(moverPos.getIndex()))
                    panelTile.moveCell(moverPos, movingToward, 0);
                else {
                    panelTile.updateNeighborCells(moverPos);
                }
            }
            PanelCellPos abovePiston = pistonPos.offset(Side.TOP);
            if (abovePiston!=null && abovePiston.getIPanelCell()!=null)
                panelTile.updateCell(abovePiston);
        }
    }

    /**
     * move a component to an adjacent position and iterate into adjacent cells
     * @param cellPos position to move a cell from
     * @param towardSide direction to move it toward
     * @param iteration how many times have we iterated
     * @return whether the move was successful and/or if adjacent cell can move into this pos
     */
    private boolean moveCell(PanelCellPos cellPos, Side towardSide, Integer iteration) {
        if (iteration > 12) return false;

        IPanelCell cell = cellPos.getIPanelCell();

        if (cell == null) return true;
        if (!cell.isPushable()) return false;

        PanelCellPos newPos = cellPos.offset(towardSide);

        //try to move the neighbor first and make sure it's successful
        if (newPos==null || ( newPos.getIPanelCell() != null && !newPos.getPanelTile().moveCell(newPos,towardSide,iteration+1)))
            return false;

        int oldIndex = cellPos.getIndex();
        int newIndex = newPos.getIndex();
        newPos.getPanelTile().cells.put(newIndex, cell);
        newPos.getPanelTile().cellDirections.put(newIndex,cellPos.getCellFacing());
        cellPos.getPanelTile().cells.remove(oldIndex);
        cellPos.getPanelTile().cellDirections.remove(oldIndex);

        newPos.getPanelTile().updateNeighborCells(newPos);
        cellPos.getPanelTile().updateNeighborCells(cellPos);

        newPos.getPanelTile().markDirty();
        newPos.getPanelTile().flagSync=true;
        newPos.getPanelTile().flagVoxelShapeUpdate();
        cellPos.getPanelTile().markDirty();
        cellPos.getPanelTile().flagSync=true;
        cellPos.getPanelTile().flagVoxelShapeUpdate();

        if (cell instanceof IObservingPanelCell)
            ((IObservingPanelCell) cell).frontNeighborUpdated(cellPos);

        return true;
    }

    public boolean canExtendTo(PanelCellPos cellPos, Side side, Integer iteration)
    {
        if (iteration>12)return false;

        IPanelCell iPanelCell = cellPos.getIPanelCell();

        //check if a piston has extended into this block
        if (checkCellForPistonExtension(cellPos))
            return false;

        //if this is an empty cell, we know it can be pushed into
        if (iPanelCell==null)
            return true;

        //if this is not a pushable cell, we know we can't extend into it
        if(!iPanelCell.isPushable())
            return false;

        //otherwise, check if we're on the edge of a tile
        PanelCellPos adjacentPos = cellPos.offset(side);
        if (adjacentPos==null)
        {
            //we must be at the edge of the tile and facing some other type of block, so we can't extend
            return false;
        }
        else {
            //we can be pushed and are not on the edge, so check the next cell and see if it can also be pushed
            return adjacentPos.getPanelTile().canExtendTo(adjacentPos,side,iteration+1);
        }
    }

    @Override
    public void rotate(Rotation rotationIn) {


        Map<Integer, IPanelCell> cells = new HashMap<>();
        Map<Integer, Side> cellDirections = new HashMap<>();

        for (Integer i: this.cells.keySet()) {

                PanelCellPos cellPos1 = PanelCellPos.fromIndex(this,i);
                PanelCellPos cellPos2;
                Side side1 = this.cellDirections.get(i);
                Side side2;

                if (rotationIn == Rotation.COUNTERCLOCKWISE_90) {
                    cellPos2 = PanelCellPos.fromRowColumn(this,cellPos1.getColumn(),((cellPos1.getRow() - 4) * -1) + 3, cellPos1.getLevel());
                    side2 = side1.rotateYCCW();
                } else if (rotationIn == Rotation.CLOCKWISE_180) {
                    cellPos2 = PanelCellPos.fromRowColumn(this,((cellPos1.getRow() - 4) * -1) + 3,((cellPos1.getColumn() - 4) * -1) + 3, cellPos1.getLevel());
                    side2 = side1.getOpposite();
                } else {
                    //default rotation 90°
                    cellPos2 = PanelCellPos.fromRowColumn(this,((cellPos1.getColumn() - 4) * -1) + 3,cellPos1.getRow(), cellPos1.getLevel());
                    side2 = side1.rotateYCW();
                }

                cells.put(cellPos2.getIndex(), this.cells.get(i));
                cellDirections.put(cellPos2.getIndex(),side2);

        }

        this.cells = cells;
        this.cellDirections = cellDirections;

        updateSide(Side.FRONT);
        updateSide(Side.RIGHT);
        updateSide(Side.BACK);
        updateSide(Side.LEFT);

        if (!world.isRemote)
            markDirty();

        updateOutputs();

        flagVoxelShapeUpdate();
        flagSync();

    }


    public boolean pingOutwardObservers(EnumFacing facing) {

        boolean updated = false;
        Side side = getSideFromDirection(facing);
        List<Integer> cellIndices = getEdgeCellIndices(side);
        for (Integer i : cellIndices)
            if (cells.containsKey(i) && cells.get(i) instanceof IObservingPanelCell && cellDirections.get(i)==side) {
                if(((IObservingPanelCell)cells.get(i)).frontNeighborUpdated(PanelCellPos.fromIndex(this,i)))
                    updated=true;
            }

        return updated;
    }

    public boolean updateSide(EnumFacing facing) {
        return updateSide(getSideFromDirection(facing));
    }
    public boolean updateSide(Side side){
        boolean updated = false;

        List<Integer> cellIndices = getEdgeCellIndices(side);

        for (Integer i : cellIndices) {
            if (cells.containsKey(i)) {
                if (updateCell(i))
                    updated = true;
            }
        }

        return updated;
    }

    public boolean updateNeighborCells(PanelCellPos cellPos) {
        return updateNeighborCells(cellPos, 1);
    }

    private boolean updateNeighborCells(PanelCellPos cellPos, Integer iteration) {
        List<PanelCellPos> cellPosList = new ArrayList<>();
        boolean updateOutputs = false;

        if (updateNeighbor(cellPos,Side.FRONT,cellPosList))
            updateOutputs=true;
        if (updateNeighbor(cellPos,Side.RIGHT,cellPosList))
            updateOutputs=true;
        if (updateNeighbor(cellPos,Side.BACK,cellPosList))
            updateOutputs=true;
        if (updateNeighbor(cellPos,Side.LEFT,cellPosList))
            updateOutputs=true;
        if (updateNeighbor(cellPos,Side.TOP,cellPosList))
            updateOutputs=true;
        if (updateNeighbor(cellPos,Side.BOTTOM,cellPosList))
            updateOutputs=true;


        if (updateOutputs) {
            updateOutputs();
        }

        boolean updated = false;
        for (PanelCellPos updatePos : cellPosList)
        {
            if(updateCell(updatePos,iteration+1))
                updated=true;
        }
        return updated || updateOutputs;
    }

    private boolean updateNeighbor(PanelCellPos cellPos, Side side, List<PanelCellPos> cellPosList) {
        PanelCellPos neighborPos = cellPos.offset(side);

        //A null neighbor position means we've reached the edge of the panel, so return true that the outputs have changed
        if (neighborPos == null)
            return true;

        //get the cell that's being notified
        IPanelCell adjacentCell = neighborPos.getIPanelCell();
        //if it's an observer, check if it's facing the updated panel and if so let it know
        if (adjacentCell instanceof IObservingPanelCell) {
            Side direction1 = neighborPos.getCellFacing();
            Side direction2 = side.getOpposite();
            if (direction1 == direction2) {
                ((IObservingPanelCell) adjacentCell).frontNeighborUpdated(neighborPos);
            }
        } else if (adjacentCell != null && (!adjacentCell.isIndependentState()||(adjacentCell.needsSolidBase()&&side.getOpposite()==neighborPos.getBaseDirection())))
            //if a component exists within the cell and is either affected by inputs or needs a solid base and it's base changed, add it to the list to be notified
            cellPosList.add(neighborPos);

        return false;
    }

    public boolean checkCellForPistonExtension(PanelCellPos cellPos) {
        for(Side panelSide : new Side[]{Side.FRONT,Side.RIGHT,Side.BACK,Side.LEFT,Side.TOP,Side.BOTTOM})
        {
            PanelCellPos neighborPos = cellPos.offset(panelSide);
            if (neighborPos!=null) {
                IPanelCell cell = neighborPos.getIPanelCell();
                if (cell instanceof Piston && neighborPos.getCellFacing() == panelSide && ((Piston) cell).isExtended())
                    return true;
            }
        }
        return false;
    }

    protected boolean updateCell(Integer cellIndex) {
        PanelCellPos cellPos = PanelCellPos.fromIndex(this,cellIndex);
        return updateCell(cellPos, 1);
    }
    protected boolean updateCell(PanelCellPos cellPos) {
        return updateCell(cellPos, 1);
    }
    /**
     * Update a cell with a potential input change
     *
     * @param cellPos position of cell to update
     * @return true if this update caused the panel output to change
     */
    private boolean updateCell(PanelCellPos cellPos, int iteration) {

        boolean change = false;
        if (iteration > (16 * Config.CIRCUIT_MAX_ITERATION)) {
            if (!this.flagOverflow)
                TinyRedstone.LOGGER.warn("Redstone panel at " + pos.getX() + "," + pos.getY() + "," + pos.getZ() + " iterated too many times.");
            this.flagOverflow=true;
            return false;
        }

        //if this cell position is on a different panel than this one, call this method on that panel
        if (cellPos.getPanelTile()!=this)
            return cellPos.getPanelTile().updateCell(cellPos,iteration);

        IPanelCell thisCell = cellPos.getIPanelCell();
        //if there is a cell at this position, notify it of the update and process any of its updates
        if (thisCell != null) {
            //if the cell needs a solid base, check if its base has been removed
            if (thisCell.needsSolidBase()) {
                Side baseDirection = cellPos.getBaseDirection();
                PanelCellPos basePos = cellPos.offset(baseDirection);
                if (basePos != null && (basePos.getIPanelCell() == null || (!basePos.getIPanelCell().isPushable()) && !(basePos.getIPanelCell() instanceof Piston && basePos.getCellFacing()==cellPos.getBaseDirection().getOpposite() ) )) {
                    Registration.REDSTONE_PANEL_BLOCK.removeCell(cellPos, null);
                    change = true;
                }
            }

            //if change is still false (cell hasn't been removed, and this cell is affected by input changes
            //notify the cell of the input change and check for output changes
            if (!change && !thisCell.isIndependentState() && thisCell.neighborChanged(cellPos)) {
                //update neighbors if this cell output changed
                updateNeighborCells(cellPos, iteration + 1);
                if (thisCell instanceof RedstoneDust) {
                    PanelCellPos above = cellPos.offset(Side.TOP), below = cellPos.offset(Side.BOTTOM);
                    if (above != null)
                        updateNeighborCells(above, iteration + 1);
                    if (below != null)
                        updateNeighborCells(below, iteration + 1);
                }
                change = true;
            }
        }


        if (change) {
            this.markDirty();
            flagSync();
        }
        return change;
    }

    private Side getPanelCellSide(PanelCellPos cellPos, EnumFacing facing) {
        return getPanelCellSide(cellPos,getSideFromDirection(facing));
    }

    /**
     * Get which side of the cell is facing the given side of the panel
     * @param cellPos position of cell being queried
     * @param panelSide the side of the redstone panel
     * @return the side of the cell that is facing the given side of the panel
     */
    public Side getPanelCellSide(PanelCellPos cellPos,Side panelSide)
    {
        Side cellDirection = cellPos.getPanelTile().getCellFacing(cellPos);

        if (cellDirection == null) return null;

        if (cellDirection==panelSide)
            return Side.FRONT;
        if ((panelSide==Side.TOP||panelSide==Side.BOTTOM)&&cellDirection!=Side.TOP&&cellDirection!=Side.BOTTOM)
            return panelSide;
        if (cellDirection==panelSide.getOpposite())
            return Side.BACK;
        if (cellDirection==panelSide.rotateYCW())
            return Side.LEFT;
        if (cellDirection==panelSide.rotateYCCW())
            return Side.RIGHT;
        if (cellDirection==panelSide.rotateForward())
            return Side.TOP;
        if (cellDirection==panelSide.rotateBack())
            return Side.BOTTOM;
        if (cellDirection==Side.TOP || cellDirection==Side.BOTTOM){
            return panelSide;
        }
        return null;
    }

    /**
     * Recalculate the outputs of the tile. Will sync to client and return true if any output changes
     * @return boolean true if any outputs have changed
     */
    public boolean updateOutputs() {
        boolean change = false;
        int weak, strong, wire;

        List<EnumFacing> directionsUpdated = new ArrayList<>();

        //check edge cells
        Side[] sides;
        if (hasBase())
            sides = new Side[]{Side.FRONT,Side.RIGHT,Side.BACK,Side.LEFT,Side.TOP};
        else
            sides = new Side[]{Side.FRONT,Side.RIGHT,Side.BACK,Side.LEFT,Side.TOP,Side.BOTTOM};

        for (Side panelSide : sides) {
            EnumFacing direction = getDirectionFromSide(panelSide);
            IBlockState neighborBlockState = world.getBlockState(pos.offset(direction));
            boolean neighborIsWire = PanelCellNeighbor.blockIsRedstoneWire(neighborBlockState.getBlock());
            weak=0;strong=0;wire=0;
            List<Integer> indices = getEdgeCellIndices(direction);
            for (int i:indices) {
                PanelCellPos cellPos = PanelCellPos.fromIndex(this,i);
                IPanelCell cell = cellPos.getIPanelCell();
                Side side = getPanelCellSide(cellPos, direction);
                int cellStrongOutput = ((!neighborIsWire && cell instanceof TinyBlock)||cell instanceof RedstoneDust)?0:cell.getStrongRsOutput(side);
                int cellWeakOutput = cell.getWeakRsOutput(side);
                int cellWireOutput = (cell instanceof RedstoneDust)?cellWeakOutput:0;

                if (neighborIsWire && cell.powerDrops()) {
                    cellStrongOutput -= 1;
                    cellWeakOutput -= 1;
                    cellWireOutput -= 1;
                }

                if (neighborIsWire && cell instanceof TinyBlock) {
                    cellWeakOutput = cellStrongOutput;
                }

                if (cellStrongOutput > strong) {
                    strong = cellStrongOutput;
                }
                if (cellWeakOutput > weak) {
                    weak = cellWeakOutput;
                }
                if (cellWireOutput > wire)
                    wire = cellWireOutput;
            }

            if (strongPowerToNeighbors.get(panelSide) == null || strong != strongPowerToNeighbors.get(panelSide) ||
                    weakPowerToNeighbors.get(panelSide) == null || weak != weakPowerToNeighbors.get(panelSide) ||
                    wirePowerToNeighbors.get(panelSide) == null || wire != wirePowerToNeighbors.get(panelSide)
            ) {
                change = true;
                strongPowerToNeighbors.put(panelSide, strong);
                weakPowerToNeighbors.put(panelSide, weak);
                wirePowerToNeighbors.put(panelSide,wire);
                directionsUpdated.add(direction);
            }

        }

        int ll = 0;
        for(Integer index : cells.keySet())
        {
            ll+=cells.get(index).lightOutput();
        }
        if (ll!=this.lightOutput) {
            this.lightOutput=ll;
            this.flagLightUpdate=true;
        }

        if (change)
        {
            flagSync();
            world.notifyNeighborsOfStateChange(pos,this.getBlockState().getBlock(),true);
            for (EnumFacing direction : directionsUpdated) {
                BlockPos neighborPos = pos.offset(direction);
                IBlockState neighborBlockState = world.getBlockState(neighborPos);
                if (neighborBlockState!=null && neighborBlockState.isOpaqueCube())
                    world.notifyNeighborsOfStateChange(neighborPos,neighborBlockState.getBlock(),true);
            }
        }

        return change;
    }

    /**
     * Should neighboring blocks be checking for wire signals (from tiny redstone dust) when asking for direct signals?
     * This is similar to vanilla redstone dust's "shouldSignal" flag which suppresses signal reading from redstone dust
     * while another redstone dust is querying the inputs into a neighboring block.
     * @return true if wire signals should be included when a neighbor asks for "direct" signal strength
     */
    public static boolean getCheckWireSignals(){
        return checkWireSignals;
    }

    private List<Integer> getEdgeCellIndices(EnumFacing edge) {
        return getEdgeCellIndices(getSideFromDirection(edge));
    }
    private List<Integer> getEdgeCellIndices(Side side){

        List<Integer> cellIndices = new ArrayList<>();
        boolean hasBase = hasBase();

        for (int i1 = 0 ; i1<(hasBase?447:511) ; i1+=64) {
            if (side == Side.LEFT) {
                for (int i = i1; i < i1+8; i++) {
                    if (cells.containsKey(i)) {
                        cellIndices.add(i);
                    }
                }
            } else if (side == Side.FRONT) {
                for (int i = i1; i < i1+64; i += 8) {
                    if (cells.containsKey(i)) {
                        cellIndices.add(i);
                    }
                }
            } else if (side == Side.RIGHT) {
                for (int i = i1+56; i < i1+64; i++) {
                    if (cells.containsKey(i)) {
                        cellIndices.add(i);
                    }
                }
            } else if (side == Side.BACK) {
                for (int i = i1+7; i < i1+64; i += 8) {
                    if (cells.containsKey(i)) {
                        cellIndices.add(i);
                    }
                }
            }
        }
        if (side==Side.TOP) {
            for (int i = (hasBase ? 384 : 448); i < (hasBase ? 448 : 512); i++) {
                if (cells.containsKey(i)) {
                    cellIndices.add(i);
                }
            }
        }else if (side==Side.BOTTOM)
        {
            for (int i = 0; i < 64; i++) {
                if (cells.containsKey(i)) {
                    cellIndices.add(i);
                }
            }
        }

        return cellIndices;
    }


    public int getColor() {
        if (this.Color == null) {
            return EnumDyeColor.GRAY.getColorValue();
        }
        return this.Color;
    }

    public int getLightOutput()
    {
        if(panelCover==null || panelCover.allowsLightOutput())
            return this.lightOutput;
        return 0;
    }

    public void sync()
    {
        if (!world.isRemote && (!isCovered() || panelCover.allowsLightOutput()))
            // PORT-TODO: 1.16 used Constants.BlockFlags.BLOCK_UPDATE (==3). 1.12.2 Forge has no Constants.BlockFlags,
            // so use the raw vanilla flag 3 (1=notify neighbors | 2=send to client) for notifyBlockUpdate.
            this.world.notifyBlockUpdate(pos,this.getBlockState(),this.getBlockState(), 3);
    }

    protected void handleCrash(Exception e)
    {
        this.flagCrashed=true;
        sync();
        TinyRedstone.LOGGER.error("Redstone Panel Crashed at " + pos.getX() + "," + pos.getY() + "," + pos.getZ(),e);
    }

    public boolean isCrashed()
    {
        return this.flagCrashed;
    }
    public void resetCrashFlag()
    {
        this.flagCrashed=false;
    }

    public boolean isOverflown()
    {
        return this.flagOverflow;
    }
    public void resetOverflownFlag()
    {
        this.flagOverflow=false;
    }
    public boolean isCovered()
    {
        return panelCover!=null;
    }

    protected EnumFacing getDirectionFromSide(Side side) {
        // PORT-TODO: 1.16 used BlockStateProperties.FACING (a Direction property). 1.12.2 has no BlockStateProperties.
        // We use vanilla BlockDirectional.FACING (the standard 1.12.2 facing PropertyDirection). PanelBlock MUST
        // declare the SAME property when it is converted, or these getValue() calls will throw.
        EnumFacing facing = getBlockState().getValue(net.minecraft.block.BlockDirectional.FACING);
        switch (side) {
            case FRONT:
                switch (facing) {
                    case DOWN:
                    case EAST:
                    case WEST:
                        return EnumFacing.NORTH;
                    case UP:
                        return EnumFacing.SOUTH;
                    case SOUTH:
                        return EnumFacing.DOWN;
                    case NORTH:
                        return EnumFacing.UP;
                }
                break;
            case RIGHT:
                switch (facing) {
                    case DOWN:
                    case NORTH:
                    case SOUTH:
                    case UP:
                        return EnumFacing.EAST;
                    case EAST:
                        return EnumFacing.UP;
                    case WEST:
                        return EnumFacing.DOWN;
                }
                break;
            case BACK:
                switch (facing) {
                    case DOWN:
                    case EAST:
                    case WEST:
                        return EnumFacing.SOUTH;
                    case UP:
                        return EnumFacing.NORTH;
                    case SOUTH:
                        return EnumFacing.UP;
                    case NORTH:
                        return EnumFacing.DOWN;
                }
                break;
            case LEFT:
                switch (facing) {
                    case DOWN:
                    case NORTH:
                    case SOUTH:
                    case UP:
                        return EnumFacing.WEST;
                    case EAST:
                        return EnumFacing.DOWN;
                    case WEST:
                        return EnumFacing.UP;
                }
            case TOP:
                return facing.getOpposite();
            case BOTTOM:
                return facing;
        }
        //this should not be reached
        return null;
    }

    public Side getSideFromDirection(EnumFacing direction) {
        EnumFacing facing = getBlockState().getValue(net.minecraft.block.BlockDirectional.FACING);
        switch (direction){
            case NORTH:
                switch (facing) {
                    case DOWN:
                    case EAST:
                    case WEST:
                        return Side.FRONT;
                    case UP:
                        return Side.BACK;
                    case NORTH:
                        return Side.BOTTOM;
                    case SOUTH:
                        return Side.TOP;
                }
                break;
            case EAST:
                switch (facing){
                    case DOWN:
                    case NORTH:
                    case SOUTH:
                    case UP:
                        return Side.RIGHT;
                    case EAST:
                        return Side.BOTTOM;
                    case WEST:
                        return Side.TOP;
                }
                break;
            case SOUTH:
                switch (facing){
                    case UP:
                        return Side.FRONT;
                    case DOWN:
                    case EAST:
                    case WEST:
                        return Side.BACK;
                    case SOUTH:
                        return Side.BOTTOM;
                    case NORTH:
                        return Side.TOP;
                }
                break;
            case WEST:
                switch (facing){
                    case UP:
                    case DOWN:
                    case NORTH:
                    case SOUTH:
                        return Side.LEFT;
                    case WEST:
                        return Side.BOTTOM;
                    case EAST:
                        return Side.TOP;
                }
                break;
            case UP:
                switch (facing){
                    case NORTH:
                        return Side.FRONT;
                    case EAST:
                        return Side.RIGHT;
                    case SOUTH:
                        return Side.BACK;
                    case WEST:
                        return Side.LEFT;
                    case UP:
                        return Side.BOTTOM;
                    case DOWN:
                        return Side.TOP;
                }
                break;
            case DOWN:
                switch (facing){
                    case SOUTH:
                        return Side.FRONT;
                    case WEST:
                        return Side.RIGHT;
                    case NORTH:
                        return Side.BACK;
                    case EAST:
                        return Side.LEFT;
                    case DOWN:
                        return Side.BOTTOM;
                    case UP:
                        return Side.TOP;
                }
        }


        return null;
    }

    protected EnumFacing getPlayerDirectionFacing(EntityPlayer player, boolean allowVertical){

        EnumFacing panelFacing = getBlockState().getValue(net.minecraft.block.BlockDirectional.FACING);

        // 1.16 Direction.orderedByNearest(entity) has no 1.12.2 equivalent. Reproduce its intent:
        // order the 6 facings by alignment with the player's look vector (most-aligned first).
        final Vec3d look = player.getLookVec();
        EnumFacing[] playerFacings = EnumFacing.values().clone();
        java.util.Arrays.sort(playerFacings, (a, b) -> {
            double da = a.getFrontOffsetX() * look.x + a.getFrontOffsetY() * look.y + a.getFrontOffsetZ() * look.z;
            double db = b.getFrontOffsetX() * look.x + b.getFrontOffsetY() * look.y + b.getFrontOffsetZ() * look.z;
            return Double.compare(db, da);
        });
        for(EnumFacing facing : playerFacings)
        {
            if (allowVertical || (facing!=panelFacing && facing!=panelFacing.getOpposite()))
                return facing;
        }
        return  player.getHorizontalFacing();

    }

    public boolean hasBase()
    {
        if(this.getBlockState().getPropertyKeys().contains(Registration.HAS_PANEL_BASE)
                && !this.getBlockState().getValue(Registration.HAS_PANEL_BASE))
            return false;
        return true;
    }


    @CheckForNull
    public IPanelCell getIPanelCell(PanelCellPos cellPos){
        if (cellPos==null)return null;
        return this.cells.get(cellPos.getIndex());
    }

    @CheckForNull
    public Side getCellFacing(PanelCellPos cellPos){
        if (cellPos==null)return null;
        return this.cellDirections.get(cellPos.getIndex());
    }

    public List<PanelCellPos> getCellPositions()
    {
        List<PanelCellPos> poss = new ArrayList<>();
        for (Integer index : cells.keySet())
        {
            poss.add(PanelCellPos.fromIndex(this,index));
        }
        return poss;
    }

    public int getCellCount()
    {
        return cells.size();
    }

    public void removeCell(PanelCellPos cellPos)
    {
        if (cellPos.getIPanelCell() != null) {
            int cellIndex = cellPos.getIndex();
            boolean isRedstoneDust = cellPos.getIPanelCell() instanceof RedstoneDust;

            cellPos.getIPanelCell().onRemove(cellPos);

            //remove from panel
            cellDirections.remove(cellIndex);
            cells.remove(cellIndex);

            BlockPos pos = cellPos.getPanelTile().getPos();
            // PORT-TODO: 1.16 playLocalSound(x,y,z,SoundEvent,cat,vol,pitch,distanceDelay) -> 1.12.2
            // World.playSound(EntityPlayer, x,y,z, SoundEvent, cat, vol, pitch). Passing null player plays for
            // everyone in range. SoundEvents.STONE_BREAK -> SoundEvents.BLOCK_STONE_BREAK in 1.12.2.
            cellPos.getPanelTile().getLevel().playSound(
                    null,
                    pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.BLOCK_STONE_BREAK,
                    SoundCategory.BLOCKS, 0.15f, 2f
            );

            //let neighbors know about vacancy
            updateNeighborCells(cellPos);
            //for tiny redstone dust, alert the whole neighborhood
            if (isRedstoneDust)
            {
                PanelCellPos above = cellPos.offset(Side.TOP),
                        below = cellPos.offset(Side.BOTTOM);
                if (above!=null)
                    updateNeighborCells(above);
                if (below!=null)
                    updateNeighborCells(below);
            }

            flagVoxelShapeUpdate();
            flagSync();
        }
    }

    public void removeAllCells(@Nullable EntityPlayer player){
        Object[] indices = cells.keySet().toArray();
        for (Object index : indices){
            ((PanelBlock)this.getBlockState().getBlock()).removeCell(PanelCellPos.fromIndex(this,(Integer) index),player);
        }
    }

    public void addCell(PanelCellPos cellPos,IPanelCell panelCell, Side facing, EntityPlayer player)
    {
        int cellIndex = cellPos.getIndex();
        cellDirections.put(cellIndex, facing);
        cells.put(cellIndex, panelCell);

        BlockPos pos = cellPos.getPanelTile().getPos();
        cellPos.getPanelTile().getLevel().playSound(
                null,
                pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.BLOCK_STONE_PLACE,
                SoundCategory.BLOCKS, 0.15f, 2f
        );

        panelCell.onPlace(cellPos,player);
        updateNeighborCells(cellPos);
        flagVoxelShapeUpdate();
    }

    public boolean hasCellsOnFace(EnumFacing direction)
    {
        return !getEdgeCellIndices(direction).isEmpty();
    }
    public void flagSync()
    {
        this.flagSync=true;
    }

    // 1.12.2 helper: Block.box used 0..16 pixel-space; AxisAlignedBB is 0..1 block-space.
    private static AxisAlignedBB box16(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1/16d, y1/16d, z1/16d, x2/16d, y2/16d, z2/16d);
    }
    // 1.12.2 has no VoxelShapes.or (disjoint union); AxisAlignedBB.union returns the encompassing box.
    private static AxisAlignedBB union(AxisAlignedBB a, AxisAlignedBB b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.union(b);
    }

    public AxisAlignedBB getVoxelShape(){

        if (voxelShape==null) {
            if (isCovered()) {
                AxisAlignedBB coverShape = panelCover.getShape();
                boolean fullBlock = coverShape.minX <= 0 && coverShape.minY <= 0 && coverShape.minZ <= 0
                        && coverShape.maxX >= 1 && coverShape.maxY >= 1 && coverShape.maxZ >= 1;
                if (fullBlock || this.getBlockState().getValue(net.minecraft.block.BlockDirectional.FACING) == EnumFacing.DOWN)
                    voxelShape = coverShape;
                else {
                    double minX = coverShape.minX, minY = coverShape.minY, minZ = coverShape.minZ;
                    double maxX = coverShape.maxX, maxY = coverShape.maxY, maxZ = coverShape.maxZ;
                    switch (this.getBlockState().getValue(net.minecraft.block.BlockDirectional.FACING)) {
                        case UP:
                            voxelShape = new AxisAlignedBB(minX, 1 - maxY, 1 - maxZ, maxX, 1 - minY, 1 - minZ);
                            break;
                        case NORTH:
                            voxelShape = new AxisAlignedBB(minX, 1 - maxZ, minY, maxX, 1 - minZ, maxY);
                            break;
                        case EAST:
                            voxelShape = new AxisAlignedBB(1 - maxY, minX, minZ, 1 - minY, maxX, maxZ);
                            break;
                        case SOUTH:
                            voxelShape = new AxisAlignedBB(minX, minZ, 1 - maxY, maxX, maxZ, 1 - minY);
                            break;
                        case WEST:
                            voxelShape = new AxisAlignedBB(minY, 1 - maxX, minZ, maxY, 1 - minX, maxZ);
                            break;
                    }
                }
            } else {
                if (hasBase()) {
                    switch (this.getBlockState().getValue(net.minecraft.block.BlockDirectional.FACING)) {
                        case UP:
                            voxelShape = box16(0, 14, 0, 16, 16, 16);
                            break;
                        case NORTH:
                            voxelShape = box16(0, 0, 0, 16, 16, 2);
                            break;
                        case EAST:
                            voxelShape = box16(14, 0, 0, 16, 16, 16);
                            break;
                        case SOUTH:
                            voxelShape = box16(0, 0, 14, 16, 16, 16);
                            break;
                        case WEST:
                            voxelShape = box16(0, 0, 0, 2, 16, 16);
                            break;
                        default: //DOWN
                            voxelShape = box16(0, 0, 0, 16, 2, 16);
                    }
                } else if (cells.isEmpty()||isCrashed())
                {
                    voxelShape = box16(0,0,0,16,0.1,16);
                } else //we have no base, but we do have cells
                    voxelShape = null;

                for (Integer index : cells.keySet()) {
                    PanelCellPos cellPos = PanelCellPos.fromIndex(this, index);
                    AxisAlignedBB cellVoxelShape = getCellVoxelShape(cellPos);
                    if (cellVoxelShape != null)
                        voxelShape = union(voxelShape, cellVoxelShape);
                }
                if (voxelShape == null) //no base, cells produced no shape
                    voxelShape = box16(0,0,0,16,0.1,16);
            }
        }
        return voxelShape;
    }

    @CheckForNull
    public AxisAlignedBB getCellVoxelShape(PanelCellPos cellPos)
    {
        if (cellPos!=null)
        {
            IPanelCell cell = cellPos.getIPanelCell();
            if (cell != null) {
                AxisAlignedBB voxelShape = null;
                EnumFacing thisFacing = this.getBlockState().getValue(net.minecraft.block.BlockDirectional.FACING);
                PanelCellVoxelShape[] cellShapes = cell.getShapes(cellPos);
                for (PanelCellVoxelShape cellShape : cellShapes) {
                    float rowStart = cellPos.getRow() * 2f + (float) cellShape.getPoint1().x * 2f;
                    float rowEnd = cellPos.getRow() * 2f + (float) cellShape.getPoint2().x * 2f;
                    float columnStart = cellPos.getColumn() * 2f + (float) cellShape.getPoint1().z * 2f;
                    float columnEnd = cellPos.getColumn() * 2f + (float) cellShape.getPoint2().z * 2f;
                    float levelStart = ((hasBase()) ? 2 : 0) + cellPos.getLevel() * 2f + (float) cellShape.getPoint1().y * 2f;
                    float levelEnd = ((hasBase()) ? 2 : 0) + cellPos.getLevel() * 2f + (float) cellShape.getPoint2().y * 2f;

                    switch (thisFacing) {
                        case UP:
                            voxelShape = union(voxelShape, box16(rowStart, 16 - levelEnd, 16 - columnEnd, rowEnd, 16 - levelStart, 16 - columnStart));
                            break;
                        case NORTH:
                            voxelShape = union(voxelShape, box16(rowStart, 16 - columnEnd, levelStart, rowEnd, 16 - columnStart, levelEnd));
                            break;
                        case EAST:
                            voxelShape = union(voxelShape, box16(16 - levelEnd, rowStart, columnStart, 16 - levelStart, rowEnd, columnEnd));
                            break;
                        case SOUTH:
                            voxelShape = union(voxelShape, box16(rowStart, columnStart, 16 - levelEnd, rowEnd, columnEnd, 16 - levelStart));
                            break;
                        case WEST:
                            voxelShape = union(voxelShape, box16(levelStart, 16 - rowEnd, columnStart, levelEnd, 16 - rowStart, columnEnd));
                            break;
                        default: //DOWN
                            voxelShape = union(voxelShape, box16(rowStart, levelStart, columnStart, rowEnd, levelEnd, columnEnd));
                    }
                }
                return voxelShape;
            }
        }
        return null;
    }

    public void flagVoxelShapeUpdate()
    {
        voxelShape=null;
    }

    public void onBlockDestroy() {
        for (Integer index : cells.keySet()) {
            PanelCellPos pos = PanelCellPos.fromIndex(this, index);
            cells.get(index).onRemove(pos);
        }
    }

    // PORT-TODO: 1.16 returned BlockRayTraceResult; 1.12.2 has only the unified RayTraceResult (no separate
    // BlockRayTraceResult). Callers (PanelCellPos.fromHitVec, compat/hwyla) must accept RayTraceResult once
    // they are converted. xRot/yRot/getEyePosition/level.clip(RayTraceContext) -> 1.12.2 rotationPitch/
    // rotationYaw/getPositionEyes/world.rayTraceBlocks below.
    public RayTraceResult getPlayerCollisionHitResult(EntityPlayer player) {
        float xRotation = player.rotationPitch;
        float yRotation = player.rotationYaw;
        Vec3d eyePosition = player.getPositionEyes(1f);
        float v = -MathHelper.cos(-xRotation * ((float)Math.PI / 180F));
        float x = (MathHelper.sin(-yRotation * ((float)Math.PI / 180F) - (float)Math.PI)) * v;
        float y = MathHelper.sin(-xRotation * ((float)Math.PI / 180F));
        float z = (MathHelper.cos(-yRotation * ((float)Math.PI / 180F) - (float)Math.PI)) * v;
        // PORT-TODO: 1.16 used ForgeMod.REACH_DISTANCE attribute (default 5). 1.12.2 has no reach-distance
        // attribute; use a fixed reach of 5.0 (vanilla survival reach). Adjust if a creative-reach path is needed.
        double reachDistance = 5.0D;
        Vec3d vec31 = eyePosition.addVector((double)x * reachDistance, (double)y * reachDistance, (double)z * reachDistance);
        return world.rayTraceBlocks(eyePosition, vec31, false, true, false);
    }
}
