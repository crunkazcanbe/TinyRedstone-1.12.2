package com.dannyandson.tinyredstone.network;

import com.dannyandson.tinyredstone.blocks.RenderHelper;
import com.dannyandson.tinyredstone.setup.Registration;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

public class ValidTinyBlockCacheSync implements IMessage {

    ResourceLocation itemRegistryName;
    BlockPos chopperPos;

    public ValidTinyBlockCacheSync() {}

    public ValidTinyBlockCacheSync(@Nullable BlockPos chopperPos, ResourceLocation itemRegistryName)
    {
        this.itemRegistryName=itemRegistryName;
        this.chopperPos = chopperPos;
    }

    @Override
    public void fromBytes(ByteBuf buf){
        PacketBuffer buffer = new PacketBuffer(buf);
        this.itemRegistryName=buffer.readResourceLocation();
        try {
            this.chopperPos=buffer.readBlockPos();
        }catch (IndexOutOfBoundsException e){
            this.chopperPos=null;
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeResourceLocation(this.itemRegistryName);
        if (this.chopperPos!=null)
            buffer.writeBlockPos(this.chopperPos);
    }

    public static class ClientHandler implements IMessageHandler<ValidTinyBlockCacheSync, IMessage> {
        @Override
        public IMessage onMessage(ValidTinyBlockCacheSync message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ResourceLocation texture1 = new ResourceLocation(message.itemRegistryName.getResourceDomain(), "block/" + message.itemRegistryName.getResourcePath());
                ResourceLocation texture2 = new ResourceLocation(message.itemRegistryName.getResourceDomain(), "block/" + message.itemRegistryName.getResourcePath() + "_side");
                TextureAtlasSprite sprite1 = RenderHelper.getSprite(texture1);
                TextureAtlasSprite sprite2 = RenderHelper.getSprite(texture2);
                // PORT-TODO: 1.16 used TextureManager.INTENTIONAL_MISSING_TEXTURE as the "missing" sentinel.
                // 1.12.2 equivalent is the block atlas's missing sprite. Verify RenderHelper.getSprite returns
                // this same instance for unknown textures so the comparison stays valid.
                TextureAtlasSprite missing = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
                if (sprite1!=missing || sprite2!=missing) {
                    //tell server chopper menu at block pos to update
                    ModNetworkHandler.sendToServer(new ValidTinyBlockCacheSync(message.chopperPos, message.itemRegistryName));
                }
            });
            return null;
        }
    }

    public static class ServerHandler implements IMessageHandler<ValidTinyBlockCacheSync, IMessage> {
        @Override
        public IMessage onMessage(ValidTinyBlockCacheSync message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!Registration.validBlockTextureCache.contains(message.itemRegistryName.toString()))
                    Registration.validBlockTextureCache.add(message.itemRegistryName.toString());
                // Block-cutter output-type update was dropped for v0.1; the texture-cache add above is the remaining behavior.
            });
            return null;
        }
    }

}
