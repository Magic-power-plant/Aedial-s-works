package com.mpp.aedialsworks.common.registry;

import com.mpp.aedialsworks.Aedialsworks;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Central registration point; feature domains declare their entries here. */
public final class AWBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Aedialsworks.MODID);

    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.integration.ae2.PowerMachineBlock> BETTER_LEVEL_MAINTAINER=BLOCKS.register("better_level_maintainer",()->new com.mpp.aedialsworks.powertools.integration.ae2.PowerMachineBlock(com.mpp.aedialsworks.powertools.MachineKind.MAINTAINER));

    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.integration.ae2.PowerMachineBlock> AUTO_CRAFTER=BLOCKS.register("auto_crafter",()->new com.mpp.aedialsworks.powertools.integration.ae2.PowerMachineBlock(com.mpp.aedialsworks.powertools.MachineKind.CRAFTER));

    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.integration.ae2.PowerMachineBlock> STORAGE_LEVEL_EMITTER=BLOCKS.register("storage_level_emitter",()->new com.mpp.aedialsworks.powertools.integration.ae2.PowerMachineBlock(com.mpp.aedialsworks.powertools.MachineKind.EMITTER));

    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.integration.ae2.PowerMachineBlock> STORAGE_DISPLAY=BLOCKS.register("storage_display",()->new com.mpp.aedialsworks.powertools.integration.ae2.PowerMachineBlock(com.mpp.aedialsworks.powertools.MachineKind.DISPLAY));

    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.integration.ae2.PowerMachineBlock> STORAGE_LEVEL_ALARM=BLOCKS.register("storage_level_alarm",()->new com.mpp.aedialsworks.powertools.integration.ae2.PowerMachineBlock(com.mpp.aedialsworks.powertools.MachineKind.ALARM));

    private AWBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
