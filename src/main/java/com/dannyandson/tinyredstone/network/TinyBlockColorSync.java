package com.dannyandson.tinyredstone.network;

import com.dannyandson.tinyredstone.api.IColorablePanelCell;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TinyBlockColorSync implements IMessage {
    private BlockPos pos;
    private int cellIndex;
    private int color;

    public TinyBlockColorSync() {}

    public TinyBlockColorSync(BlockPos pos, int cellIndex, int color)
    {
        this.pos=pos;
        this.cellIndex=cellIndex;
        this.color = color;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        this.pos= buffer.readBlockPos();
        this.cellIndex=buffer.readInt();
        this.color =buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeBlockPos(pos);
        buffer.writeInt(cellIndex);
        buffer.writeInt(color);
    }

    public static class Handler implements IMessageHandler<TinyBlockColorSync, IMessage> {
        @Override
        public IMessage onMessage(TinyBlockColorSync message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.getServerWorld().getTileEntity(message.pos);
                if (te instanceof PanelTile)
                {
                    PanelCellPos cellPos = PanelCellPos.fromIndex((PanelTile) te, message.cellIndex);
                    IPanelCell cell = cellPos.getIPanelCell();
                    if (cell instanceof IColorablePanelCell)
                    {
                        ((IColorablePanelCell)cell).setColor(message.color);
                        ((PanelTile) te).flagSync();
                    }
                }
            });
            return null;
        }
    }
}
