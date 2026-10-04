package com.dannyandson.tinyredstone.blocks;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.IColorablePanelCell;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.api.IPanelCover;
import com.dannyandson.tinyredstone.blocks.panelcells.RedstoneDust;
import com.dannyandson.tinyredstone.gui.ClearPanelGUI;
import com.dannyandson.tinyredstone.gui.PanelCrashGUI;
import com.dannyandson.tinyredstone.gui.TinyBlockGUI;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.util.EnumFacing.*;

public class PanelBlock extends Block {

    // PORT-TODO: 1.16 FACING was BlockStateProperties.FACING (all 6 directions). The shared
    // conversion contract mandates a HORIZONTAL PropertyDirection here. Vertical placement
    // (panel facing UP/DOWN) is therefore not representable in the blockstate; placement on
    // floor/ceiling falls through to NORTH below. Reconcile if vertical panels are needed.
    // Use the vanilla all-6-direction facing property (the SAME object PanelTile reads via
    // BlockDirectional.FACING) so blockstate lookups don't throw a property-mismatch. Panels
    // can sit on floor (UP), ceiling (DOWN) or walls (horizontal), so all 6 are needed.
    public static final PropertyDirection FACING = net.minecraft.block.BlockDirectional.FACING;

    private static final Map<EnumFacing,AxisAlignedBB> BASE = new HashMap<>();
    static{
        // AxisAlignedBB uses 0..1 block-space (1.16 Block.box used 0..16 pixel-space).
        BASE.put(UP ,
                new AxisAlignedBB(0/16d, 16/16d, 0/16d, 16/16d, 14/16d, 16/16d)
        );
        BASE.put(DOWN ,
                new AxisAlignedBB(0/16d, 0/16d, 0/16d, 16/16d, 2/16d, 16/16d)
        );
        BASE.put(NORTH ,
                new AxisAlignedBB(0/16d, 0/16d, 0/16d, 16/16d, 16/16d, 2/16d)
        );
        BASE.put(EAST ,
                new AxisAlignedBB(16/16d, 0/16d, 0/16d, 14/16d, 16/16d, 16/16d)
        );
        BASE.put(SOUTH ,
                new AxisAlignedBB(0/16d, 0/16d, 16/16d, 16/16d, 16/16d, 14/16d)
        );
        BASE.put(WEST ,
                new AxisAlignedBB(0/16d, 0/16d, 0/16d, 2/16d, 16/16d, 16/16d)
        );
    }

    private static final Map<Item, Class<? extends IPanelCell>> itemPanelCellMap = new HashMap<>();
    private static final Map<Class<? extends IPanelCell>, Item> panelCellItemMap = new HashMap<>();
    private static final Map<Item, Class<? extends IPanelCover>> itemPanelCoverMap = new HashMap<>();
    private static final Map<Class<? extends IPanelCover>, Item> panelCoverItemMap = new HashMap<>();

    public PanelBlock() {
        super(Material.ROCK);
        setHardness(2.0f);
        setResistance(2.0f);
        setSoundType(SoundType.STONE);
        // PORT-TODO: setRegistryName/setTranslationKey/setCreativeTab are set by Registration
        // (matching the original 1.16 DeferredRegister behavior, which registered name there).
        setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, NORTH)
                .withProperty(Registration.HAS_PANEL_BASE, false));
    }

    public Class<? extends IPanelCell> getIPanelCellByItem(Item item) {
        return itemPanelCellMap.get(item);
    }

    public Item getItemByIPanelCell(Class<? extends IPanelCell> panelCell) {
        return panelCellItemMap.get(panelCell);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new PanelTile();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, Registration.HAS_PANEL_BASE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        // bits 0-2: facing index (0-5: down,up,north,south,west,east); bit 3: has base
        EnumFacing facing = EnumFacing.getFront(meta & 7);
        boolean hasBase = (meta & 8) != 0;
        return getDefaultState().withProperty(FACING, facing).withProperty(Registration.HAS_PANEL_BASE, hasBase);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getIndex();
        if (state.getValue(Registration.HAS_PANEL_BASE))
            meta |= 8;
        return meta;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
    }

    /**
     * Determine the blockstate to place. In 1.12.2 this is called with full placement context.
     */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, net.minecraft.entity.EntityLivingBase placer, EnumHand hand) {
        ItemStack stack = placer.getHeldItem(hand);
        boolean hasBase = stack.getItem() == Registration.REDSTONE_PANEL_ITEM;
        if (stack.hasTagCompound()) {
            NBTTagCompound itemTag = stack.getTagCompound().getCompoundTag("BlockEntityTag");
            if (itemTag.hasKey("hasBase") && !itemTag.getBoolean("hasBase"))
                hasBase = false;
        }

        // Panel attaches to the clicked face — all 6 directions supported now.
        EnumFacing placedFacing = facing.getOpposite();

        if (hasBase) {
            return getDefaultState().withProperty(FACING, placedFacing).withProperty(Registration.HAS_PANEL_BASE, true);
        }
        return getDefaultState().withProperty(FACING, placedFacing).withProperty(Registration.HAS_PANEL_BASE, false);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        TileEntity te = source.getTileEntity(pos);
        if (te instanceof PanelTile) {
            PanelTile panelTile = (PanelTile) te;
            if (panelTile.panelCellHovering != null) {
                AxisAlignedBB cellShape = panelTile.getCellVoxelShape(panelTile.panelCellHovering);
                if (cellShape != null) {
                    if (panelTile.hasBase()) {
                        // PORT-TODO: 1.16 returned VoxelShapes.or(BASE, cellShape) (a true union).
                        // AxisAlignedBB has no union; using the encompassing bounding box of both.
                        return BASE.get(state.getValue(FACING)).union(cellShape);
                    } else {
                        return cellShape;
                    }
                }
            }
        }
        return getCollisionBoundingBoxAABB(state, source, pos);
    }

    /**
     * Collision shape. In 1.12.2 collision is queried via getCollisionBoundingBox.
     * Extracted to a helper so getBoundingBox can reuse it (mirrors 1.16 getShape→getCollisionShape).
     */
    private AxisAlignedBB getCollisionBoundingBoxAABB(IBlockState state, IBlockAccess source, BlockPos pos) {
        TileEntity te = source.getTileEntity(pos);
        if (te instanceof PanelTile) {
            // PORT-TODO: 1.16 returned PanelTile.getVoxelShape() (a multi-box VoxelShape).
            // 1.12.2 collision per-block is a single AxisAlignedBB; using its render bounds.
            AxisAlignedBB shape = ((PanelTile) te).getVoxelShape();
            if (shape != null)
                return shape;
        }
        if (state.getPropertyKeys().contains(Registration.HAS_PANEL_BASE) && state.getValue(Registration.HAS_PANEL_BASE))
            return BASE.get(state.getValue(FACING));
        // if there's no PanelTile, Minecraft is caching the shape for the block state
        // (used to calculate light blocking and collision queries without context).
        return new AxisAlignedBB(2/16d, 2/16d, 2/16d, 14/16d, 14/16d, 14/16d);
    }

    @SuppressWarnings("deprecation")
    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return getCollisionBoundingBoxAABB(state, source, pos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        // 1.16 BlockRenderType.INVISIBLE -> 1.12.2 EnumBlockRenderType.INVISIBLE (TESR-only rendering)
        return EnumBlockRenderType.INVISIBLE;
    }

    /**
     * This block can provide redstone power
     * @return true if this block can provide redstone power
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean canProvidePower(IBlockState iBlockState) {
        return true;
    }

    /**
     * Determine if this block will provide power to redstone and can make a redstone connection on the side provided.
     *
     * @param blockReader                 The current world
     * @param pos                         Block position in world of the wire that is trying to connect
     * @param directionFromNeighborToThis the side of the wire that is trying to make a connection to this block.
     * @return true if this is a power output for redstone, so that redstone wire should connect to it
     */
    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess blockReader, BlockPos pos, @Nullable EnumFacing directionFromNeighborToThis) {
        TileEntity tileentity = blockReader.getTileEntity(pos);
        if (tileentity instanceof PanelTile && directionFromNeighborToThis != null) {
            PanelTile panelTile = (PanelTile) tileentity;
            EnumFacing facing = directionFromNeighborToThis.getOpposite();

            return panelTile.hasCellsOnFace(facing);

        }
        return super.canConnectRedstone(state, blockReader, pos, directionFromNeighborToThis);
    }

    /**
     * How much weak power does this block provide to the adjacent block?
     *
     * @param blockReader   Minecraft block reader
     * @param pos   the position of this block
     * @param state the blockstate of this block
     * @param directionFromNeighborToThis   eg EAST means that this is to the EAST of the block which is asking for weak power
     * @return The power provided [0 - 15]
     */
    @Override
    @SuppressWarnings("deprecation")
    public int getWeakPower(IBlockState state, IBlockAccess blockReader, BlockPos pos, EnumFacing directionFromNeighborToThis) {
        TileEntity tileentity = blockReader.getTileEntity(pos);
        if (tileentity instanceof PanelTile) {
            PanelTile panelTile = (PanelTile) tileentity;

            Integer power = panelTile.weakPowerToNeighbors.get(panelTile.getSideFromDirection(directionFromNeighborToThis.getOpposite()));

            return (power == null) ? 0 : power;

        }
        return 0;
    }

    /**
     * How much strong (direct) power does this block provide to the adjacent block?
     *
     * @param blockReader Minecraft block reader
     * @param pos                         the position of this block
     * @param state                       the blockstate of this block
     * @param directionFromNeighborToThis eg EAST means that this is to the EAST of the block which is asking for strong power
     * @return The power provided [0 - 15]
     */
    @Override
    @SuppressWarnings("deprecation")
    public int getStrongPower(IBlockState state, IBlockAccess blockReader, BlockPos pos, EnumFacing directionFromNeighborToThis) {
        TileEntity tileentity = blockReader.getTileEntity(pos);
        if (tileentity instanceof PanelTile) {
            PanelTile panelTile = (PanelTile) tileentity;

            //first get strong (direct) power
            Integer power = panelTile.strongPowerToNeighbors.get(panelTile.getSideFromDirection(directionFromNeighborToThis.getOpposite()));

            //if it is not a full signal and vanilla redstone is looking for signals from wires (is it not checking a block for direct power)
            //and panel tiles are checking signals from wires (they are not checking a block for direct power)
            //provide the power provided by tiny redstone dust.
            if ((power==null || power < 15) && Blocks.REDSTONE_WIRE.canProvidePower(state) && PanelTile.getCheckWireSignals()) {
                Integer power2 = panelTile.wirePowerToNeighbors.get(panelTile.getSideFromDirection(directionFromNeighborToThis.getOpposite()));
                power = (power==null||(power2!=null && power2>power))?power2:power;
            }

            return (power == null) ? 0 : power;
        }
        return 0;
    }

    // Called when a neighbouring block changes.
    // Only called on the server side.
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState currentState, World world, BlockPos pos, Block blockIn, BlockPos neighborPos) {

        EnumFacing direction;
        if (pos.east().equals(neighborPos))
            direction = EAST;
        else if (pos.south().equals(neighborPos))
            direction = SOUTH;
        else if (pos.west().equals(neighborPos))
            direction = WEST;
        else if (pos.north().equals(neighborPos))
            direction = NORTH;
        else if (pos.up().equals(neighborPos))
            direction = UP;
        else if (pos.down().equals(neighborPos))
            direction = DOWN;
        else
            return;

        TileEntity tileentity = world.getTileEntity(pos);
        if (tileentity instanceof PanelTile) {
                boolean change = false;
                PanelTile panelTile = (PanelTile) tileentity;
            try {

                Side side = panelTile.getSideFromDirection(direction);
                if (side != null) {

                    if (panelTile.pingOutwardObservers(direction))
                        change = true;

                    if (panelTile.updateSide(direction))
                        change = true;

                    if (panelTile.updateOutputs()) {
                        if (!world.isRemote)
                            panelTile.markDirty();
                    }
                    if (change) {
                        panelTile.flagSync();
                    }
                }
            } catch (Exception e) {
                panelTile.handleCrash(e);
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tileentity = world.getTileEntity(pos);
        if (tileentity instanceof PanelTile) {
            PanelTile panelTile = (PanelTile) tileentity;
            panelTile.wirePowerToNeighbors.clear();
            panelTile.weakPowerToNeighbors.clear();
            panelTile.strongPowerToNeighbors.clear();

            world.notifyNeighborsOfStateChange(pos, this, false);
            for (EnumFacing direction : EnumFacing.values()) {
                BlockPos neighborPos = pos.offset(direction);
                IBlockState neighborBlockState = world.getBlockState(neighborPos);
                if (neighborBlockState != null && neighborBlockState.isOpaqueCube())
                    world.notifyNeighborsOfStateChange(neighborPos, neighborBlockState.getBlock(), false);
            }
        }

        super.breakBlock(world, pos, state);
    }

    // PORT-TODO: 1.16 getHarvestTool returned ToolType.get("wrench"). 1.12.2 ToolType does not
    // exist; harvest tool/level are configured via setHarvestLevel (e.g. in Registration/ctor)
    // or left default. Original required a "wrench" tool type which has no 1.12.2 equivalent.

    private ItemStack getItemWithNBT(IBlockAccess worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileentity = worldIn.getTileEntity(pos);
        if (tileentity instanceof PanelTile) {
            PanelTile panelTile = (PanelTile) tileentity;
            ItemStack itemstack = new ItemStack(this);
            NBTTagCompound compoundNBT = panelTile.saveToNbt(new NBTTagCompound());
            compoundNBT.setBoolean("hasBase",panelTile.hasBase());
            if (!compoundNBT.hasNoTags()) {
                itemstack.setTagInfo("BlockEntityTag", compoundNBT);
            }
            return itemstack;
        }
        return null;
    }

    /**
     * Called before the Block is set to air in the world. Called regardless of if the player's tool can actually collect
     * this block. (1.16 playerWillDestroy -> 1.12.2 onBlockHarvested)
     */
    @Override
    public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
        TileEntity te = worldIn.getTileEntity(pos);
        PanelTile panelTile = null;
        if (te instanceof PanelTile){
            panelTile = (PanelTile) te;
            panelTile.onBlockDestroy();
        }
        if(!player.isCreative() && (panelTile==null || panelTile.hasBase() || panelTile.getCellCount()>0)) {
            // PORT-TODO: 1.16 used DyeColor.GRAY.getColorValue(); 1.12.2 EnumDyeColor has no
            // getColorValue(). Using getColorComponentValues-derived packed int via
            // EnumDyeColor.GRAY.getColorValue() equivalent is unavailable; comparing against the
            // gray packed color constant. panelTile.Color is the packed int field on PanelTile.
            ItemStack itemstack =
                    (panelTile.getCellCount()>0||panelTile.Color!=EnumDyeColor.GRAY.getColorValue()||panelTile.panelCover!=null)
                            ? getItemWithNBT(worldIn, pos, state)
                            : new ItemStack(this);
            if(itemstack != null) {
                EntityItem itementity = new EntityItem(worldIn, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, itemstack);
                itementity.setDefaultPickupDelay();
                worldIn.spawnEntity(itementity);
            }
        }
        super.onBlockHarvested(worldIn, pos, state, player);
    }

    // PORT-TODO: 1.16 getPickBlock(BlockState, RayTraceResult, IBlockReader, BlockPos, EntityPlayer).
    // 1.12.2 signature is getPickBlock(IBlockState, RayTraceResult, World, BlockPos, EntityPlayer).
    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof PanelTile) {
            PanelTile panelTile = (PanelTile) te;
            if (!panelTile.isCovered()) {
                PanelCellPos panelCellPos = PanelCellPos.fromHitVec(panelTile, state.getValue(FACING), panelTile.getPlayerCollisionHitResult(player));
                IPanelCell cell = panelTile.getIPanelCell(panelCellPos);
                if (cell != null) {
                    ItemStack itemStack = new ItemStack(panelCellItemMap.get(cell.getClass()));
                    NBTTagCompound itemTag = cell.getItemTag();
                    if (itemTag != null) {
                        for (String key : itemTag.getKeySet()) {
                            itemStack.setTagInfo(key, itemTag.getTag(key));
                        }
                    }
                    return itemStack;
                }
            }
        }
        ItemStack itemStack = getItemWithNBT(world, pos, state);
        if(itemStack == null) return super.getPickBlock(state, target, world, pos, player);
        return itemStack;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing hitFace, float hitX, float hitY, float hitZ) {

        boolean handled = false;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof PanelTile && hand==EnumHand.MAIN_HAND) {
            PanelTile panelTile = (PanelTile) te;
            try {

                // PORT-TODO: 1.16 PosInPanelCell.fromHitVec took a BlockRayTraceResult; the sibling
                // contract exposes getPlayerCollisionHitResult()-style RayTraceResult. Build the
                // hit result from the activation hit vector / face for the panel cell lookup.
                RayTraceResult result = panelTile.getPlayerCollisionHitResult(player);
                PosInPanelCell posInPanelCell = PosInPanelCell.fromHitVec(panelTile, pos, result);
                Item heldItem = player.getHeldItem(hand).getItem();

                if (panelTile.isCrashed() || panelTile.isOverflown()) {
                    //open crash GUI if on client and panel is in crashed state
                    if (world.isRemote)
                        PanelCrashGUI.open(panelTile);
                    handled = true;
                } else if ((posInPanelCell==null || posInPanelCell.getIPanelCell()==null) && heldItem == Registration.REDSTONE_WRENCH && !player.isSneaking() && !panelTile.isCovered()) {
                    //rotate panel if holding wrench
                    panelTile.rotate(Rotation.CLOCKWISE_90);
                    handled = true;
                    // PORT: 1.16 state.updateNeighbourShapes(world,pos,3) -> refresh block/neighbor state
                    world.notifyBlockUpdate(pos, state, state, 3);
                } else if (posInPanelCell != null) {
                    if (heldItem == Registration.REDSTONE_WRENCH && player.isSneaking()) {
                        //harvest block on sneak right click with wrench
                        this.onBlockHarvested(world, pos, state, player);
                        if(!world.isRemote) world.destroyBlock(pos, true);
                        handled = true;
                    } else if (heldItem == Registration.TINY_COLOR_SELECTOR && posInPanelCell.getIPanelCell() instanceof IColorablePanelCell) {
                        if(world.isRemote)
                            TinyBlockGUI.open(panelTile, posInPanelCell.getIndex(), (IColorablePanelCell)posInPanelCell.getIPanelCell());
                        handled = true;
                    } else if (heldItem instanceof ItemDye && posInPanelCell.getIPanelCell()==null) {
                        //dye the panel if right clicking with a dye
                        // PORT-TODO: 1.16 ((DyeItem)heldItem).getDyeColor() -> 1.12.2 derives EnumDyeColor
                        // from the dye item's metadata via EnumDyeColor.byDyeDamage(stack metadata).
                        EnumDyeColor dyeColor = EnumDyeColor.byDyeDamage(player.getHeldItem(hand).getMetadata());
                        int color = dyeColor.getColorValue();
                        if (color != panelTile.Color) {
                            panelTile.Color = color;
                            //remove an item from the player's stack
                            if (!player.isCreative())
                                player.getHeldItem(hand).setCount(player.getHeldItem(hand).getCount() - 1);
                            world.notifyBlockUpdate(pos,state,state,0);
                        }
                        handled = true;
                    } else if (heldItem == Item.getItemFromBlock(Blocks.BARRIER) && player.getName().equals("Dev")) {
                        //Allows testing of crash management in Dev environment.
                        throw new Exception("Test Exception");
                    } else if (itemPanelCoverMap.containsKey(heldItem) && !panelTile.isCovered()) {
                        //right clicked with a panel cover
                        try {
                            Object panelCoverObject = itemPanelCoverMap.get(heldItem).getConstructors()[0].newInstance();
                            if (panelCoverObject instanceof IPanelCover) {
                                panelTile.panelCover = (IPanelCover) panelCoverObject;
                                ((IPanelCover) panelCoverObject).onPlace(panelTile,player);
                                panelTile.flagLightUpdate = true;
                                panelTile.flagVoxelShapeUpdate();

                                //do one last sync after covering panel
                                if (!world.isRemote)
                                    world.notifyBlockUpdate(pos, state, state, 3);

                                //remove an item from the player's stack
                                if (!player.isCreative())
                                    player.getHeldItem(hand).setCount(player.getHeldItem(hand).getCount() - 1);

                                handled = true;

                            }
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            TinyRedstone.LOGGER.error("Exception thrown while" + e.getMessage());
                        }
                    } else if (posInPanelCell.getIPanelCell() != null && !panelTile.isCovered() && posInPanelCell.getIPanelCell().hasActivation(player) && !player.isSneaking()) {

                        //if player clicked on a panel cell, activate it
                        if (posInPanelCell.getIPanelCell().onBlockActivated(posInPanelCell, posInPanelCell.getSegment(), player)) {
                            panelTile.updateCell(posInPanelCell);
                            panelTile.updateNeighborCells(posInPanelCell);
                            if (posInPanelCell.getIPanelCell() instanceof RedstoneDust) {
                                PanelCellPos above = posInPanelCell.offset(Side.TOP), below = posInPanelCell.offset(Side.BOTTOM);
                                if (above !=null)
                                    panelTile.updateNeighborCells(above);
                                if (below!=null)
                                    panelTile.updateNeighborCells(below);
                            }
                        }
                        handled = true;
                    } else if (itemPanelCellMap.containsKey(heldItem) && !panelTile.isCovered()) {
                        //if player is holding an item registered as a panel cell, try to place that cell on the panel
                        PanelCellPos placementPos = posInPanelCell;
                        if(placementPos.getIPanelCell()!=null)
                        {
                            placementPos = posInPanelCell.offset(panelTile.getSideFromDirection(result.sideHit));
                        }

                        //but first, check to see if cell exists and is empty
                        if (placementPos!=null && placementPos.getIPanelCell()==null && !panelTile.checkCellForPistonExtension(placementPos)) {
                            try {
                                //catch any exception thrown while attempting to construct from the registered IPanelCell class
                                Object panelCell = itemPanelCellMap.get(heldItem).getConstructors()[0].newInstance();

                                if (panelCell instanceof IPanelCell) {
                                    IPanelCell cell = (IPanelCell) panelCell;
                                    boolean placementOK = true;
                                    Side rotationLock = RotationLock.getServerRotationLock(player);
                                    Side cellFacing = rotationLock == null
                                            ? panelTile.getSideFromDirection(panelTile.getPlayerDirectionFacing(player, cell.canPlaceVertical()))
                                            : rotationLock;

                                    if (cell.needsSolidBase()) {
                                        Side attachingSideDir = panelTile.getSideFromDirection(result.sideHit).getOpposite();
                                        Side attachingSideRel = (attachingSideDir==Side.TOP || attachingSideDir==Side.BOTTOM)?attachingSideDir:Side.FRONT;
                                        if (
                                            //check if the cell can attach to the side of the block facing
                                                !cell.canAttachToBaseOnSide(attachingSideRel) || (
                                                        //if so, check if it's being placed against a full block
                                                        !posInPanelCell.equals(placementPos) && (
                                                                posInPanelCell.getIPanelCell() == null
                                                                        || !posInPanelCell.getIPanelCell().isPushable()
                                                        )
                                                )
                                        ) {
                                            placementOK = false;
                                        }
                                        else {
                                            //set the direction of the base block
                                            cell.setBaseSide(attachingSideRel);
                                            //set the cell direction to face the base block
                                            if (attachingSideRel==Side.FRONT)
                                                cellFacing=attachingSideDir;
                                        }
                                    }
                                    if (placementOK) {
                                        //place the cell on the panel

                                        placementPos.getPanelTile().addCell(
                                                placementPos,
                                                cell,
                                                cellFacing,
                                                player
                                        );

                                        //remove an item from the player's stack
                                        if (!player.isCreative())
                                            player.getHeldItem(hand).setCount(player.getHeldItem(hand).getCount() - 1);

                                        // PORT: 1.16 state.updateNeighbourShapes(world,pos,3)
                                        world.notifyBlockUpdate(pos, state, state, 3);
                                    }
                                    handled = true;
                                }

                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                TinyRedstone.LOGGER.error(e.getMessage());
                            }
                        }
                    } else if (panelTile.isOverflown())
                    {
                        //open crash GUI if on client and panel is in crashed state
                        if (world.isRemote)
                            PanelCrashGUI.open(panelTile);
                        handled = true;
                    }

                    panelTile.flagSync();
                    if (!world.isRemote) {
                        panelTile.markDirty();
                    }
                    panelTile.updateOutputs();
                }

            }catch (Exception e)
            {
                panelTile.handleCrash(e);
            }
        }
        return handled;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        int ll = 0;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof PanelTile) {
            PanelTile panelTile = (PanelTile) te;
            ll=panelTile.getLightOutput();
        }
        return Math.min(ll,15);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
        Item heldItem = player.getHeldItemMainhand().getItem();

        if ((heldItem==Registration.REDSTONE_WRENCH || PanelBlock.itemPanelCellMap.containsKey(heldItem) || PanelBlock.itemPanelCoverMap.containsKey(heldItem)))
        {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof PanelTile) {
                PanelTile panelTile = (PanelTile) te;

                try {
                    if (panelTile.isCovered())
                    {
                        removeCover(panelTile,player);
                        panelTile.flagLightUpdate=true;
                    }
                    else {
                        if(heldItem==Registration.REDSTONE_WRENCH && player.isSneaking()) {
                            if(world.isRemote)
                                ClearPanelGUI.open(panelTile);
                        }
                        else {
                            RayTraceResult result = panelTile.getPlayerCollisionHitResult(player);
                            PanelCellPos panelCellPos = PanelCellPos.fromHitVec(panelTile, world.getBlockState(pos).getValue(FACING), result);

                            if (panelCellPos != null) {
                                if (panelCellPos.getIPanelCell() != null) {
                                    //if player left clicks with wrench, remove cell
                                    removeCell(panelCellPos, player);

                                    // PORT: 1.16 state.updateNeighbourShapes(world,pos,3)
                                    world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
                                }
                            }
                        }
                    }
                }catch (Exception e)
                {
                    panelTile.handleCrash(e);
                }
            }
        }
    }

   protected void removeCell(PanelCellPos cellPos, @Nullable EntityPlayer player)
    {
        if (cellPos.getIPanelCell()!=null) {

            PanelTile panelTile = cellPos.getPanelTile();
            World world = panelTile.getWorld();
            BlockPos pos = panelTile.getPos();

            // drop panel cell item
            if (player==null || !player.isCreative()) {
                Item item = panelCellItemMap.get(cellPos.getIPanelCell().getClass());
                ItemStack itemStack = new ItemStack(item);
                NBTTagCompound itemTag = cellPos.getIPanelCell().getItemTag();
                if (itemTag!=null){
                    for (String key : itemTag.getKeySet()){
                        itemStack.setTagInfo(key,itemTag.getTag(key));
                    }
                }
                EntityItem itemEntity = new EntityItem(world, pos.getX(), pos.getY()+.5, pos.getZ(), itemStack);
                world.spawnEntity(itemEntity);
                if (player!=null)
                    itemEntity.setPosition(player.posX,player.posY,player.posZ);
            }

            //remove from panel
            panelTile.removeCell(cellPos);

            // PORT: 1.16 panelTile.getBlockState().updateNeighbourShapes(world,pos,3)
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }

    }

    private void removeCover(PanelTile panelTile,EntityPlayer player)
    {
        if (panelTile.isCovered())
        {
            World world = panelTile.getWorld();
            BlockPos pos = panelTile.getPos();

            // drop panel cell item
            if (!player.isCreative()) {
                Item item = panelCoverItemMap.get(panelTile.panelCover.getClass());
                ItemStack itemStack = new ItemStack(item);
                NBTTagCompound coverTag = panelTile.panelCover.getItemTag();
                if (coverTag!=null) {
                    for (String key : coverTag.getKeySet()) {
                        itemStack.setTagInfo(key, coverTag.getTag(key));
                    }
                }
                EntityItem itemEntity = new EntityItem(world, pos.getX(), pos.getY()+.5, pos.getZ(), itemStack);
                world.spawnEntity(itemEntity);
                itemEntity.setPosition(player.posX,player.posY,player.posZ);
            }

            //remove from panel
            panelTile.panelCover = null;
            panelTile.flagVoxelShapeUpdate();
            panelTile.flagSync();

        }
    }

    public static void registerPanelCell(Class<? extends IPanelCell> iPanelCellClass, Item correspondingItem)
    {
        itemPanelCellMap.put(correspondingItem,iPanelCellClass);
        panelCellItemMap.put(iPanelCellClass,correspondingItem);
    }

    public static void registerPanelCover(Class<? extends IPanelCover> iPanelCellCover, Item correspondingItem)
    {
        itemPanelCoverMap.put(correspondingItem,iPanelCellCover);
        panelCoverItemMap.put(iPanelCellCover,correspondingItem);
    }

    public static Item getPanelCellItemFromClass(Class<? extends IPanelCell> iPanelCellClass)
    {
        return panelCellItemMap.get(iPanelCellClass);
    }

    public static Class<? extends IPanelCell> getPanelCellClassFromItem(Item item)
    {
        return itemPanelCellMap.get(item);
    }

    public static boolean isPanelCellItem(Item item)
    {
        return itemPanelCellMap.containsKey(item);
    }

}
