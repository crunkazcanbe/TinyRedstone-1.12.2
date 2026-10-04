package com.dannyandson.tinyredstone;

import com.dannyandson.tinyredstone.api.AbstractPanelCellItem;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.PanelBlock;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.RotationLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.InvocationTargetException;

@Mod.EventBusSubscriber(modid = TinyRedstone.MODID, value = Side.CLIENT)
public class ClientBinding {

    public static KeyBinding rotationLock;

    public static void registerKeyBindings() {
        // 1.12.2 KeyBinding uses LWJGL2 keycodes (org.lwjgl.input.Keyboard); LEFT_ALT == LMENU.
        rotationLock = new KeyBinding("key." + TinyRedstone.MODID + ".rotation_lock", KeyConflictContext.IN_GAME, Keyboard.KEY_LMENU, "TinyRedstone");
        ClientRegistry.registerKeyBinding(rotationLock);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent keyInputEvent) {
        // 1.12.2: detect which number key (1-9) was just pressed.
        int eventKey = Keyboard.getEventKey();
        if (!Keyboard.getEventKeyState()) return; // only on key-down
        int numberKey = 0;
        if (eventKey >= Keyboard.KEY_1 && eventKey <= Keyboard.KEY_9) {
            numberKey = eventKey - Keyboard.KEY_1 + 1;
        }
        if (numberKey > 0 && numberKey <= 9) {
            final EntityPlayer player = Minecraft.getMinecraft().player;
            if (player == null) return;
            if (player.inventory.currentItem + 1 == numberKey) return;
            final ItemStack mainHand = player.getHeldItemMainhand();
            final Item mainHandItem = mainHand.getItem();

            if (mainHandItem instanceof AbstractPanelCellItem) {
                RotationLock.removeLock();
            }
        }
    }

    @SubscribeEvent
    public static void wheelEvent(final MouseEvent mouseEvent) {
        if (mouseEvent.isCanceled()) return;
        // 1.12.2 MouseEvent.getDwheel() is the scroll delta (positive = up).
        final int scrollDelta = mouseEvent.getDwheel();
        if (scrollDelta == 0) return;
        final EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;
        World world = Minecraft.getMinecraft().world;
        final ItemStack mainHand = player.getHeldItemMainhand();
        final Item mainHandItem = mainHand.getItem();

        if (mainHandItem instanceof AbstractPanelCellItem) {
            if (Keyboard.isKeyDown(rotationLock.getKeyCode())) {
                RayTraceResult rayTraceResult = Minecraft.getMinecraft().objectMouseOver;
                if (rayTraceResult != null && rayTraceResult.getBlockPos() != null) {
                    BlockPos blockPos = rayTraceResult.getBlockPos();
                    TileEntity te = world.getTileEntity(blockPos);
                    if (te instanceof PanelTile) {
                        try {
                            IPanelCell panelCell = (IPanelCell) PanelBlock.getPanelCellClassFromItem(mainHandItem).getConstructors()[0].newInstance();
                            RotationLock.lockRotation((PanelTile) te, player, panelCell.canPlaceVertical(), scrollDelta < 0);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            TinyRedstone.LOGGER.error("Exception thrown when attempting to lock rotation: " + e.getMessage());
                        }
                    } else {
                        try {
                            IPanelCell panelCell = (IPanelCell) PanelBlock.getPanelCellClassFromItem(mainHandItem).getConstructors()[0].newInstance();
                            RotationLock.lockRotation(panelCell.canPlaceVertical(), scrollDelta < 0);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            TinyRedstone.LOGGER.error("Exception thrown when attempting to lock rotation: " + e.getMessage());
                        }
                    }
                    mouseEvent.setCanceled(true);
                }
            } else {
                RotationLock.removeLock();
            }
        }
    }
}
