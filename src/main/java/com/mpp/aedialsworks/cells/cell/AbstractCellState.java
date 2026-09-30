package com.mpp.aedialsworks.cells.cell;

import appeng.api.config.*;
import appeng.api.stacks.*;
import appeng.api.storage.*;
import appeng.api.storage.cells.*;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.definitions.AEItems;
import com.mpp.aedialsworks.cells.api.CellCapacity;
import com.mpp.aedialsworks.cells.upgrades.CellUpgrades;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Owns the load/mutate/persist lifecycle; capability interfaces expose only storage and accounting. */
public abstract class AbstractCellState implements StorageCell,CellCapacity {
    protected final ItemStack stack;
    protected final TieredCellItem item;
    protected final ISaveProvider saveProvider;
    protected final IUpgradeInventory upgrades;
    protected AbstractCellState(ItemStack stack,ISaveProvider saveProvider){
        this.stack=stack;this.item=(TieredCellItem)stack.getItem();this.saveProvider=saveProvider;this.upgrades=item.getUpgrades(stack);
    }
    protected abstract void saveContents(CompoundTag tag);
    protected final void changed(){persist();if(saveProvider!=null)saveProvider.saveChanges();}
    @Override public final void persist(){var n=new CompoundTag();n.putInt("version",1);saveContents(n);stack.getOrCreateTag().put("awCell",n);}
    protected boolean accepts(AEKey key){
        if(!item.family.enabled()||!item.keyType(stack).contains(key))return false;
        if(key instanceof AEItemKey i&&StorageCells.isCellHandled(i.toStack()))return false;
        var selected=item.getConfigInventory(stack).keySet();
        String tag=CellUpgrades.has(upgrades,"tag_card")?stack.getOrCreateTag().getString("filterTag"):"";
        return ResourceFilters.matches(key,selected,upgrades.isInstalled(AEItems.INVERTER_CARD),upgrades.isInstalled(AEItems.FUZZY_CARD),item.getFuzzyMode(stack),tag);
    }
    protected boolean overflow(){return CellUpgrades.has(upgrades,"overflow_card");}
    @Override public long totalBytes(){return CellMath.multiply(item.displayBytes(stack),item.multiplier(stack));}
    @Override public int maximumTypes(){return item.maximumTypes(stack);}
    @Override public double getIdleDrain(){return item.getIdleDrain();}
    @Override public boolean canFitInsideCell(){return storedUnits()==0&&!item.family.creative();}
    @Override public Component getDescription(){return stack.getHoverName();}
    @Override public CellState getStatus(){
        if(storedUnits()==0)return CellState.EMPTY;
        if(usedBytes()>=totalBytes())return CellState.FULL;
        return storedTypes()>=maximumTypes()?CellState.TYPES_FULL:CellState.NOT_EMPTY;
    }
}
