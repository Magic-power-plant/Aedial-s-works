package com.mpp.aedialsworks.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Typed settings; read values at use time so Forge reload and server sync stay effective.
 * Legacy keys/defaults come from the matching classes under docs/.
 */
public final class AWServerConfig {
    public final CellTerminal cellterminal;
    public final PowerTools powertools;
    public final Cells cells;

    public AWServerConfig(ForgeConfigSpec.Builder b) {
        cellterminal = new CellTerminal(b);
        powertools = new PowerTools(b);
        cells = new Cells(b);
    }

    public static final class CellTerminal {
        public final Tabs tabs;
        public final Polling polling;
        public final CellOperations cellOperations;
        public final Misc misc;
        public final Network network;

        private CellTerminal(ForgeConfigSpec.Builder b) {
            b.push("cellterminal");
            tabs = new Tabs(b);
            polling = new Polling(b);
            cellOperations = new CellOperations(b);
            misc = new Misc(b);
            network = new Network(b);
            b.pop();
        }
    }

    public static final class Tabs {
        public final ForgeConfigSpec.BooleanValue terminalTabEnabled;
        public final ForgeConfigSpec.BooleanValue inventoryTabEnabled;
        public final ForgeConfigSpec.BooleanValue partitionTabEnabled;
        public final ForgeConfigSpec.BooleanValue tempAreaTabEnabled;
        public final ForgeConfigSpec.BooleanValue storageBusInventoryTabEnabled;
        public final ForgeConfigSpec.BooleanValue storageBusPartitionTabEnabled;
        public final ForgeConfigSpec.BooleanValue networkToolsTabEnabled;

        private Tabs(ForgeConfigSpec.Builder b) {
            b.push("tabs");
            terminalTabEnabled = b.define("terminalTabEnabled", true);
            inventoryTabEnabled = b.define("inventoryTabEnabled", true);
            partitionTabEnabled = b.define("partitionTabEnabled", true);
            tempAreaTabEnabled = b.define("tempAreaTabEnabled", true);
            storageBusInventoryTabEnabled = b.define("storageBusInventoryTabEnabled", true);
            storageBusPartitionTabEnabled = b.define("storageBusPartitionTabEnabled", true);
            networkToolsTabEnabled = b.define("networkToolsTabEnabled", true);
            b.pop();
        }
    }

    public static final class Polling {
        public final ForgeConfigSpec.BooleanValue storageBusPollingEnabled;
        public final ForgeConfigSpec.IntValue pollingInterval;

        private Polling(ForgeConfigSpec.Builder b) {
            b.push("polling");
            storageBusPollingEnabled = b.define("storageBusPollingEnabled", false);
            pollingInterval = b.defineInRange("pollingInterval", 20, 1, 1200);
            b.pop();
        }
    }

    public static final class CellOperations {
        public final ForgeConfigSpec.BooleanValue cellEjectEnabled;
        public final ForgeConfigSpec.BooleanValue cellInsertEnabled;
        public final ForgeConfigSpec.BooleanValue cellSwapEnabled;

        private CellOperations(ForgeConfigSpec.Builder b) {
            b.push("cell_operations");
            cellEjectEnabled = b.define("cellEjectEnabled", true);
            cellInsertEnabled = b.define("cellInsertEnabled", true);
            cellSwapEnabled = b.define("cellSwapEnabled", true);
            b.pop();
        }
    }

    public static final class Misc {
        public final ForgeConfigSpec.BooleanValue partitionEditEnabled;
        public final ForgeConfigSpec.BooleanValue priorityEditEnabled;
        public final ForgeConfigSpec.BooleanValue upgradeInsertEnabled;
        public final ForgeConfigSpec.BooleanValue upgradeExtractEnabled;

        private Misc(ForgeConfigSpec.Builder b) {
            b.push("misc");
            partitionEditEnabled = b.define("partitionEditEnabled", true);
            priorityEditEnabled = b.define("priorityEditEnabled", true);
            upgradeInsertEnabled = b.define("upgradeInsertEnabled", true);
            upgradeExtractEnabled = b.define("upgradeExtractEnabled", true);
            b.pop();
        }
    }

    public static final class Network {
        public final ForgeConfigSpec.IntValue maxChunkBytes;
        public final ForgeConfigSpec.IntValue minRefreshIntervalTicks;
        public final ForgeConfigSpec.BooleanValue enableDeltaUpdates;

        private Network(ForgeConfigSpec.Builder b) {
            b.push("network");
            b.comment("Legacy configured range retained. P1 bulk transport must clamp each frame to its safe wire limit.");
            maxChunkBytes = b.defineInRange("maxChunkBytes", 524288, 4096, 10485760);
            minRefreshIntervalTicks = b.defineInRange("minRefreshIntervalTicks", 10, 1, 200);
            enableDeltaUpdates = b.define("enableDeltaUpdates", true);
            b.pop();
        }
    }

    public static final class PowerTools {
        public final Crafter crafter;
        public final Maintainer maintainer;
        public final InterfaceTerminal interfaceTerminal;

        private PowerTools(ForgeConfigSpec.Builder b) {
            b.push("powertools");
            crafter = new Crafter(b);
            maintainer = new Maintainer(b);
            interfaceTerminal = new InterfaceTerminal(b);
            b.pop();
        }
    }

    public static final class Crafter {
        public final ForgeConfigSpec.IntValue baseCraftsPerOperation;

        private Crafter(ForgeConfigSpec.Builder b) {
            b.push("crafter");
            baseCraftsPerOperation = b.defineInRange("baseCraftsPerOperation", 1, 1, 1000000);
            b.pop();
        }
    }

    public static final class Maintainer {
        public final ForgeConfigSpec.IntValue maxConcurrentCalculations;
        public final ForgeConfigSpec.IntValue maxCpuRetryCount;

        private Maintainer(ForgeConfigSpec.Builder b) {
            b.push("maintainer");
            maxConcurrentCalculations = b.defineInRange("maxConcurrentCalculations", 2, 1, 32);
            maxCpuRetryCount = b.defineInRange("maxCpuRetryCount", 10, 1, 1000);
            b.pop();
        }
    }

    public static final class InterfaceTerminal {
        public final ForgeConfigSpec.BooleanValue enableAutoCrafterPatternRows;

        private InterfaceTerminal(ForgeConfigSpec.Builder b) {
            b.push("interfaceTerminal");
            b.comment("Reserved for the P4 pattern access terminal adapter; checked at runtime, not by a mixin plugin.");
            enableAutoCrafterPatternRows = b.define("enableAutoCrafterPatternRows", true);
            b.pop();
        }
    }

    public static final class Cells {
        public final General general;
        public final IdleDrain idleDrain;
        public final Interfaces interfaces;

        private Cells(ForgeConfigSpec.Builder b) {
            b.push("cells");
            general = new General(b);
            idleDrain = new IdleDrain(b);
            interfaces = new Interfaces(b);
            b.pop();
        }
    }

    public static final class General {
        public final ForgeConfigSpec.IntValue hdItemMaxTypes;
        public final ForgeConfigSpec.IntValue hdFluidMaxTypes;
        public final ForgeConfigSpec.IntValue configurableCellItemMaxTypes;
        public final ForgeConfigSpec.IntValue configurableCellFluidMaxTypes;
        public final ForgeConfigSpec.IntValue compactingCellUpgradeSlots;
        public final ForgeConfigSpec.IntValue hdItemCellUpgradeSlots;
        public final ForgeConfigSpec.IntValue hdCompactingCellUpgradeSlots;
        public final ForgeConfigSpec.IntValue hdFluidCellUpgradeSlots;
        public final ForgeConfigSpec.IntValue configurableCellUpgradeSlots;
        public final ForgeConfigSpec.IntValue nbtSizeWarningThresholdKB;
        public final ForgeConfigSpec.BooleanValue enableNbtSizeTooltip;
        public final ForgeConfigSpec.IntValue subnetProxyUpgradeSlots;
        public final ForgeConfigSpec.IntValue subnetProxyMinTickRate;
        public final ForgeConfigSpec.IntValue subnetProxyMaxTickRate;
        public final ForgeConfigSpec.BooleanValue subnetProxyReportExtractionFaults;
        public final ForgeConfigSpec.BooleanValue subnetProxyReportUpdateChurn;
        public final ForgeConfigSpec.IntValue subnetProxyUpdateChurnLogDelay;

        private General(ForgeConfigSpec.Builder b) {
            b.push("general");
            hdItemMaxTypes = b.defineInRange("hdItemMaxTypes", 63, 1, 16384);
            hdFluidMaxTypes = b.defineInRange("hdFluidMaxTypes", 63, 1, 16384);
            configurableCellItemMaxTypes = b.defineInRange("configurableCellItemMaxTypes", 63, 1, 16384);
            configurableCellFluidMaxTypes = b.defineInRange("configurableCellFluidMaxTypes", 63, 1, 16384);
            compactingCellUpgradeSlots = b.defineInRange("compactingCellUpgradeSlots", 4, 1, 16);
            hdItemCellUpgradeSlots = b.defineInRange("hdItemCellUpgradeSlots", 4, 1, 16);
            hdCompactingCellUpgradeSlots = b.defineInRange("hdCompactingCellUpgradeSlots", 4, 1, 16);
            hdFluidCellUpgradeSlots = b.defineInRange("hdFluidCellUpgradeSlots", 4, 1, 16);
            configurableCellUpgradeSlots = b.defineInRange("configurableCellUpgradeSlots", 4, 1, 16);
            nbtSizeWarningThresholdKB = b.defineInRange("nbtSizeWarningThresholdKB", 100, 1, 10000);
            enableNbtSizeTooltip = b.define("enableNbtSizeTooltip", true);
            subnetProxyUpgradeSlots = b.defineInRange("subnetProxyUpgradeSlots", 5, 1, 24);
            subnetProxyMinTickRate = b.defineInRange("subnetProxyMinTickRate", 5, 1, 200);
            subnetProxyMaxTickRate = b.defineInRange("subnetProxyMaxTickRate", 60, 1, 1200);
            subnetProxyReportExtractionFaults = b.define("subnetProxyReportExtractionFaults", false);
            subnetProxyReportUpdateChurn = b.define("subnetProxyReportUpdateChurn", false);
            subnetProxyUpdateChurnLogDelay = b.defineInRange("subnetProxyUpdateChurnLogDelay", 5, 1, 1440);
            b.pop();
        }
    }

    public static final class IdleDrain {
        public final ForgeConfigSpec.DoubleValue compactingIdleDrain;
        public final ForgeConfigSpec.DoubleValue hdIdleDrain;
        public final ForgeConfigSpec.DoubleValue hdCompactingIdleDrain;
        public final ForgeConfigSpec.DoubleValue fluidHdIdleDrain;
        public final ForgeConfigSpec.DoubleValue configurableCellIdleDrain;

        private IdleDrain(ForgeConfigSpec.Builder b) {
            b.push("idle_drain");
            compactingIdleDrain = b.defineInRange("compactingIdleDrain", 6.0, 0.0, 100.0);
            hdIdleDrain = b.defineInRange("hdIdleDrain", 10.0, 0.0, 100.0);
            hdCompactingIdleDrain = b.defineInRange("hdCompactingIdleDrain", 20.0, 0.0, 100.0);
            fluidHdIdleDrain = b.defineInRange("fluidHdIdleDrain", 10.0, 0.0, 100.0);
            configurableCellIdleDrain = b.defineInRange("configurableCellIdleDrain", 3.0, 0.0, 100.0);
            b.pop();
        }
    }

    public static final class Interfaces {
        public final ForgeConfigSpec.ConfigValue<String> interfaceMaxSlotSizeLimit;
        public final ForgeConfigSpec.BooleanValue interfaceMaxSlotSizeUseFixedValues;
        public final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> interfaceMaxSlotSizeOffsets;
        public final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> interfaceMaxSlotSizeFixedValues;
        public final ForgeConfigSpec.IntValue interfaceMinPollingRate;

        private Interfaces(ForgeConfigSpec.Builder b) {
            b.push("interfaces");
            b.comment("Decimal long retained as text for compatibility; -1 means Long.MAX_VALUE.");
            interfaceMaxSlotSizeLimit = b.define("interfaceMaxSlotSizeLimit", "2147483647", ConfigValidators::positiveLongOrUnlimited);
            interfaceMaxSlotSizeUseFixedValues = b.define("interfaceMaxSlotSizeUseFixedValues", false);
            interfaceMaxSlotSizeOffsets = b.defineList("interfaceMaxSlotSizeOffsets", java.util.List.of("1", "10", "100", "1000"), ConfigValidators::positiveLong);
            interfaceMaxSlotSizeFixedValues = b.defineList("interfaceMaxSlotSizeFixedValues", java.util.List.of("1", "10", "100", "1000", "10000", "100000", "1000000", "10000000"), ConfigValidators::positiveLong);
            interfaceMinPollingRate = b.defineInRange("interfaceMinPollingRate", 0, 0, Integer.MAX_VALUE);
            b.pop();
        }
    }

}
