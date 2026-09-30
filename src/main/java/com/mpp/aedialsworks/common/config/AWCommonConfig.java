package com.mpp.aedialsworks.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Typed settings; read values at use time so Forge reload and server sync stay effective.
 * Legacy keys/defaults come from the matching classes under docs/.
 */
public final class AWCommonConfig {
    public final Cells cells;

    public AWCommonConfig(ForgeConfigSpec.Builder b) {
        cells = new Cells(b);
    }

    public static final class Cells {
        public final EnabledCells enabledCells;

        private Cells(ForgeConfigSpec.Builder b) {
            b.push("cells");
            enabledCells = new EnabledCells(b);
            b.pop();
        }
    }

    public static final class EnabledCells {
        public final ForgeConfigSpec.BooleanValue enableCompactingCells;
        public final ForgeConfigSpec.BooleanValue enableHDCells;
        public final ForgeConfigSpec.BooleanValue enableHDCompactingCells;
        public final ForgeConfigSpec.BooleanValue enableFluidHDCells;
        public final ForgeConfigSpec.BooleanValue enableConfigurableCells;

        private EnabledCells(ForgeConfigSpec.Builder b) {
            b.push("enabled_cells");
            b.comment("Availability policy; never conditionally remove registry entries on either side.");
            enableCompactingCells = b.define("enableCompactingCells", true);
            b.comment("Availability policy; never conditionally remove registry entries on either side.");
            enableHDCells = b.define("enableHDCells", true);
            b.comment("Availability policy; never conditionally remove registry entries on either side.");
            enableHDCompactingCells = b.define("enableHDCompactingCells", true);
            b.comment("Availability policy; never conditionally remove registry entries on either side.");
            enableFluidHDCells = b.define("enableFluidHDCells", true);
            b.comment("Availability policy; never conditionally remove registry entries on either side.");
            enableConfigurableCells = b.define("enableConfigurableCells", true);
            b.pop();
        }
    }

}
