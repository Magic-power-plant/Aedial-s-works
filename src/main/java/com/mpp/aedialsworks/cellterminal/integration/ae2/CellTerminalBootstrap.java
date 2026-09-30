package com.mpp.aedialsworks.cellterminal.integration.ae2;
import appeng.api.features.GridLinkables;
import appeng.api.parts.PartModels;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.WirelessTerminalItem;
import com.mpp.aedialsworks.common.registry.AWItems;
public final class CellTerminalBootstrap {
    private CellTerminalBootstrap() {}
    public static void setup() {
        TerminalMenuAdapter.registerOpener();
        GridLinkables.register(AWItems.WIRELESS_CELL_TERMINAL.get(),WirelessTerminalItem.LINKABLE_HANDLER);
        Upgrades.add(AEItems.ENERGY_CARD,AWItems.WIRELESS_CELL_TERMINAL.get(),2);
    }
    public static void registerModels() {
        PartModels.registerModels(TerminalDisplayPartAdapter.OFF,TerminalDisplayPartAdapter.ON,TerminalDisplayPartAdapter.DIM);
    }
}
