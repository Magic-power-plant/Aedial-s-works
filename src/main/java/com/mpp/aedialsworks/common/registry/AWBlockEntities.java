package com.mpp.aedialsworks.common.registry;

import com.mpp.aedialsworks.Aedialsworks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Central registration point; feature domains declare their entries here. */
public final class AWBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Aedialsworks.MODID);

    public static final net.minecraftforge.registries.RegistryObject<BlockEntityType<com.mpp.aedialsworks.powertools.integration.ae2.PowerBlockEntity>> BETTER_LEVEL_MAINTAINER=BLOCK_ENTITY_TYPES.register("better_level_maintainer",()->BlockEntityType.Builder.of((pos,state)->new com.mpp.aedialsworks.powertools.integration.ae2.PowerBlockEntity(AWBlockEntities.BETTER_LEVEL_MAINTAINER.get(),pos,state),AWBlocks.BETTER_LEVEL_MAINTAINER.get()).build(null));
    public static final net.minecraftforge.registries.RegistryObject<BlockEntityType<com.mpp.aedialsworks.powertools.integration.ae2.PowerBlockEntity>> AUTO_CRAFTER=BLOCK_ENTITY_TYPES.register("auto_crafter",()->BlockEntityType.Builder.of((pos,state)->new com.mpp.aedialsworks.powertools.integration.ae2.PowerBlockEntity(AWBlockEntities.AUTO_CRAFTER.get(),pos,state),AWBlocks.AUTO_CRAFTER.get()).build(null));
    public static final net.minecraftforge.registries.RegistryObject<BlockEntityType<com.mpp.aedialsworks.powertools.integration.ae2.PowerBlockEntity>> STORAGE_LEVEL_EMITTER=BLOCK_ENTITY_TYPES.register("storage_level_emitter",()->BlockEntityType.Builder.of((pos,state)->new com.mpp.aedialsworks.powertools.integration.ae2.PowerBlockEntity(AWBlockEntities.STORAGE_LEVEL_EMITTER.get(),pos,state),AWBlocks.STORAGE_LEVEL_EMITTER.get()).build(null));
    public static final net.minecraftforge.registries.RegistryObject<BlockEntityType<com.mpp.aedialsworks.powertools.integration.ae2.PowerBlockEntity>> STORAGE_DISPLAY=BLOCK_ENTITY_TYPES.register("storage_display",()->BlockEntityType.Builder.of((pos,state)->new com.mpp.aedialsworks.powertools.integration.ae2.PowerBlockEntity(AWBlockEntities.STORAGE_DISPLAY.get(),pos,state),AWBlocks.STORAGE_DISPLAY.get()).build(null));
    public static final net.minecraftforge.registries.RegistryObject<BlockEntityType<com.mpp.aedialsworks.powertools.integration.ae2.PowerBlockEntity>> STORAGE_LEVEL_ALARM=BLOCK_ENTITY_TYPES.register("storage_level_alarm",()->BlockEntityType.Builder.of((pos,state)->new com.mpp.aedialsworks.powertools.integration.ae2.PowerBlockEntity(AWBlockEntities.STORAGE_LEVEL_ALARM.get(),pos,state),AWBlocks.STORAGE_LEVEL_ALARM.get()).build(null));
    private AWBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
