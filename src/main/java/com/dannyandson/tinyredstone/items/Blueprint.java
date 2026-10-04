package com.dannyandson.tinyredstone.items;

import com.dannyandson.tinyredstone.Config;
import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.PanelBlock;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.Side;
import com.dannyandson.tinyredstone.blocks.panelcells.TinyBlock;
import com.dannyandson.tinyredstone.blocks.panelcells.TransparentBlock;
import com.dannyandson.tinyredstone.gui.BlueprintGUI;
import com.dannyandson.tinyredstone.setup.ModSetup;
import com.dannyandson.tinyredstone.setup.Registration;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class Blueprint extends Item {

    public Blueprint() {
        super();
        setCreativeTab(ModSetup.ITEM_GROUP);
    }

    @Override
    public  void  addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flags)
    {
        if (stack.getTagCompound() !=null && stack.getTagCompound().hasKey("blueprint"))
        {
            list.add(new net.minecraft.util.text.TextComponentTranslation("message.item.blueprint.full").getFormattedText());
            List<ItemStack> blueprintItems = getRequiredItemStacks(stack.getTagCompound().getCompoundTag("blueprint"));
            for (ItemStack item : blueprintItems)
            {
                String itemName = item.getDisplayName();
                list.add(itemName + " : " + item.getCount());
            }
        }
        else
        {
            list.add(new net.minecraft.util.text.TextComponentTranslation("message.item.blueprint.empty").getFormattedText());
        }
    }


    //called when item is used on a block
    @Override
    @Nonnull
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack heldStack = player.getHeldItem(hand);
        TileEntity te = world.getTileEntity(pos);
        PanelTile panelTile = null;
        if (te instanceof PanelTile) {
            panelTile = (PanelTile) te;
        } else if (Config.ALLOW_WORLD_PLACEMENT){
            ItemStack itemStackCopy = heldStack.copy();
            // PORT-TODO: 1.16 used BlockItemUseContext + BlockItem.place(...) to place the panel block.
            // 1.12.2 equivalent is ItemBlock.onItemUse on the panel item; forwarding click coords.
            ((ItemBlock) Registration.REDSTONE_PANEL_ITEM).onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
            player.setHeldItem(hand, itemStackCopy);
            te = world.getTileEntity(pos.offset(facing));
            if (te instanceof PanelTile) {
                panelTile = (PanelTile) te;
            }
        }
        if (panelTile != null) {
            if (heldStack.getTagCompound() !=null && heldStack.getTagCompound().hasKey("blueprint"))
            {
                if (panelTile.getCellCount()==0 && player!=null)
                {
                    NBTTagCompound blueprintNBT = heldStack.getTagCompound().getCompoundTag("blueprint");
                    List<ItemStack> items = getRequiredItemStacks(blueprintNBT);
                    if (player.isCreative() || playerHasSufficientComponents(items, player)) {

                        panelTile.loadCellsFromNBT(blueprintNBT,false);
                        panelTile.updateSide(Side.FRONT);
                        panelTile.updateSide(Side.RIGHT);
                        panelTile.updateSide(Side.BACK);
                        panelTile.updateSide(Side.LEFT);
                        panelTile.updateSide(Side.TOP);
                        panelTile.markDirty();

                        if (!player.isCreative())
                        {
                            for (ItemStack item : items)
                            {
                                int itemsToRemove = item.getCount();
                                for(ItemStack invStack : player.inventory.mainInventory)
                                {
                                    if (stacksAreMatchingItem(invStack,item))
                                    {
                                        int removeCt = Math.min(invStack.getCount(),itemsToRemove);
                                        invStack.setCount(invStack.getCount()-removeCt);
                                        itemsToRemove-=removeCt;
                                    }
                                    if (itemsToRemove==0)
                                        break;
                                }
                            }
                        }
                    }
                    panelTile.removeOutOfRange(player);
                }
            }
            else {
                NBTTagCompound nbt = new NBTTagCompound();
                NBTTagCompound blueprintNBT = panelTile.saveToNbt(new NBTTagCompound());
                nbt.setInteger("CustomModelData",1);
                nbt.setTag("blueprint",blueprintNBT);

                heldStack.setTagCompound(nbt);

            }
        }

        return EnumActionResult.SUCCESS;
    }

    //called when item is right clicked in the air
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        if(worldIn.isRemote && Config.JSON_BLUEPRINT)
            BlueprintGUI.open(playerIn.getHeldItem(handIn));
        return super.onItemRightClick(worldIn, playerIn, handIn);
    }

    private static List<ItemStack> getRequiredItemStacks(NBTTagCompound blueprintNBT) {
        List<ItemStack> itemStacks = new ArrayList<>();

        if (blueprintNBT.hasKey("cells")) {
            NBTTagCompound cellsNBT = blueprintNBT.getCompoundTag("cells");
            for (String key : cellsNBT.getKeySet()) {
                try {
                    Class iPanelCellClass = Class.forName(cellsNBT.getCompoundTag(key).getString("class"));
                    if (IPanelCell.class.isAssignableFrom(iPanelCellClass)) {
                        Item item = PanelBlock.getPanelCellItemFromClass((Class<? extends IPanelCell>) iPanelCellClass);
                        ItemStack itemStack = item.getDefaultInstance();
                        if (iPanelCellClass == TinyBlock.class || iPanelCellClass == TransparentBlock.class) {
                            NBTTagCompound cellDataNBT = cellsNBT.getCompoundTag(key).getCompoundTag("data");
                            if (cellDataNBT.hasKey("made_from_namespace")) {
                                NBTTagCompound madeFromTag = new NBTTagCompound();
                                madeFromTag.setString("namespace", cellDataNBT.getString("made_from_namespace"));
                                madeFromTag.setString("path", cellDataNBT.getString("made_from_path"));
                                NBTTagCompound itemTag = new NBTTagCompound();
                                itemTag.setTag("made_from", madeFromTag);
                                itemStack.setTagCompound(itemTag);
                            }
                        }

                        boolean addNeeded = true;

                        for (ItemStack stack : itemStacks) {
                            if (stacksAreMatchingItem(stack, itemStack)) {
                                stack.setCount(stack.getCount() + 1);
                                addNeeded = false;
                                break;
                            }
                        }

                        if (addNeeded)
                            itemStacks.add(itemStack);

                    }

                } catch (ClassNotFoundException e) {
                    TinyRedstone.LOGGER.error("Class not found exception while attempting to read components from blueprint NBT: " + e.getLocalizedMessage());
                }

            }
        }
        return itemStacks;
    }

    private static boolean playerHasSufficientComponents(List<ItemStack> itemStacks, EntityPlayer player)
    {
        for (ItemStack itemStack : itemStacks)
        {
            int count = 0;
            for(ItemStack invStack : player.inventory.mainInventory)
            {
                if (stacksAreMatchingItem(invStack,itemStack))
                {
                    count+= invStack.getCount();
                }
            }
            if (count<itemStack.getCount())
                return false;
        }
        return true;
    }

    private static boolean stacksAreMatchingItem(ItemStack stack1, ItemStack stack2){
        return stack1.getItem() == stack2.getItem() &&
                (
                        (!stack1.hasTagCompound() && !stack2.hasTagCompound()) ||
                                (stack1.hasTagCompound() && stack1.getTagCompound().equals(stack2.getTagCompound()))
                );
    }

    /**
     * Checks nbt for valid blueprint data. Removes any irrelevant data or cells with no definitions installed.
     * RETURNS NULL if no valid data found.
     * @param nbt NBTTagCompound to be cleaned up - usually acquired untrusted source such as a json file or network
     * @return cleaned up NBT with any irrelevant date removed. NULL if no valid data found.
     */
    @Nullable
    public static NBTTagCompound cleanUpBlueprintNBT(NBTTagCompound nbt)
    {
        if (nbt.hasKey("blueprint"))
        {
            NBTTagCompound blueprintNBT = nbt.getCompoundTag("blueprint");
            if (blueprintNBT.hasKey("cells"))
            {
                NBTTagCompound newCellsNBT = new NBTTagCompound();

                NBTTagCompound cellsNBT = blueprintNBT.getCompoundTag("cells");
                for (String key : cellsNBT.getKeySet())
                {
                    try {
                        if (IPanelCell.class.isAssignableFrom(Class.forName(cellsNBT.getCompoundTag(key).getString("class"))))
                        {
                            newCellsNBT.setTag(key,cellsNBT.getCompoundTag(key));
                        }
                    }catch (ClassNotFoundException e)
                    {
                        TinyRedstone.LOGGER.error("Class not found exception while attempting to read components from blueprint NBT: " + e.getLocalizedMessage());
                    }

                }

                NBTTagCompound newNBT = new NBTTagCompound();
                NBTTagCompound newBlueprintNBT = new NBTTagCompound();
                newNBT.setInteger("CustomModelData",1);
                newBlueprintNBT.setTag("cells",newCellsNBT);
                newNBT.setTag("blueprint",newBlueprintNBT);
                if (nbt.hasKey("display"))
                    newNBT.setTag("display",nbt.getCompoundTag("display"));

                return newNBT;
            }
        }
        return null;
    }

}
