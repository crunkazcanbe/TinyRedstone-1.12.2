package com.dannyandson.tinyredstone.network;

import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.PanelCellPos;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.panelcells.GateType;
import com.dannyandson.tinyredstone.blocks.panelcells.LogicGate;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client -> server: apply the full Logic Gate option set chosen in {@code LogicGateGUI} to the cell.
 */
public class LogicGateConfigSync implements IMessage {
    private BlockPos pos;
    private int cellIndex;
    private String type;
    private int interval, maxCount, outputStrength, pulseLength, rotation;
    private boolean invertOutput, invBack, invLeft, invRight, subtractMode;

    public LogicGateConfigSync() {}

    public LogicGateConfigSync(BlockPos pos, int cellIndex, LogicGate g) {
        this.pos = pos;
        this.cellIndex = cellIndex;
        this.type = g.getGateType().name();
        this.interval = g.getInterval();
        this.maxCount = g.getMaxCount();
        this.outputStrength = g.getOutputStrength();
        this.pulseLength = g.getPulseLength();
        this.rotation = g.getRotation();
        this.invertOutput = g.isInvertOutput();
        this.invBack = g.isInvBack();
        this.invLeft = g.isInvLeft();
        this.invRight = g.isInvRight();
        this.subtractMode = g.isSubtractMode();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer b = new PacketBuffer(buf);
        pos = b.readBlockPos();
        cellIndex = b.readInt();
        type = b.readString(64);
        interval = b.readInt();
        maxCount = b.readInt();
        outputStrength = b.readInt();
        pulseLength = b.readInt();
        rotation = b.readInt();
        invertOutput = b.readBoolean();
        invBack = b.readBoolean();
        invLeft = b.readBoolean();
        invRight = b.readBoolean();
        subtractMode = b.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer b = new PacketBuffer(buf);
        b.writeBlockPos(pos);
        b.writeInt(cellIndex);
        b.writeString(type == null ? "AND" : type);
        b.writeInt(interval);
        b.writeInt(maxCount);
        b.writeInt(outputStrength);
        b.writeInt(pulseLength);
        b.writeInt(rotation);
        b.writeBoolean(invertOutput);
        b.writeBoolean(invBack);
        b.writeBoolean(invLeft);
        b.writeBoolean(invRight);
        b.writeBoolean(subtractMode);
    }

    public static class Handler implements IMessageHandler<LogicGateConfigSync, IMessage> {
        @Override
        public IMessage onMessage(LogicGateConfigSync m, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.getServerWorld().getTileEntity(m.pos);
                if (te instanceof PanelTile) {
                    PanelCellPos cellPos = PanelCellPos.fromIndex((PanelTile) te, m.cellIndex);
                    IPanelCell cell = cellPos.getIPanelCell();
                    if (cell instanceof LogicGate) {
                        ((LogicGate) cell).applyConfig(GateType.byName(m.type), m.interval, m.maxCount,
                                m.outputStrength, m.invertOutput, m.invBack, m.invLeft, m.invRight,
                                m.pulseLength, m.subtractMode, m.rotation);
                        ((PanelTile) te).flagSync();
                    }
                }
            });
            return null;
        }
    }
}
