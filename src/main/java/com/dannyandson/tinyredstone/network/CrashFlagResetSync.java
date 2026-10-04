package com.dannyandson.tinyredstone.network;

import com.dannyandson.tinyredstone.blocks.PanelTile;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class CrashFlagResetSync implements IMessage {
    private BlockPos pos;

    public CrashFlagResetSync() {}

    public CrashFlagResetSync(BlockPos pos)
    {
        this.pos=pos;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.pos = new PacketBuffer(buf).readBlockPos();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        new PacketBuffer(buf).writeBlockPos(pos);
    }

    public static class Handler implements IMessageHandler<CrashFlagResetSync, IMessage> {
        @Override
        public IMessage onMessage(CrashFlagResetSync message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.getServerWorld().getTileEntity(message.pos);
                if (te instanceof PanelTile)
                {
                    ((PanelTile)te).resetCrashFlag();
                    ((PanelTile)te).resetOverflownFlag();
                }
            });
            return null;
        }
    }
}
