package com.mpp.aedialsworks.cells.integration.ae2;
import appeng.items.parts.PartItem;
import com.mpp.aedialsworks.cells.MachineKind;
import net.minecraft.world.item.Item;
public final class CellsPartItem extends PartItem<CellsPart> {
    public final MachineKind kind;
    public CellsPartItem(MachineKind kind){super(new Item.Properties(),CellsPart.class,CellsPart::new);this.kind=kind;}
}
