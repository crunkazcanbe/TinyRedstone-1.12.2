package com.dannyandson.tinyredstone.network;

import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PanelCellSync implements IMessage {
    private BlockPos pos;
    private int cellIndex;
    private NBTTagCompound nbt;

    public PanelCellSync() {}

    public PanelCellSync(BlockPos pos, int cellIndex, NBTTagCompound nbt)
    {
        this.pos=pos;
        this.cellIndex=cellIndex;
        this.nbt = nbt;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        this.pos= buffer.readBlockPos();
        this.cellIndex=buffer.readInt();
        try {
            this.nbt = buffer.readCompoundTag();
        } catch (java.io.IOException e) {
            this.nbt = null;
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeBlockPos(pos);
        buffer.writeInt(cellIndex);
        buffer.writeCompoundTag(nbt);
    }

    public static class Handler implements IMessageHandler<PanelCellSync, IMessage> {
        @Override
        public IMessage onMessage(PanelCellSync message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                TileEntity te = Minecraft.getMinecraft().world.getTileEntity(message.pos);
                if (te instanceof PanelTile)
                {
                    PanelCellPos cellPos = PanelCellPos.fromIndex((PanelTile) te, message.cellIndex);
                    IPanelCell cell = cellPos.getIPanelCell();
                    if (cell != null)
                    {
                        cell.readNBT(message.nbt);
                    }
                }
            });
            return null;
        }
    }
}
