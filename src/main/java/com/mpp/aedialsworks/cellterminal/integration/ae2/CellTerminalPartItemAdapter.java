package com.mpp.aedialsworks.cellterminal.integration.ae2;
import appeng.items.parts.PartItem;
import com.mpp.aedialsworks.cellterminal.part.PartCellTerminal;
import net.minecraft.world.item.Item;
public abstract class CellTerminalPartItemAdapter extends PartItem<PartCellTerminal> {
    protected CellTerminalPartItemAdapter() { super(new Item.Properties(),PartCellTerminal.class,PartCellTerminal::new); }
}
