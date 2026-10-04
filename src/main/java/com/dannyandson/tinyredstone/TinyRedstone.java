package com.dannyandson.tinyredstone;

import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.api.IPanelCover;
import com.dannyandson.tinyredstone.blocks.PanelBlock;
import com.dannyandson.tinyredstone.setup.ClientSetup;
import com.dannyandson.tinyredstone.setup.ModSetup;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = TinyRedstone.MODID, name = TinyRedstone.NAME, version = TinyRedstone.VERSION)
public class TinyRedstone {
    public static final String MODID = "tinyredstone";
    public static final String NAME = "Tiny Redstone";
    public static final String VERSION = "1.12.2-0.1.0";

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    @Mod.Instance(MODID)
    public static TinyRedstone INSTANCE;

    // Sided proxy: CommonSetup on both sides, ClientSetup only on the client.
    @SidedProxy(
            clientSide = "com.dannyandson.tinyredstone.setup.ClientSetup",
            serverSide = "com.dannyandson.tinyredstone.setup.ModSetup")
    public static ModSetup proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 1.12.2: Config loads from the suggested config file during preInit.
        Config.load(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Registration of panel cells / cover types (was FMLCommonSetupEvent in 1.16).
        proxy.init(event);
    }

    public static void registerPanelCell(Class<? extends IPanelCell> iPanelCellClass, Item correspondingItem)
    {
        PanelBlock.registerPanelCell(iPanelCellClass, correspondingItem);
    }

    public static void registerPanelCover(Class<? extends IPanelCover> iPanelCoverClass, Item correspondingItem)
    {
        PanelBlock.registerPanelCover(iPanelCoverClass, correspondingItem);
    }
}
