package com.dannyandson.tinyredstone.setup;

import com.dannyandson.tinyredstone.ClientBinding;
import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.PanelTileColor;
import com.dannyandson.tinyredstone.blocks.PanelTileRenderer;
import com.dannyandson.tinyredstone.blocks.panelcells.*;
import com.dannyandson.tinyredstone.blocks.panelcovers.DarkCover;
import com.dannyandson.tinyredstone.blocks.panelcovers.LightCover;
import com.dannyandson.tinyredstone.gui.ToolbarOverlay;
import com.dannyandson.tinyredstone.items.PanelItemColor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = TinyRedstone.MODID, value = Side.CLIENT)
public class ClientSetup extends ModSetup {

    @Override
    public void init(final FMLInitializationEvent event) {
        // Run the common setup first (panel cells, networking).
        super.init(event);

        // Bind the TileEntity renderer for the panel (was TinyRedstone#doClientStuff in 1.16).
        TinyRedstone.LOGGER.info("Binding Renderer for Redstone Panel tile entity.");
        ClientRegistry.bindTileEntitySpecialRenderer(PanelTile.class, new PanelTileRenderer());
        PanelTileRenderer.register();

        Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(new PanelTileColor(), Registration.REDSTONE_PANEL_BLOCK);
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler(new PanelItemColor(), Registration.REDSTONE_PANEL_ITEM);

        ClientBinding.registerKeyBindings();
    }

    @SuppressWarnings("unused")
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        // PORT-TODO: 1.16 filtered to the block atlas via event.getMap().location()/PlayerContainer.BLOCK_ATLAS.
        //            1.12.2 only stitches the block atlas here, so the filter is unnecessary; verify all the
        //            registerSprite ResourceLocations resolve once the cell/cover classes are converted.
        event.getMap().registerSprite(PanelTileRenderer.TEXTURE);
        event.getMap().registerSprite(PanelTileRenderer.TEXTURE_CRASHED);

        event.getMap().registerSprite(Repeater.TEXTURE_REPEATER_ON);
        event.getMap().registerSprite(Repeater.TEXTURE_REPEATER_OFF);
        event.getMap().registerSprite(RedstoneDust.TEXTURE_REDSTONE_DUST);
        event.getMap().registerSprite(RedstoneDust.TEXTURE_REDSTONE_DUST_OFF);
        event.getMap().registerSprite(RedstoneDust.TEXTURE_REDSTONE_DUST_SEGMENT);
        event.getMap().registerSprite(RedstoneDust.TEXTURE_REDSTONE_DUST_SEGMENT_ON);
        event.getMap().registerSprite(RedstoneDust.TEXTURE_REDSTONE_DUST_SEGMENT_OFF);
        event.getMap().registerSprite(Torch.TEXTURE_TORCH_ON);
        event.getMap().registerSprite(Torch.TEXTURE_TORCH_OFF);
        event.getMap().registerSprite(Torch.TEXTURE_TORCH_TOP_ON);
        event.getMap().registerSprite(Torch.TEXTURE_TORCH_TOP_OFF);
        event.getMap().registerSprite(Piston.TEXTURE_PISTON_SIDE_TOP);
        event.getMap().registerSprite(Piston.TEXTURE_PISTON_SIDE_BOTTOM);
        event.getMap().registerSprite(SuperRepeater.TEXTURE_SUPER_REPEATER_ON);
        event.getMap().registerSprite(SuperRepeater.TEXTURE_SUPER_REPEATER_OFF);
        event.getMap().registerSprite(Lever.TEXTURE_LEVER);
        event.getMap().registerSprite(Lever.TEXTURE_LEVER_TOP);
        event.getMap().registerSprite(Comparator.TEXTURE_COMPARATOR_OFF);
        event.getMap().registerSprite(Comparator.TEXTURE_COMPARATOR_ON);
        event.getMap().registerSprite(Comparator.TEXTURE_COMPARATOR_SUBTRACT_OFF);
        event.getMap().registerSprite(Comparator.TEXTURE_COMPARATOR_SUBTRACT_ON);
        event.getMap().registerSprite(TransparentBlock.TEXTURE_TRANSPARENT_BLOCK);
        event.getMap().registerSprite(GhostRenderer.TEXTURE_REDSTONE_DUST_SEGMENT_GHOST);
        event.getMap().registerSprite(TinyBlock.TEXTURE_GRASS_BLOCK_TOP);

        event.getMap().registerSprite(LightCover.TEXTURE_LIGHT_COVER);
        event.getMap().registerSprite(DarkCover.TEXTURE_DEFAULT_COVER);

        event.getMap().registerSprite(ToolbarOverlay.TEXTURE_ROTATION_LOCK);

        // Logic Gate: hand-drawn fallback top-face per gate type (used when Project Red isn't present)
        for (GateType gt : GateType.values()) {
            event.getMap().registerSprite(new net.minecraft.util.ResourceLocation(
                    TinyRedstone.MODID, "block/gate_" + gt.name().toLowerCase()));
        }
        // Reuse Project Red's REAL gate faces (board base + per-gate surface) when it's installed
        if (net.minecraftforge.fml.common.Loader.isModLoaded("projectred-integration")) {
            event.getMap().registerSprite(new net.minecraft.util.ResourceLocation("projectred", "blocks/integration/base"));
            for (GateType gt : GateType.values()) {
                String pr = gt.prSurface();
                if (pr != null) {
                    event.getMap().registerSprite(new net.minecraft.util.ResourceLocation(
                            "projectred", "blocks/integration/surface/" + pr + "-0"));
                }
            }
        }
    }
}
