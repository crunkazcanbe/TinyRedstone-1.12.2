package com.dannyandson.tinyredstone.network;

import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.panelcells.Repeater;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RepeaterTickSync implements IMessage {
    private BlockPos pos;
    private int cellIndex;
    private int ticks;

    public RepeaterTickSync() {}

    public RepeaterTickSync(BlockPos pos, int cellIndex, int ticks)
    {
        this.pos=pos;
        this.cellIndex=cellIndex;
        this.ticks = ticks;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        this.pos= buffer.readBlockPos();
        this.cellIndex=buffer.readInt();
        this.ticks=buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeBlockPos(pos);
        buffer.writeInt(cellIndex);
        buffer.writeInt(ticks);
    }

    public static class Handler implements IMessageHandler<RepeaterTickSync, IMessage> {
        @Override
        public IMessage onMessage(RepeaterTickSync message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.getServerWorld().getTileEntity(message.pos);
                if (te instanceof PanelTile)
                {
                    PanelCellPos cellPos = PanelCellPos.fromIndex((PanelTile) te, message.cellIndex);
                    IPanelCell cell = cellPos.getIPanelCell();
                    if (cell instanceof Repeater)
                    {
                        ((Repeater)cell).setTicks(message.ticks);
                        ((PanelTile) te).flagSync();
                    }
                }
            });
            return null;
        }
    }
}
