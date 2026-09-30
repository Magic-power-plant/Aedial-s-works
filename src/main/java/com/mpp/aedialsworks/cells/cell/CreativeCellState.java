package com.mpp.aedialsworks.cells.cell;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.api.storage.cells.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
public final class CreativeCellState extends AbstractCellState {
    public CreativeCellState(ItemStack stack,ISaveProvider provider){super(stack,provider);}
    private boolean configured(AEKey key){return item.getConfigInventory(stack).keySet().contains(key);}
    @Override public long insert(AEKey key,long amount,Actionable mode,IActionSource source){MEStorage.checkPreconditions(key,amount,mode,source);return configured(key)?amount:0;}
    @Override public long extract(AEKey key,long amount,Actionable mode,IActionSource source){MEStorage.checkPreconditions(key,amount,mode,source);return configured(key)?amount:0;}
    @Override public void getAvailableStacks(KeyCounter out){for(var key:item.getConfigInventory(stack).keySet())out.set(key,Long.MAX_VALUE);}
    @Override public long totalBytes(){return Long.MAX_VALUE;}
    @Override public long usedBytes(){return 0;}
    @Override public long storedUnits(){return storedTypes()==0?0:Long.MAX_VALUE;}
    @Override public int storedTypes(){return item.getConfigInventory(stack).keySet().size();}
    @Override public boolean canFitInsideCell(){return false;}
    @Override public CellState getStatus(){return storedTypes()==0?CellState.EMPTY:CellState.TYPES_FULL;}
    @Override protected void saveContents(CompoundTag n){}
}
