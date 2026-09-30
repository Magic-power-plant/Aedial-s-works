package com.mpp.aedialsworks.common.registry;

import com.mpp.aedialsworks.Aedialsworks;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Central registration point; feature domains declare their entries here. */
public final class AWMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Aedialsworks.MODID);

    public static final net.minecraftforge.registries.RegistryObject<MenuType<com.mpp.aedialsworks.cellterminal.menu.CellTerminalMenu>> CELL_TERMINAL =
            MENU_TYPES.register("cell_terminal", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(
                    com.mpp.aedialsworks.cellterminal.integration.ae2.TerminalMenuAdapter::fromNetwork));

    public static final net.minecraftforge.registries.RegistryObject<MenuType<com.mpp.aedialsworks.powertools.menu.PowerToolsMenu>> POWER_TOOLS=MENU_TYPES.register("power_tools",()->net.minecraftforge.common.extensions.IForgeMenuType.create(com.mpp.aedialsworks.powertools.menu.PowerToolsMenu::fromNetwork));
    public static final net.minecraftforge.registries.RegistryObject<MenuType<com.mpp.aedialsworks.cells.menu.CellsMenu>> CELLS=MENU_TYPES.register("cells",()->net.minecraftforge.common.extensions.IForgeMenuType.create(com.mpp.aedialsworks.cells.menu.CellsMenu::fromNetwork));
    public static final net.minecraftforge.registries.RegistryObject<MenuType<com.mpp.aedialsworks.cells.menu.CellConfigurationMenu>> CELL_CONFIGURATION=MENU_TYPES.register("cell_configuration",()->net.minecraftforge.common.extensions.IForgeMenuType.create(com.mpp.aedialsworks.cells.menu.CellConfigurationMenu::fromNetwork));
    private AWMenus() {}

    public static void register(IEventBus bus) {
        MENU_TYPES.register(bus);
    }
}
