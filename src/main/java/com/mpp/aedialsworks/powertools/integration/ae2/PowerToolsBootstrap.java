package com.mpp.aedialsworks.powertools.integration.ae2;
import appeng.api.features.GridLinkables;
import appeng.api.upgrades.Upgrades;
import appeng.api.parts.PartModels;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.core.definitions.AEItems;
import com.mpp.aedialsworks.common.registry.*;
import com.mpp.aedialsworks.common.util.AWIds;
import com.mpp.aedialsworks.powertools.menu.PowerToolsMenu;
public final class PowerToolsBootstrap {
    private PowerToolsBootstrap(){}
    public static void setup(){
        PowerToolsMenu.registerOpener();
        GridLinkables.register(AWItems.REMOTE_STORAGE_MONITOR.get(),WirelessTerminalItem.LINKABLE_HANDLER);
        Upgrades.add(AEItems.ENERGY_CARD,AWItems.REMOTE_STORAGE_MONITOR.get(),2);
        AWBlocks.BETTER_LEVEL_MAINTAINER.get().setBlockEntity(PowerBlockEntity.class,AWBlockEntities.BETTER_LEVEL_MAINTAINER.get(),null,null);
        AEBaseBlockEntity.registerBlockEntityItem(AWBlockEntities.BETTER_LEVEL_MAINTAINER.get(),AWItems.BETTER_LEVEL_MAINTAINER.get());
        AWBlocks.AUTO_CRAFTER.get().setBlockEntity(PowerBlockEntity.class,AWBlockEntities.AUTO_CRAFTER.get(),null,null);
        AEBaseBlockEntity.registerBlockEntityItem(AWBlockEntities.AUTO_CRAFTER.get(),AWItems.AUTO_CRAFTER.get());
        AWBlocks.STORAGE_LEVEL_EMITTER.get().setBlockEntity(PowerBlockEntity.class,AWBlockEntities.STORAGE_LEVEL_EMITTER.get(),null,null);
        AEBaseBlockEntity.registerBlockEntityItem(AWBlockEntities.STORAGE_LEVEL_EMITTER.get(),AWItems.STORAGE_LEVEL_EMITTER.get());
        AWBlocks.STORAGE_DISPLAY.get().setBlockEntity(PowerBlockEntity.class,AWBlockEntities.STORAGE_DISPLAY.get(),null,null);
        AEBaseBlockEntity.registerBlockEntityItem(AWBlockEntities.STORAGE_DISPLAY.get(),AWItems.STORAGE_DISPLAY.get());
        AWBlocks.STORAGE_LEVEL_ALARM.get().setBlockEntity(PowerBlockEntity.class,AWBlockEntities.STORAGE_LEVEL_ALARM.get(),null,null);
        AEBaseBlockEntity.registerBlockEntityItem(AWBlockEntities.STORAGE_LEVEL_ALARM.get(),AWItems.STORAGE_LEVEL_ALARM.get());
    }
    public static void models(){PartModels.registerModels(AWIds.id(MonitorPart.model(true,0)),AWIds.id(MonitorPart.model(false,0)),AWIds.id(MonitorPart.model(false,1)),AWIds.id(MonitorPart.model(false,2)));}
}
