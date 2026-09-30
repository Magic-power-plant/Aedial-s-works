package com.mpp.aedialsworks.cellterminal.menu;
import com.mpp.aedialsworks.common.config.AWConfigs;
public enum TerminalTab {
    TERMINAL, INVENTORY, PARTITION, TEMP, BUS_INVENTORY, BUS_PARTITION, NETWORK_TOOLS, SUBNETS;
    public boolean enabled() {
        var t=AWConfigs.SERVER.cellterminal.tabs;
        return switch(this) {
            case TERMINAL -> t.terminalTabEnabled.get(); case INVENTORY -> t.inventoryTabEnabled.get();
            case PARTITION -> t.partitionTabEnabled.get(); case TEMP -> t.tempAreaTabEnabled.get();
            case BUS_INVENTORY -> t.storageBusInventoryTabEnabled.get(); case BUS_PARTITION -> t.storageBusPartitionTabEnabled.get();
            case NETWORK_TOOLS -> t.networkToolsTabEnabled.get(); case SUBNETS -> true;
        };
    }
    public boolean bus() { return this==BUS_INVENTORY || this==BUS_PARTITION; }
}
