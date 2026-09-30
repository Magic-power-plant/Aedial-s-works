package com.mpp.aedialsworks.common.registry;

import com.mpp.aedialsworks.Aedialsworks;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Central registration point; feature domains declare their entries here. */
public final class AWItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Aedialsworks.MODID);

    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.cellterminal.items.ItemCellTerminal> CELL_TERMINAL =
            ITEMS.register("cell_terminal", com.mpp.aedialsworks.cellterminal.items.ItemCellTerminal::new);
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.cellterminal.items.ItemWirelessCellTerminal> WIRELESS_CELL_TERMINAL =
            ITEMS.register("wireless_cell_terminal", com.mpp.aedialsworks.cellterminal.items.ItemWirelessCellTerminal::new);

    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.BlockItem> BETTER_LEVEL_MAINTAINER=ITEMS.register("better_level_maintainer",()->new net.minecraft.world.item.BlockItem(AWBlocks.BETTER_LEVEL_MAINTAINER.get(),new Item.Properties()));
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.BlockItem> AUTO_CRAFTER=ITEMS.register("auto_crafter",()->new net.minecraft.world.item.BlockItem(AWBlocks.AUTO_CRAFTER.get(),new Item.Properties()));
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.BlockItem> STORAGE_LEVEL_EMITTER=ITEMS.register("storage_level_emitter",()->new net.minecraft.world.item.BlockItem(AWBlocks.STORAGE_LEVEL_EMITTER.get(),new Item.Properties()));
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.BlockItem> STORAGE_DISPLAY=ITEMS.register("storage_display",()->new net.minecraft.world.item.BlockItem(AWBlocks.STORAGE_DISPLAY.get(),new Item.Properties()));
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.BlockItem> STORAGE_LEVEL_ALARM=ITEMS.register("storage_level_alarm",()->new net.minecraft.world.item.BlockItem(AWBlocks.STORAGE_LEVEL_ALARM.get(),new Item.Properties()));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.integration.ae2.PowerPartItem> STORAGE_LEVEL_EMITTER_PART=ITEMS.register("storage_level_emitter_part",()->new com.mpp.aedialsworks.powertools.integration.ae2.PowerPartItem(true,0));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.integration.ae2.PowerPartItem> STORAGE_DISPLAY_PART=ITEMS.register("storage_display_part",()->new com.mpp.aedialsworks.powertools.integration.ae2.PowerPartItem(false,0));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.integration.ae2.PowerPartItem> STORAGE_DISPLAY_PART_SMALLER=ITEMS.register("storage_display_part_smaller",()->new com.mpp.aedialsworks.powertools.integration.ae2.PowerPartItem(false,1));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.integration.ae2.PowerPartItem> STORAGE_DISPLAY_PART_SMALLERER=ITEMS.register("storage_display_part_smallerer",()->new com.mpp.aedialsworks.powertools.integration.ae2.PowerPartItem(false,2));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.items.NetworkToolItem> NETWORK_HEALTH_SCANNER=ITEMS.register("network_health_scanner",()->new com.mpp.aedialsworks.powertools.items.NetworkToolItem(com.mpp.aedialsworks.powertools.items.NetworkToolItem.Kind.SCANNER));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.items.NetworkToolItem> NETWORK_COMPONENT_LOCATOR=ITEMS.register("network_component_locator",()->new com.mpp.aedialsworks.powertools.items.NetworkToolItem(com.mpp.aedialsworks.powertools.items.NetworkToolItem.Kind.LOCATOR));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.items.NetworkToolItem> PRIORITY_TUNER=ITEMS.register("priority_tuner",()->new com.mpp.aedialsworks.powertools.items.NetworkToolItem(com.mpp.aedialsworks.powertools.items.NetworkToolItem.Kind.TUNER));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.items.NetworkToolItem> CARDS_DISTRIBUTOR=ITEMS.register("cards_distributor",()->new com.mpp.aedialsworks.powertools.items.NetworkToolItem(com.mpp.aedialsworks.powertools.items.NetworkToolItem.Kind.DISTRIBUTOR));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.items.NetworkToolItem> STORAGE_LEVEL_ALARM_LOCATOR=ITEMS.register("storage_level_alarm_locator",()->new com.mpp.aedialsworks.powertools.items.NetworkToolItem(com.mpp.aedialsworks.powertools.items.NetworkToolItem.Kind.ALARM_LOCATOR));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.items.CrafterSpeedUpgrade> CRAFTER_SPEED_UPGRADE_I=ITEMS.register("crafter_speed_upgrade_i",()->new com.mpp.aedialsworks.powertools.items.CrafterSpeedUpgrade(1));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.items.CrafterSpeedUpgrade> CRAFTER_SPEED_UPGRADE_II=ITEMS.register("crafter_speed_upgrade_ii",()->new com.mpp.aedialsworks.powertools.items.CrafterSpeedUpgrade(2));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.items.CrafterSpeedUpgrade> CRAFTER_SPEED_UPGRADE_III=ITEMS.register("crafter_speed_upgrade_iii",()->new com.mpp.aedialsworks.powertools.items.CrafterSpeedUpgrade(3));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.items.CrafterSpeedUpgrade> CRAFTER_SPEED_UPGRADE_IV=ITEMS.register("crafter_speed_upgrade_iv",()->new com.mpp.aedialsworks.powertools.items.CrafterSpeedUpgrade(4));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.powertools.items.RemoteMonitorItem> REMOTE_STORAGE_MONITOR=ITEMS.register("remote_storage_monitor",com.mpp.aedialsworks.powertools.items.RemoteMonitorItem::new);
    private AWItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
