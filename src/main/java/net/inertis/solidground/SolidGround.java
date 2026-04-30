package net.inertis.solidground;

// net imports
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// java imports

// com imports
import com.mojang.logging.LogUtils;
import net.inertis.solidground.config.SolidGroundConfigCommon;

// logger import
import org.slf4j.Logger;


/**
 * Main Class
 */
@Mod(SolidGround.MODID)
public class SolidGround
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "solidground";
    // Directly reference an SLF4J logger
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Main Class Constructor
     * Registers ModEventBuses
     */
    public SolidGround()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SolidGroundConfigCommon.SPEC);

        // --- NEW: REGISTER PACKETS ---
        net.inertis.solidground.network.NetworkHandler.register();

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Solid Ground mod: Common setup complete.");
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        // Add items to creative mode tabs here
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Solid Ground mod: Server starting...");
    }


    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("Solid Ground mod: Client setup complete.");
        }
    }
}
