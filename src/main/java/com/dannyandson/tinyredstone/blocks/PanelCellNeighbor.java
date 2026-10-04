package com.dannyandson.tinyredstone.blocks;

import com.dannyandson.tinyredstone.Config;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.panelcells.TinyBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class PanelCellNeighbor {
    private IPanelCell iPanelCell=null;
    private BlockPos blockPos=null;
    private final Side neighborDirection;
    private Side neighborsSide =null;
    private final PanelTile panelTile;
    private PanelCellPos cellPos;
    private IBlockState blockState;

    /**
     * Construct a PanelCellNeighbor object for querying an IPanelCell
     * @param cellPos Cell position of the neighbor
     * @param panelCell IPanelCell of the neighbor
     * @param neighborsSide Side of the neighbor facing the querying cell.
     * @param neighborDirection Direction of the neighbor from the querying cell relative to the panel facing
     */
    PanelCellNeighbor(PanelCellPos cellPos, IPanelCell panelCell, Side neighborsSide, Side neighborDirection)
    {
        this.iPanelCell=panelCell;
        this.panelTile= cellPos.getPanelTile();
        this.neighborsSide =neighborsSide;
        this.cellPos=cellPos;
        this.neighborDirection =neighborDirection;
    }

    /**
     * Construct a PanelCellNeighbor object for querying a block in the world
     * @param panelTile PanelTile of the querying cell
     * @param blockPos Position of the neighbor block
     * @param neighborDirection Direction of the neighbor block relative to the panel facing
     */
    PanelCellNeighbor(PanelTile panelTile,BlockPos blockPos,Side neighborDirection)
    {
        this.blockPos=blockPos;
        this.panelTile=panelTile;
        this.neighborDirection = neighborDirection;
    }

    /**
     * Gets redstone output of the facing side of the cell
     *
     * @return integer 0-15 indicating the strength of redstone signal
     */
    public int getWeakRsOutput() {
        if (iPanelCell!=null){
            return iPanelCell.getWeakRsOutput(neighborsSide);
        }
        else if (blockPos!=null)
        {
            return panelTile.getLevel().getRedstonePower(blockPos,panelTile.getDirectionFromSide(neighborDirection));
        }
        return 0;
    }

    public int getStrongRsOutput() {
        if (iPanelCell!=null && !(iPanelCell instanceof TinyBlock)){
            return iPanelCell.getStrongRsOutput(neighborsSide);
        }
        else if (blockPos!=null)
        {
            return panelTile.getLevel().getStrongPower(blockPos,panelTile.getDirectionFromSide(neighborDirection));
        }
        return 0;
    }

    public int getStrongRsOutputForWire()
    {
        if (iPanelCell!=null){
            return iPanelCell.getStrongRsOutput(neighborsSide);
        }
        else if (blockPos!=null) {
            //when checking if a redstone wire (tiny redstone dust) should pick up a signal from a block in the world
            //first check that block's strong (direct) signal output.
            int signal = getStrongRsOutput();

            //if the strong signal is not a full signal, check if the neighbor is a redstone wire
            // (vanilla redstone dust or a similarly behaving modded block).
            //If so, get its weak (indirect) signal.
            if (signal<15 && blockIsRedstoneWire(getNeighborBlockState().getBlock(),false))
                signal = getWeakRsOutput();

            //if we still don't have a full redstone signal, check if the neighbor is a redstone conductor
            //such as a full block that can carry a redstone signal.
            //If so, check its inputs and see if any are strong (direct) signals such as those from a repeater.
            IBlockState neighborState = getNeighborBlockState();
            if (signal<15 && neighborState.getBlock().isNormalCube(neighborState,panelTile.getLevel(),blockPos)) {
                //Tell redstone panels not to include redstone wire signals when asked for direct signals.
                //This is similar to vanilla redstone dust's "shouldSignal" flag.
                PanelTile.checkWireSignals = false;
                //Check all this neighbor's neighbors to see if any are providing direct signals
                for (EnumFacing direction : new EnumFacing[]{EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST}) {
                    BlockPos neighborNeighborPos = blockPos.offset(direction);
                    IBlockState neighborNeighborState = panelTile.getLevel().getBlockState(neighborNeighborPos);
                    Block neighborNeighborBlock = neighborNeighborState.getBlock();
                    if (neighborNeighborBlock != Blocks.REDSTONE_WIRE) {
                        signal = Math.max(signal, panelTile.getLevel().getStrongPower(neighborNeighborPos, direction));
                    }
                    if (signal >= 15) return signal;
                }
                //Re-enable inclusion of wire signals
                PanelTile.checkWireSignals = true;
            }
            return signal;
        }
        return 0;
    }

    public boolean hasComparatorOverride()
    {
        if (blockPos!=null) {
            IBlockState state = getNeighborBlockState();
            return state.getBlock().hasComparatorInputOverride(state);
        }
        return false;
    }

    public int getComparatorOverride()
    {
        if (blockPos!=null && hasComparatorOverride()) {
            IBlockState state = getNeighborBlockState();
            return state.getBlock().getComparatorInputOverride(state, panelTile.getLevel(), blockPos);
        }
        return 0;
    }

    /**
     * Does the power level drop when transmitting between cells/blocks (such as with redstone dust)?
     *
     * @return true if power level should drop, false if not
     */
    public boolean powerDrops() {
        if (iPanelCell!=null)
            return iPanelCell.powerDrops();
        if(blockPos!=null) {
            return blockIsRedstoneWire(getNeighborBlockState().getBlock());
        }
        return false;
    }

    public boolean isPushable()
    {
        return panelTile.canExtendTo(this.cellPos,this.neighborDirection,0);
    }

    /**
     * Is this neighbor on a Redstone Panel
     * @return True if this neighbor is on a panel, false if it is a block in the world (you are an edge cell)
     */
    public boolean isOnPanel()
    {
        return this.cellPos!=null;
    }

    public boolean canConnectRedstone()
    {
        IBlockState blockState = getNeighborBlockState();
        if (blockState!=null && !blockIsRedstoneWire(getNeighborBlockState().getBlock(),false))
            return blockState.getBlock().canConnectRedstone(blockState,panelTile.getLevel(),this.blockPos,panelTile.getDirectionFromSide(neighborDirection));
        if (iPanelCell!=null && !(iPanelCell instanceof TinyBlock) && !(iPanelCell.powerDrops()))
            return true;
        return false;
    }

    /**
     * Gets the neighboring cell if neighbor is an iPanelCell
     * @return Either an IPanelCell instance or null if no neighbor is not an IPanelCell
     */
    public IPanelCell getNeighborIPanelCell() {
        return iPanelCell;
    }
    /**
     * Gets the neighboring BlockState if the neighbor is a block
     * @return Either a BlockState or null if neighbor is not a block
     */
    public IBlockState getNeighborBlockState()
    {
        if (blockPos!=null)
        {
            if (blockState==null)
                blockState=panelTile.getLevel().getBlockState(blockPos);
            return blockState;
        }
        return null;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public PanelCellPos getCellPos() {
        return cellPos;
    }

    public Side getNeighborsSide() {
        return neighborsSide;
    }

    public static boolean blockIsRedstoneWire(Block block){
        return blockIsRedstoneWire(block,true);
    }
    public static boolean blockIsRedstoneWire(Block block, boolean includeVanilla){
        if (includeVanilla && block == Blocks.REDSTONE_WIRE) return true;
        return Config.REDSTONE_WIRE_LIST.contains(block.getRegistryName().toString());
    }


}
