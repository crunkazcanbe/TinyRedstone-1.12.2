package com.dannyandson.tinyredstone.network;

import com.dannyandson.tinyredstone.blocks.PanelTile;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PlaySound implements IMessage {
    private BlockPos pos;
    private String namespace;
    private String path;
    private float volume;
    private float pitch;

    public PlaySound() {}

    public PlaySound(BlockPos pos, String namespace, String path, float volume, float pitch)
    {
        this.pos = pos;
        this.namespace = namespace;
        this.path = path;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        this.pos = buffer.readBlockPos();
        this.namespace = buffer.readString(32767);
        this.path = buffer.readString(32767);
        this.volume = buffer.readFloat();
        this.pitch = buffer.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeBlockPos(pos);
        buffer.writeString(this.namespace);
        buffer.writeString(this.path);
        buffer.writeFloat(volume);
        buffer.writeFloat(pitch);
    }

    public static class Handler implements IMessageHandler<PlaySound, IMessage> {
        @Override
        public IMessage onMessage(PlaySound message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                TileEntity te = Minecraft.getMinecraft().world.getTileEntity(message.pos);
                if (te instanceof PanelTile)
                {
                    te.getWorld().playSound(
                            message.pos.getX(), message.pos.getY(), message.pos.getZ(),
                            new SoundEvent(new ResourceLocation(message.namespace, message.path)),
                            SoundCategory.BLOCKS, message.volume, message.pitch, false
                    );
                }
            });
            return null;
        }
    }
}
