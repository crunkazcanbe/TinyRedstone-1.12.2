package com.dannyandson.tinyredstone.setup;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.blocks.PanelBlock;
import com.dannyandson.tinyredstone.blocks.PanelTile;
import com.dannyandson.tinyredstone.blocks.panelcells.*;
import com.dannyandson.tinyredstone.blocks.panelcovers.DarkCover;
import com.dannyandson.tinyredstone.blocks.panelcovers.LightCover;
import com.dannyandson.tinyredstone.blocks.panelcovers.TrimCover;
import com.dannyandson.tinyredstone.items.*;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = TinyRedstone.MODID)
public class Registration {

    // 1.12.2: plain static fields, populated during RegistryEvent.Register.
    public static PanelBlock REDSTONE_PANEL_BLOCK;
    public static Item REDSTONE_PANEL_ITEM;

    public static Item TINY_SOLID_BLOCK;
    public static Item TINY_TRANSPARENT_BLOCK;

    public static Item TINY_REDSTONE_ITEM;
    public static Item TINY_REDSTONE_TORCH;
    public static Item TINY_REPEATER;
    public static Item TINY_REDSTONE_BLOCK;
    public static Item TINY_COMPARATOR;
    public static Item TINY_PISTON;
    public static Item TINY_STICKY_PISTON;
    public static Item TINY_REDSTONE_LAMP;
    public static Item TINY_BUTTON;
    public static Item TINY_STONE_BUTTON;
    public static Item TINY_OBSERVER;
    public static Item TINY_SUPER_REPEATER;
    public static Item TINY_LEVER;
    public static Item TINY_REDSTONE_BRIDGE;
    public static Item TINY_NOTE_BLOCK;
    public static Item LOGIC_GATE;

    public static Item BLUEPRINT;

    public static Item SILICON;
    public static Item SILICON_COMPOUND;

    public static RedstoneWrench REDSTONE_WRENCH;
    public static RedstoneWrench TINY_COLOR_SELECTOR;

    public static Item PANEL_COVER_DARK;
    public static Item PANEL_COVER_LIGHT;
    public static Item PANEL_COVER_TRIM;

    // PORT-TODO: 1.16 codec/reload-listener texture-override system (CodecTinyBlockOverrides + TinyBlockData)
    //            was dropped for v0.1 (no 1.12.2 Codec/DataFixer equivalent). Tiny blocks/covers use their
    //            default textures. This set survives as the server-side "valid tiny block texture" cache that
    //            the chopper menu sync used to live on TinyBlockData.
    public static final java.util.Set<String> validBlockTextureCache = new java.util.HashSet<>();

    // 1.12.2: PropertyBool replaces 1.16 BooleanProperty.
    public static final PropertyBool HAS_PANEL_BASE = PropertyBool.create("has_panel_base");

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        REDSTONE_PANEL_BLOCK = (PanelBlock) new PanelBlock()
                .setRegistryName(new ResourceLocation(TinyRedstone.MODID, "redstone_panel"))
                .setUnlocalizedName(TinyRedstone.MODID + ".redstone_panel");
        // 1.12.2: an ItemBlock reliably appears in creative when the BLOCK carries the tab
        // (the PanelItem's own setCreativeTab wasn't enough on its own here).
        REDSTONE_PANEL_BLOCK.setCreativeTab(ModSetup.ITEM_GROUP);

        event.getRegistry().register(REDSTONE_PANEL_BLOCK);

        // TileEntities are registered here (block phase) in 1.12.2.
        GameRegistry.registerTileEntity(PanelTile.class, new ResourceLocation(TinyRedstone.MODID, "redstone_panel"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // Block items
        REDSTONE_PANEL_ITEM = setup(new PanelItem(), "redstone_panel");

        TINY_SOLID_BLOCK = setup(new TinyBlockItem(), "tiny_solid_block");
        TINY_TRANSPARENT_BLOCK = setup(new TinyBlockItem(), "tiny_transparent_block");

        TINY_REDSTONE_ITEM = setup(new PanelCellItem(), "tiny_redstone");
        TINY_REDSTONE_TORCH = setup(new PanelCellItem(), "tiny_redstone_torch");
        TINY_REPEATER = setup(new PanelCellItem(), "tiny_repeater");
        TINY_REDSTONE_BLOCK = setup(new PanelCellItem(), "tiny_redstone_block");
        TINY_COMPARATOR = setup(new PanelCellItem(), "tiny_comparator");
        TINY_PISTON = setup(new PanelCellItem(), "tiny_piston");
        TINY_STICKY_PISTON = setup(new PanelCellItem(), "tiny_sticky_piston");
        TINY_REDSTONE_LAMP = setup(new PanelCellItem(), "tiny_redstone_lamp");
        TINY_BUTTON = setup(new PanelCellItem(), "tiny_button");
        TINY_STONE_BUTTON = setup(new PanelCellItem(), "tiny_stone_button");
        TINY_OBSERVER = setup(new PanelCellItem(), "tiny_observer");
        TINY_SUPER_REPEATER = setup(new PanelCellItem(), "tiny_super_repeater");
        TINY_LEVER = setup(new PanelCellItem(), "tiny_lever");
        TINY_REDSTONE_BRIDGE = setup(new PanelCellItem(), "tiny_redstone_bridge");
        TINY_NOTE_BLOCK = setup(new PanelCellItem(), "tiny_note_block");
        LOGIC_GATE = setup(new PanelCellItem(), "logic_gate");

        BLUEPRINT = setup(new Blueprint(), "blueprint");

        SILICON = setup(new Item().setCreativeTab(ModSetup.ITEM_GROUP), "silicon");
        SILICON_COMPOUND = setup(new Item().setCreativeTab(ModSetup.ITEM_GROUP), "silicon_compound");

        REDSTONE_WRENCH = (RedstoneWrench) setup(new RedstoneWrench(), "redstone_wrench");
        TINY_COLOR_SELECTOR = (RedstoneWrench) setup(new TinyColorSelector(), "tiny_color_selector");

        PANEL_COVER_DARK = setup(new PanelCoverItem(), "dark_panel_cover");
        PANEL_COVER_LIGHT = setup(new PanelCoverItem(), "light_panel_cover");
        PANEL_COVER_TRIM = setup(new PanelCoverItem(), "trim_panel_cover");

        // REDSTONE_PANEL_ITEM (a PanelItem, an ItemBlock subclass) IS the panel's item block,
        // already named "redstone_panel" — register it once. (A second plain ItemBlock with the
        // same name was a duplicate that crashed Forge's registry id-sync.)
        event.getRegistry().register(REDSTONE_PANEL_ITEM);

        event.getRegistry().register(TINY_SOLID_BLOCK);
        event.getRegistry().register(TINY_TRANSPARENT_BLOCK);

        event.getRegistry().register(TINY_REDSTONE_ITEM);
        event.getRegistry().register(TINY_REDSTONE_TORCH);
        event.getRegistry().register(TINY_REPEATER);
        event.getRegistry().register(TINY_REDSTONE_BLOCK);
        event.getRegistry().register(TINY_COMPARATOR);
        event.getRegistry().register(TINY_PISTON);
        event.getRegistry().register(TINY_STICKY_PISTON);
        event.getRegistry().register(TINY_REDSTONE_LAMP);
        event.getRegistry().register(TINY_BUTTON);
        event.getRegistry().register(TINY_STONE_BUTTON);
        event.getRegistry().register(TINY_OBSERVER);
        event.getRegistry().register(TINY_SUPER_REPEATER);
        event.getRegistry().register(TINY_LEVER);
        event.getRegistry().register(TINY_REDSTONE_BRIDGE);
        event.getRegistry().register(TINY_NOTE_BLOCK);
        event.getRegistry().register(LOGIC_GATE);

        event.getRegistry().register(BLUEPRINT);
        event.getRegistry().register(SILICON);
        event.getRegistry().register(SILICON_COMPOUND);
        event.getRegistry().register(REDSTONE_WRENCH);
        event.getRegistry().register(TINY_COLOR_SELECTOR);
        event.getRegistry().register(PANEL_COVER_DARK);
        event.getRegistry().register(PANEL_COVER_LIGHT);
        event.getRegistry().register(PANEL_COVER_TRIM);
    }

    /**
     * Helper: set registry name + translation key on an item (mirrors the old per-item Properties).
     */
    private static Item setup(Item item, String name) {
        return item
                .setRegistryName(new ResourceLocation(TinyRedstone.MODID, name))
                .setUnlocalizedName(TinyRedstone.MODID + "." + name);
    }

    //called at FMLCommonSetupEvent in ModSetup (init)
    public static void registerPanelCells() {
        TinyRedstone.registerPanelCell(RedstoneDust.class, TINY_REDSTONE_ITEM);
        TinyRedstone.registerPanelCell(Repeater.class, TINY_REPEATER);
        TinyRedstone.registerPanelCell(Torch.class, TINY_REDSTONE_TORCH);
        TinyRedstone.registerPanelCell(Comparator.class, TINY_COMPARATOR);
        TinyRedstone.registerPanelCell(RedstoneBlock.class, TINY_REDSTONE_BLOCK);
        TinyRedstone.registerPanelCell(TinyBlock.class, TINY_SOLID_BLOCK);
        TinyRedstone.registerPanelCell(Piston.class, TINY_PISTON);
        TinyRedstone.registerPanelCell(StickyPiston.class, TINY_STICKY_PISTON);
        TinyRedstone.registerPanelCell(RedstoneLamp.class, TINY_REDSTONE_LAMP);
        TinyRedstone.registerPanelCell(TransparentBlock.class, TINY_TRANSPARENT_BLOCK);
        TinyRedstone.registerPanelCell(Button.class, TINY_BUTTON);
        TinyRedstone.registerPanelCell(StoneButton.class, TINY_STONE_BUTTON);
        TinyRedstone.registerPanelCell(Observer.class, TINY_OBSERVER);
        TinyRedstone.registerPanelCell(SuperRepeater.class, TINY_SUPER_REPEATER);
        TinyRedstone.registerPanelCell(Lever.class, TINY_LEVER);
        TinyRedstone.registerPanelCell(RedstoneBridge.class, TINY_REDSTONE_BRIDGE);
        TinyRedstone.registerPanelCell(NoteBlock.class, TINY_NOTE_BLOCK);
        TinyRedstone.registerPanelCell(LogicGate.class, LOGIC_GATE);

        TinyRedstone.registerPanelCover(DarkCover.class, PANEL_COVER_DARK);
        TinyRedstone.registerPanelCover(LightCover.class, PANEL_COVER_LIGHT);
        TinyRedstone.registerPanelCover(TrimCover.class, PANEL_COVER_TRIM);
    }

    /**
     * 1.12.2 item model registration (replaces 1.16 datagen/automatic item models).
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        registerModel(REDSTONE_PANEL_ITEM);
        registerModel(TINY_SOLID_BLOCK);
        registerModel(TINY_TRANSPARENT_BLOCK);
        registerModel(TINY_REDSTONE_ITEM);
        registerModel(TINY_REDSTONE_TORCH);
        registerModel(TINY_REPEATER);
        registerModel(TINY_REDSTONE_BLOCK);
        registerModel(TINY_COMPARATOR);
        registerModel(TINY_PISTON);
        registerModel(TINY_STICKY_PISTON);
        registerModel(TINY_REDSTONE_LAMP);
        registerModel(TINY_BUTTON);
        registerModel(TINY_STONE_BUTTON);
        registerModel(TINY_OBSERVER);
        registerModel(TINY_SUPER_REPEATER);
        registerModel(TINY_LEVER);
        registerModel(TINY_REDSTONE_BRIDGE);
        registerModel(TINY_NOTE_BLOCK);
        registerModel(LOGIC_GATE);
        registerModel(BLUEPRINT);
        registerModel(SILICON);
        registerModel(SILICON_COMPOUND);
        registerModel(REDSTONE_WRENCH);
        registerModel(TINY_COLOR_SELECTOR);
        registerModel(PANEL_COVER_DARK);
        registerModel(PANEL_COVER_LIGHT);
        registerModel(PANEL_COVER_TRIM);
    }

    @SideOnly(Side.CLIENT)
    private static void registerModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
