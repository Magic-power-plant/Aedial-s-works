package com.mpp.aedialsworks.cellterminal.integration.ae2;
import appeng.api.inventories.BaseInternalInventory;
import appeng.api.storage.StorageCells;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import java.util.Arrays;
/** The 16-slot domain inventory owns persistence and notifies its host on every mutation. */
public final class TempCellInventory extends BaseInternalInventory {
    private final ItemStack[] cells = new ItemStack[16];
    private final Runnable changed;
    public TempCellInventory(Runnable changed) { this.changed=changed; Arrays.fill(cells, ItemStack.EMPTY); }
    @Override public int size() { return cells.length; }
    @Override public int getSlotLimit(int slot) { return 1; }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return StorageCells.isCellHandled(stack); }
    @Override public ItemStack getStackInSlot(int slot) { return cells[slot]; }
    @Override public void setItemDirect(int slot, ItemStack stack) { cells[slot]=stack; changed.run(); }
    public void read(CompoundTag tag) {
        Arrays.fill(cells, ItemStack.EMPTY);
        for (var entry : tag.getList("tempCells", Tag.TAG_COMPOUND)) {
            var item=(CompoundTag)entry; int slot=item.getInt("slot");
            if (slot >= 0 && slot < size()) cells[slot]=ItemStack.of(item.getCompound("stack"));
        }
    }
    public void write(CompoundTag tag) {
        var list=new ListTag();
        for(int i=0;i<size();i++) if(!cells[i].isEmpty()) {
            var item=new CompoundTag(); item.putInt("slot",i); item.put("stack",cells[i].save(new CompoundTag())); list.add(item);
        }
        tag.put("tempCells",list);
    }
}
