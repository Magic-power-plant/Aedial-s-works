package com.mpp.aedialsworks.cells.cell;
import appeng.api.storage.cells.*;
import com.mpp.aedialsworks.cells.cell.compacting.CompactingCellState;
import net.minecraft.world.item.ItemStack;
public final class CellHandler implements ICellHandler {
    @Override public boolean isCell(ItemStack stack){return stack.getItem() instanceof TieredCellItem;}
    @Override public StorageCell getCellInventory(ItemStack stack,ISaveProvider provider){return isCell(stack)?open(stack,provider):null;}
    public static AbstractCellState open(ItemStack stack,ISaveProvider provider){
        if(!(stack.getItem() instanceof TieredCellItem item)||stack.getCount()!=1)return null;
        if(item.family.creative())return new CreativeCellState(stack,provider);
        return item.storageFamily(stack).compacting?new CompactingCellState(stack,provider):new DenseCellState(stack,provider);
    }
}
