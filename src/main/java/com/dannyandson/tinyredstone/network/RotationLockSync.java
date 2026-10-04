package com.dannyandson.tinyredstone.network;

import com.dannyandson.tinyredstone.blocks.RotationLock;
import com.dannyandson.tinyredstone.blocks.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RotationLockSync implements IMessage {
    private Side rotationLock;

    public RotationLockSync() {}

    public RotationLockSync(Side rotationLock) {
        this.rotationLock = rotationLock;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.rotationLock = new PacketBuffer(buf).readEnumValue(Side.class);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        new PacketBuffer(buf).writeEnumValue(rotationLock);
    }

    public static class Handler implements IMessageHandler<RotationLockSync, IMessage> {
        @Override
        public IMessage onMessage(RotationLockSync message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() ->
                    RotationLock.lockServerRotation(player, message.rotationLock));
            return null;
        }
    }
}
