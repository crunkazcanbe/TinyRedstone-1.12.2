package com.dannyandson.tinyredstone.setup;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.network.ModNetworkHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * Common (both-sides) proxy. ClientSetup extends this and adds client-only wiring.
 * In 1.16 this lived on the FML mod event bus; in 1.12.2 it is driven via the @SidedProxy
 * from TinyRedstone#init.
 */
public class ModSetup {

    // 1.12.2: CreativeTabs replaces 1.16 ItemGroup.
    public static final CreativeTabs ITEM_GROUP = new CreativeTabs("tinyredstone") {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(Registration.TINY_REDSTONE_TORCH);
        }
    };

    public void init(final FMLInitializationEvent event) {
        Registration.registerPanelCells();
        ModNetworkHandler.registerMessages();
        // PORT-TODO: 1.16 used AddReloadListenerEvent to register Registration.TINY_BLOCK_OVERRIDES
        //            (the data-pack reload listener for tiny block overrides). 1.12.2 has no
        //            AddReloadListenerEvent; reattach via IResourceManagerReloadListener on the client
        //            resource manager, or drop until the codec system is reimplemented.
    }
}
