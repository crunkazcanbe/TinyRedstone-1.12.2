package com.dannyandson.tinyredstone.blocks;

import com.dannyandson.tinyredstone.network.ModNetworkHandler;
import com.dannyandson.tinyredstone.network.RotationLockRemoveSync;
import com.dannyandson.tinyredstone.network.RotationLockSync;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.UUID;

// NOTE: @SideOnly's argument is fully-qualified (net.minecraftforge.fml.relauncher.Side) because
// this package also defines com.dannyandson.tinyredstone.blocks.Side, so an unqualified "Side"
// would resolve to the mod enum, not the Forge side marker.
public class RotationLock {
    private static final HashMap<UUID, Side> playerRotationLock = new HashMap<>();

    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    private static Side rotationLock;

    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static void removeLock() {
        removeLock(true);
    }

    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static void removeLock(boolean sendToServer) {
        if (rotationLock != null) {
            rotationLock = null;
            if(sendToServer) ModNetworkHandler.sendToServer(new RotationLockRemoveSync());
        }
    }

    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static void lockRotation(Side side, boolean allowVertical, boolean invert) {
        lockRotation(side, allowVertical, invert, true);
    }

    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static void lockRotation(Side side, boolean allowVertical, boolean invert, boolean sendToServer) {
        switch (side) {
            case BACK:
                side = invert ? Side.RIGHT : (allowVertical ? Side.BOTTOM : Side.LEFT);
                break;
            case LEFT:
                side = invert ? (allowVertical ? Side.TOP : Side.BACK) : Side.FRONT;
                break;
            case FRONT:
                side = invert ? Side.LEFT : Side.RIGHT;
                break;
            case RIGHT:
                side = invert ? Side.FRONT : Side.BACK;
                break;
            case BOTTOM:
                side = invert ? Side.BACK : Side.TOP;
                break;
            case TOP:
                side = invert ? Side.BOTTOM : Side.LEFT;
                break;
        }
        if(side != rotationLock) {
            rotationLock = side;
            if(sendToServer) ModNetworkHandler.sendToServer(new RotationLockSync(side));
        }
    }

    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static void lockRotation(boolean allowVertical, boolean invert) {
        lockRotation(allowVertical, invert, true);
    }

    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static void lockRotation(PanelTile panelTile, EntityPlayer playerEntity, boolean allowVertical, boolean invert) {
        lockRotation(panelTile, playerEntity, allowVertical, invert, true);
    }

    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static void lockRotation(boolean allowVertical, boolean invert, boolean sendToServer) {
        lockRotation(rotationLock == null ? Side.FRONT : rotationLock, allowVertical, invert, sendToServer);
    }

    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static void lockRotation(PanelTile panelTile, EntityPlayer playerEntity, boolean allowVertical, boolean invert, boolean sendToServer) {
        lockRotation(rotationLock == null ? panelTile.getSideFromDirection(panelTile.getPlayerDirectionFacing(playerEntity, allowVertical)) : rotationLock, allowVertical, invert, sendToServer);
    }

    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static Side getRotationLock() {
        return rotationLock;
    }

    public static void removeServerLock(EntityPlayer player) {
        playerRotationLock.remove(player.getUniqueID());
    }

    public static void lockServerRotation(EntityPlayer playerEntity, Side side) {
        playerRotationLock.put(playerEntity.getUniqueID(), side);
    }

    public static Side getServerRotationLock(EntityPlayer playerEntity) {
        return playerRotationLock.get(playerEntity.getUniqueID());
    }
}
