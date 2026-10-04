package com.dannyandson.tinyredstone;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 1.12.2 config (net.minecraftforge.common.config.Configuration) replacing the 1.16 ForgeConfigSpec.
 *
 * NOTE: 1.16 wrapped every value in a ForgeConfigSpec.*Value with a .get() accessor. 1.12.2's
 * Configuration reads plain values up-front, so these fields hold the resolved value directly.
 * PORT-TODO: any other (already/yet-to-be converted) file that called e.g. Config.TORCH_LIGHT.get()
 *            must be changed to read Config.TORCH_LIGHT directly (no .get()).
 */
public class Config {

    public static final String CATEGORY_FEATURE = "feature";
    public static final String CATEGORY_PERFORMANCE = "performance";

    // Same names/defaults as the 1.16 spec, now resolved values.
    public static boolean TORCH_LIGHT = false;
    public static int DISPLAY_MODE = 1;
    public static boolean JSON_BLUEPRINT = true;
    public static int SUPER_REPEATER_MAX = 1000;
    public static int CIRCUIT_MAX_ITERATION = 32;
    public static List<String> REDSTONE_WIRE_LIST = Arrays.asList("redstonepen:track", "cb_multipart:multipart");
    public static boolean ALLOW_WORLD_PLACEMENT = true;

    private static Configuration config;

    public static void load(FMLPreInitializationEvent event) {
        File configFile = new File(event.getModConfigurationDirectory(), TinyRedstone.MODID + ".cfg");
        config = new Configuration(configFile);
        config.load();
        sync();
    }

    private static void sync() {
        // ---- feature ----
        JSON_BLUEPRINT = config.getBoolean("json_blueprint", CATEGORY_FEATURE, true,
                "Should it be possible to export or import the blueprint as json? (default:true)");

        // ---- performance (server-side values in 1.16) ----
        TORCH_LIGHT = config.getBoolean("torch_light", CATEGORY_PERFORMANCE, false,
                "Should redstone torches output light to the surrounding area? (default:false)");

        SUPER_REPEATER_MAX = config.getInt("super_repeater_max", CATEGORY_PERFORMANCE, 1000, 4, Integer.MAX_VALUE,
                "Maximum redstone tick delay for super repeaters. 10 redstone ticks = 1 second. (default:1000)");

        CIRCUIT_MAX_ITERATION = config.getInt("max_zero_tick_run", CATEGORY_PERFORMANCE, 32, 4, 1024,
                "How many blocks long can a line of redstone run in a single tick?" +
                        "\nThis number determines approximately 2x how many zero tick super repeaters can extend a single redstone line?" +
                        "\n(Since each repeater can extend signal 2 full blocks.)" +
                        "\nVery large numbers may degrade performance and potentially risk crash. (default=32)");

        ALLOW_WORLD_PLACEMENT = config.getBoolean("allow_world_placement", CATEGORY_PERFORMANCE, true,
                "Allow components to be placed anywhere, not just on panels. (default: true)");

        String[] defaultWires = {"redstonepen:track", "cb_multipart:multipart"};
        REDSTONE_WIRE_LIST = Arrays.asList(config.getStringList("redstone_wires", CATEGORY_PERFORMANCE, defaultWires,
                "List of blocks from other mods to be treated as redstone wire."));

        // ---- performance (client-side value in 1.16) ----
        DISPLAY_MODE = config.getInt("display_mode", CATEGORY_PERFORMANCE, 1, 0, 4,
                "When should the information be displayed in the overlay? 0 = no, 1 = always, 2 = only in extended or debug, 3 = when you have a wrench in your hand, 4 = when you have any component in your hand");

        if (config.hasChanged()) {
            config.save();
        }
    }
}
