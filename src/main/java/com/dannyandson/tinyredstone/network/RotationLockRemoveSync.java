package com.dannyandson.tinyredstone.network;

import com.dannyandson.tinyredstone.blocks.RotationLock;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RotationLockRemoveSync implements IMessage {
    public RotationLockRemoveSync() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<RotationLockRemoveSync, IMessage> {
        @Override
        public IMessage onMessage(RotationLockRemoveSync message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() ->
                    RotationLock.removeServerLock(player));
            return null;
        }
    }
}
