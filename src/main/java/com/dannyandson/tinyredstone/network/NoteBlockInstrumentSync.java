package com.dannyandson.tinyredstone.network;

import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.panelcells.NoteBlock;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class NoteBlockInstrumentSync implements IMessage {
    private BlockPos pos;
    private int cellIndex;
    private String instrument;

    public NoteBlockInstrumentSync() {}

    public NoteBlockInstrumentSync(BlockPos pos, int cellIndex, String instrument)
    {
        this.pos=pos;
        this.cellIndex=cellIndex;
        this.instrument = instrument;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        this.pos= buffer.readBlockPos();
        this.cellIndex=buffer.readInt();
        this.instrument =buffer.readString(32);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeBlockPos(pos);
        buffer.writeInt(cellIndex);
        buffer.writeString(instrument);
    }

    public static class Handler implements IMessageHandler<NoteBlockInstrumentSync, IMessage> {
        @Override
        public IMessage onMessage(NoteBlockInstrumentSync message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.getServerWorld().getTileEntity(message.pos);
                if (te instanceof PanelTile)
                {
                    PanelCellPos cellPos = PanelCellPos.fromIndex((PanelTile) te, message.cellIndex);
                    IPanelCell cell = cellPos.getIPanelCell();
                    if (cell instanceof NoteBlock)
                    {
                        ((NoteBlock)cell).setInstrument(message.instrument);
                        ((PanelTile) te).flagSync();
                    }
                }
            });
            return null;
        }
    }
}
