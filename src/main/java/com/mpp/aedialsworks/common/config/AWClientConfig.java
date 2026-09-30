package com.mpp.aedialsworks.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Typed settings; read values at use time so Forge reload and server sync stay effective.
 * Legacy keys/defaults come from the matching classes under docs/.
 */
public final class AWClientConfig {
    public final CellTerminal cellterminal;
    public final PowerTools powertools;
    public final Cells cells;

    public AWClientConfig(ForgeConfigSpec.Builder b) {
        cellterminal = new CellTerminal(b);
        powertools = new PowerTools(b);
        cells = new Cells(b);
    }

    public static final class CellTerminal {
        public final Settings settings;
        public final Gui gui;
        public final Filters filters;

        private CellTerminal(ForgeConfigSpec.Builder b) {
            b.push("cellterminal");
            settings = new Settings(b);
            gui = new Gui(b);
            filters = new Filters(b);
            b.pop();
        }
    }

    public static final class Settings {
        public final ForgeConfigSpec.IntValue maxHighlightDistance;
        public final ForgeConfigSpec.IntValue highlightDuration;
        public final ForgeConfigSpec.IntValue arrowScalePercent;
        public final ForgeConfigSpec.IntValue textScalePercent;
        public final ForgeConfigSpec.BooleanValue adaptiveTextScale;
        public final ForgeConfigSpec.IntValue adaptiveTextScaleMinPercent;
        public final ForgeConfigSpec.IntValue adaptiveTextScaleMaxPercent;

        private Settings(ForgeConfigSpec.Builder b) {
            b.push("settings");
            maxHighlightDistance = b.defineInRange("maxHighlightDistance", -1, -1, 10000);
            highlightDuration = b.defineInRange("highlightDuration", 15, 1, 3600);
            arrowScalePercent = b.defineInRange("arrowScalePercent", 100, 10, 1000);
            textScalePercent = b.defineInRange("textScalePercent", 100, 10, 1000);
            adaptiveTextScale = b.define("adaptiveTextScale", true);
            adaptiveTextScaleMinPercent = b.defineInRange("adaptiveTextScaleMinPercent", 100, 10, 1000);
            adaptiveTextScaleMaxPercent = b.defineInRange("adaptiveTextScaleMaxPercent", 200, 10, 1000);
            b.pop();
        }
    }

    public static final class Gui {
        public final ForgeConfigSpec.IntValue selectedTab;
        public final ForgeConfigSpec.ConfigValue<String> terminalStyle;
        public final ForgeConfigSpec.ConfigValue<String> searchFilter;
        public final ForgeConfigSpec.ConfigValue<String> searchMode;
        public final ForgeConfigSpec.ConfigValue<String> cellSlotLimit;
        public final ForgeConfigSpec.ConfigValue<String> busSlotLimit;
        public final ForgeConfigSpec.ConfigValue<String> subnetSlotLimit;
        public final ForgeConfigSpec.ConfigValue<String> lastViewedNetworkId;
        public final ForgeConfigSpec.ConfigValue<String> subnetVisibility;
        public final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> subnetFavorites;

        private Gui(ForgeConfigSpec.Builder b) {
            b.push("gui");
            selectedTab = b.defineInRange("selectedTab", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            terminalStyle = b.defineInList("terminalStyle", "SMALL", java.util.List.of("SMALL", "TALL"));
            searchFilter = b.define("searchFilter", "");
            searchMode = b.defineInList("searchMode", "MIXED", java.util.List.of("INVENTORY", "PARTITION", "MIXED"));
            cellSlotLimit = b.defineInList("cellSlotLimit", "UNLIMITED", java.util.List.of("LIMIT_8", "LIMIT_32", "LIMIT_64", "UNLIMITED"));
            busSlotLimit = b.defineInList("busSlotLimit", "UNLIMITED", java.util.List.of("LIMIT_8", "LIMIT_32", "LIMIT_64", "UNLIMITED"));
            subnetSlotLimit = b.defineInList("subnetSlotLimit", "LIMIT_64", java.util.List.of("LIMIT_8", "LIMIT_32", "LIMIT_64", "UNLIMITED"));
            lastViewedNetworkId = b.define("lastViewedNetworkId", "0", ConfigValidators::signedLong);
            subnetVisibility = b.defineInList("subnetVisibility", "DONT_SHOW", java.util.List.of("DONT_SHOW", "SHOW_FAVORITES", "SHOW_ALL"));
            subnetFavorites = b.defineListAllowEmpty(java.util.List.of("subnetFavorites"),java.util.List.of(),value->value instanceof String id && ConfigValidators.signedLong(id));
            b.pop();
        }
    }

    public static final class PowerTools {
        public final Maintainer maintainer;
        public final Scanner scanner;
        public final RemoteMonitor remoteMonitor;
        public final Monitor monitor;
        public final Locator locator;

        private PowerTools(ForgeConfigSpec.Builder b) {
            b.push("powertools");
            maintainer = new Maintainer(b);
            scanner = new Scanner(b);
            remoteMonitor = new RemoteMonitor(b);
            monitor = new Monitor(b);
            locator = new Locator(b);
            b.pop();
        }
    }

    public static final class Maintainer {
        public final ForgeConfigSpec.BooleanValue useTallView;

        private Maintainer(ForgeConfigSpec.Builder b) {
            b.push("maintainer");
            useTallView = b.define("useTallView", false);
            b.pop();
        }
    }

    public static final class Scanner {
        public final ForgeConfigSpec.IntValue arrowScalePercent;
        public final ForgeConfigSpec.IntValue textScalePercent;
        public final ForgeConfigSpec.BooleanValue adaptiveTextScale;
        public final ForgeConfigSpec.IntValue adaptiveTextScaleMinPercent;
        public final ForgeConfigSpec.IntValue adaptiveTextScaleMaxPercent;
        public final ForgeConfigSpec.IntValue sortModeLoops;
        public final ForgeConfigSpec.IntValue sortModeChunks;
        public final ForgeConfigSpec.IntValue sortModeChokepoints;
        public final ForgeConfigSpec.IntValue sortModeMissing;
        public final ForgeConfigSpec.IntValue sortModeFatal;
        public final ForgeConfigSpec.IntValue sortModePatterns;

        private Scanner(ForgeConfigSpec.Builder b) {
            b.push("scanner");
            arrowScalePercent = b.defineInRange("arrowScalePercent", 100, 10, 1000);
            textScalePercent = b.defineInRange("textScalePercent", 100, 10, 1000);
            adaptiveTextScale = b.define("adaptiveTextScale", true);
            adaptiveTextScaleMinPercent = b.defineInRange("adaptiveTextScaleMinPercent", 100, 10, 1000);
            adaptiveTextScaleMaxPercent = b.defineInRange("adaptiveTextScaleMaxPercent", 200, 10, 1000);
            sortModeLoops = b.defineInRange("sortModeLoops", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            sortModeChunks = b.defineInRange("sortModeChunks", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            sortModeChokepoints = b.defineInRange("sortModeChokepoints", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            sortModeMissing = b.defineInRange("sortModeMissing", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            sortModeFatal = b.defineInRange("sortModeFatal", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            sortModePatterns = b.defineInRange("sortModePatterns", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            b.pop();
        }
    }

    public static final class RemoteMonitor {
        public final ForgeConfigSpec.IntValue x;
        public final ForgeConfigSpec.IntValue y;
        public final ForgeConfigSpec.IntValue paddingInternal;
        public final ForgeConfigSpec.IntValue lineSpacing;
        public final ForgeConfigSpec.IntValue iconSize;
        public final ForgeConfigSpec.IntValue iconTextGap;
        public final ForgeConfigSpec.IntValue textScalePercent;
        public final ForgeConfigSpec.BooleanValue showTotalQuantity;
        public final ForgeConfigSpec.BooleanValue shortenNumbers;
        public final ForgeConfigSpec.ConfigValue<String> gainColor;
        public final ForgeConfigSpec.ConfigValue<String> lossColor;

        private RemoteMonitor(ForgeConfigSpec.Builder b) {
            b.push("remoteMonitor");
            x = b.defineInRange("x", 5, 0, 4096);
            y = b.defineInRange("y", 5, 0, 4096);
            paddingInternal = b.defineInRange("paddingInternal", 4, 0, 64);
            lineSpacing = b.defineInRange("lineSpacing", 2, 0, 32);
            iconSize = b.defineInRange("iconSize", 16, 8, 64);
            iconTextGap = b.defineInRange("iconTextGap", 4, 0, 32);
            textScalePercent = b.defineInRange("textScalePercent", 100, 10, 1000);
            showTotalQuantity = b.define("showTotalQuantity", true);
            shortenNumbers = b.define("shortenNumbers", true);
            gainColor = b.define("gainColor", "66FF66");
            lossColor = b.define("lossColor", "FF6666");
            b.pop();
        }
    }

    public static final class Monitor {
        public final ForgeConfigSpec.IntValue displayRenderDistance;
        public final ForgeConfigSpec.ConfigValue<String> displayColorAbove;
        public final ForgeConfigSpec.ConfigValue<String> displayColorBelow;

        private Monitor(ForgeConfigSpec.Builder b) {
            b.push("monitor");
            displayRenderDistance = b.defineInRange("displayRenderDistance", 16, 4, 128);
            displayColorAbove = b.define("displayColorAbove", "FF00CC00");
            displayColorBelow = b.define("displayColorBelow", "FFCCCC00");
            b.pop();
        }
    }

    public static final class Locator {
        public final ForgeConfigSpec.BooleanValue useTallView;

        private Locator(ForgeConfigSpec.Builder b) {
            b.push("locator");
            useTallView = b.define("useTallView", false);
            b.pop();
        }
    }

    public static final class Cells {
        public final Hidden hidden;
        public final Interfaces interfaces;

        private Cells(ForgeConfigSpec.Builder b) {
            b.push("cells");
            hidden = new Hidden(b);
            interfaces = new Interfaces(b);
            b.pop();
        }
    }

    public static final class Hidden {
        public final ForgeConfigSpec.BooleanValue showControlsHelp;
        public final ForgeConfigSpec.BooleanValue jeiTransferInputsToExport;
        public final ForgeConfigSpec.BooleanValue jeiTransferOutputsToCreativeCell;

        private Hidden(ForgeConfigSpec.Builder b) {
            b.push("hidden");
            showControlsHelp = b.define("showControlsHelp", false);
            jeiTransferInputsToExport = b.define("jeiTransferInputsToExport", true);
            jeiTransferOutputsToCreativeCell = b.define("jeiTransferOutputsToCreativeCell", true);
            b.pop();
        }
    }

    public static final class Interfaces {
        public final ForgeConfigSpec.BooleanValue useFixedInterfaceTextures;

        private Interfaces(ForgeConfigSpec.Builder b) {
            b.push("interfaces");
            b.comment("Client rendering preference, requires restart.");
            useFixedInterfaceTextures = b.worldRestart().define("useFixedInterfaceTextures", true);
            b.pop();
        }
    }


    public static final class Filters {
        public final ForgeConfigSpec.ConfigValue<String> cellItemCells;
        public final ForgeConfigSpec.ConfigValue<String> cellFluidCells;
        public final ForgeConfigSpec.ConfigValue<String> cellHasItems;
        public final ForgeConfigSpec.ConfigValue<String> cellPartitioned;
        public final ForgeConfigSpec.ConfigValue<String> busItemCells;
        public final ForgeConfigSpec.ConfigValue<String> busFluidCells;
        public final ForgeConfigSpec.ConfigValue<String> busHasItems;
        public final ForgeConfigSpec.ConfigValue<String> busPartitioned;

        private Filters(ForgeConfigSpec.Builder b) {
            b.push("filters");
            cellItemCells = b.defineInList("cell_item_cells", "SHOW_ALL", java.util.List.of("SHOW_ALL", "SHOW_ONLY", "HIDE"));
            cellFluidCells = b.defineInList("cell_fluid_cells", "SHOW_ALL", java.util.List.of("SHOW_ALL", "SHOW_ONLY", "HIDE"));
            cellHasItems = b.defineInList("cell_has_items", "SHOW_ALL", java.util.List.of("SHOW_ALL", "SHOW_ONLY", "HIDE"));
            cellPartitioned = b.defineInList("cell_partitioned", "SHOW_ALL", java.util.List.of("SHOW_ALL", "SHOW_ONLY", "HIDE"));
            busItemCells = b.defineInList("bus_item_cells", "SHOW_ALL", java.util.List.of("SHOW_ALL", "SHOW_ONLY", "HIDE"));
            busFluidCells = b.defineInList("bus_fluid_cells", "SHOW_ALL", java.util.List.of("SHOW_ALL", "SHOW_ONLY", "HIDE"));
            busHasItems = b.defineInList("bus_has_items", "SHOW_ALL", java.util.List.of("SHOW_ALL", "SHOW_ONLY", "HIDE"));
            busPartitioned = b.defineInList("bus_partitioned", "SHOW_ALL", java.util.List.of("SHOW_ALL", "SHOW_ONLY", "HIDE"));
            b.pop();
        }
    }
}
