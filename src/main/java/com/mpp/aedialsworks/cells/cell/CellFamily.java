package com.mpp.aedialsworks.cells.cell;

import appeng.api.stacks.AEKeyType;
import com.mpp.aedialsworks.common.config.AWConfigs;

public enum CellFamily {
    COMPACTING("compacting",false,true,false), HD_ITEM("hyper_density",false,false,true),
    HD_COMPACTING("hyper_density_compacting",false,true,true), HD_FLUID("hyper_density_fluid",true,false,true),
    CONFIGURABLE("configurable",false,false,false), CREATIVE_ITEM("creative_item",false,false,false),
    CREATIVE_FLUID("creative_fluid",true,false,false);
    public final String id;
    public final boolean fluid, compacting, highDensity;
    CellFamily(String id,boolean fluid,boolean compacting,boolean highDensity){this.id=id;this.fluid=fluid;this.compacting=compacting;this.highDensity=highDensity;}
    public boolean creative(){return this==CREATIVE_ITEM||this==CREATIVE_FLUID;}
    public AEKeyType keyType(){return fluid?AEKeyType.fluids():AEKeyType.items();}
    public boolean enabled(){var c=AWConfigs.COMMON.cells.enabledCells;return switch(this){case COMPACTING->c.enableCompactingCells.get();case HD_ITEM->c.enableHDCells.get();case HD_COMPACTING->c.enableHDCompactingCells.get();case HD_FLUID->c.enableFluidHDCells.get();case CONFIGURABLE->c.enableConfigurableCells.get();default->true;};}
    public int maxTypes(){var g=AWConfigs.SERVER.cells.general;return compacting?1:switch(this){case HD_ITEM->g.hdItemMaxTypes.get();case HD_FLUID->g.hdFluidMaxTypes.get();case CONFIGURABLE->g.configurableCellItemMaxTypes.get();default->63;};}
    public int upgradeSlots(){var g=AWConfigs.SERVER.cells.general;return switch(this){case COMPACTING->g.compactingCellUpgradeSlots.get();case HD_ITEM->g.hdItemCellUpgradeSlots.get();case HD_COMPACTING->g.hdCompactingCellUpgradeSlots.get();case HD_FLUID->g.hdFluidCellUpgradeSlots.get();case CONFIGURABLE->g.configurableCellUpgradeSlots.get();default->0;};}
    public double idleDrain(){var g=AWConfigs.SERVER.cells.idleDrain;return switch(this){case COMPACTING->g.compactingIdleDrain.get();case HD_ITEM->g.hdIdleDrain.get();case HD_COMPACTING->g.hdCompactingIdleDrain.get();case HD_FLUID->g.fluidHdIdleDrain.get();case CONFIGURABLE->g.configurableCellIdleDrain.get();default->0;};}
}
