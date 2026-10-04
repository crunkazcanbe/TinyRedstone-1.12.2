package com.dannyandson.tinyredstone.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class ModNetworkHandler {
    private static SimpleNetworkWrapper INSTANCE;

    public static void registerMessages() {
        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("tinyredstone");

        // Server-bound packets (handled on the server) register with Side.SERVER.
        // Client-bound packets (handled on the client) register with Side.CLIENT.
        // Discriminator ids match the 1.16 nextID() assignment order.

        INSTANCE.registerMessage(RepeaterTickSync.Handler.class, RepeaterTickSync.class, 0, Side.SERVER);
        INSTANCE.registerMessage(TinyBlockColorSync.Handler.class, TinyBlockColorSync.class, 1, Side.SERVER);
        INSTANCE.registerMessage(NoteBlockInstrumentSync.Handler.class, NoteBlockInstrumentSync.class, 2, Side.SERVER);
        INSTANCE.registerMessage(PanelCellSync.Handler.class, PanelCellSync.class, 3, Side.CLIENT);
        INSTANCE.registerMessage(BlueprintSync.Handler.class, BlueprintSync.class, 4, Side.SERVER);
        INSTANCE.registerMessage(RotationLockSync.Handler.class, RotationLockSync.class, 5, Side.SERVER);
        INSTANCE.registerMessage(RotationLockRemoveSync.Handler.class, RotationLockRemoveSync.class, 6, Side.SERVER);
        INSTANCE.registerMessage(CrashFlagResetSync.Handler.class, CrashFlagResetSync.class, 7, Side.SERVER);
        INSTANCE.registerMessage(ClearPanelSync.Handler.class, ClearPanelSync.class, 8, Side.SERVER);
        INSTANCE.registerMessage(PlaySound.Handler.class, PlaySound.class, 9, Side.CLIENT);

        // ValidTinyBlockCacheSync travels both ways: server->client (ask client to verify a texture),
        // then client->server (report a valid texture). Register both handlers under one discriminator,
        // one per side.
        INSTANCE.registerMessage(ValidTinyBlockCacheSync.ClientHandler.class, ValidTinyBlockCacheSync.class, 10, Side.CLIENT);
        INSTANCE.registerMessage(ValidTinyBlockCacheSync.ServerHandler.class, ValidTinyBlockCacheSync.class, 10, Side.SERVER);

        INSTANCE.registerMessage(LogicGateConfigSync.Handler.class, LogicGateConfigSync.class, 11, Side.SERVER);
    }

    public static void sendToClient(IMessage packet, EntityPlayerMP player) {
        INSTANCE.sendTo(packet, player);
    }

    public static void sendToServer(IMessage packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendToNearestClient(IMessage packet, World level, BlockPos pos) {
        EntityPlayer player = level.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), -1d, false);
        if (player instanceof EntityPlayerMP)
            INSTANCE.sendTo(packet, (EntityPlayerMP) player);
    }

}
