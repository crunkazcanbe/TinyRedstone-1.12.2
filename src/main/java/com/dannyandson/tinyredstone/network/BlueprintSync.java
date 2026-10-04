package com.dannyandson.tinyredstone.network;

import com.dannyandson.tinyredstone.items.Blueprint;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class BlueprintSync implements IMessage {

    private NBTTagCompound nbt;

    public BlueprintSync() {}

    public BlueprintSync(NBTTagCompound nbt)
    {
        this.nbt=nbt;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        try {
            this.nbt = new PacketBuffer(buf).readCompoundTag();
        } catch (java.io.IOException e) {
            this.nbt = null;
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        new PacketBuffer(buf).writeCompoundTag(nbt);
    }

    public static class Handler implements IMessageHandler<BlueprintSync, IMessage> {
        @Override
        public IMessage onMessage(BlueprintSync message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack blueprint = player.getHeldItemMainhand();
                if (blueprint.getItem() instanceof Blueprint) {
                    blueprint.setTagCompound(message.nbt);
                }
            });
            return null;
        }
    }
}
