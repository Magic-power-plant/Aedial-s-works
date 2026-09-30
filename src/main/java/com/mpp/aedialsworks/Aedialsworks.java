package com.mpp.aedialsworks;

import com.mojang.logging.LogUtils;
import com.mpp.aedialsworks.common.config.AWConfigs;
import com.mpp.aedialsworks.common.network.AWNetwork;
import com.mpp.aedialsworks.common.registry.*;
import com.mpp.aedialsworks.data.AWDataGenerators;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Aedialsworks.MODID)
public final class Aedialsworks {
    public static final String MODID = "aedialsworks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Aedialsworks() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        AWCells.initialize();
        AWItems.register(bus);
        AWBlocks.register(bus);
        AWBlockEntities.register(bus);
        AWMenus.register(bus);
        AWRecipeSerializers.register(bus);
        AWCreativeTabs.register(bus);
        AWConfigs.register(ModLoadingContext.get());
        bus.addListener(this::commonSetup);
        bus.addListener(AWDataGenerators::gatherData);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AWNetwork.init();
            com.mpp.aedialsworks.cellterminal.integration.ae2.CellTerminalBootstrap.setup();
            com.mpp.aedialsworks.powertools.integration.ae2.PowerToolsBootstrap.setup();
            com.mpp.aedialsworks.cells.integration.ae2.CellsBootstrap.setup();
            LOGGER.info("Aedial's Works Cell Terminal, PowerTools and CELLS ready (network protocol {})", AWNetwork.PROTOCOL_VERSION);
        });
    }
}
