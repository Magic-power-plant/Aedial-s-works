package com.mpp.aedialsworks.cells.cell;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.api.storage.cells.ISaveProvider;
import com.mpp.aedialsworks.cells.upgrades.CellUpgrades;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

public final class DenseCellState extends AbstractCellState {
    private final LongLedger<AEKey> contents=new LongLedger<>();
    public DenseCellState(ItemStack stack,ISaveProvider provider){
        super(stack,provider);
        for(var raw:stack.getOrCreateTag().getCompound("awCell").getList("contents",Tag.TAG_COMPOUND)){
            var n=(CompoundTag)raw;var key=(n.contains("key")?AEKey.fromTagGeneric(n.getCompound("key")):null);
            if(key==null)throw new IllegalArgumentException("Unknown persisted cell resource");
            contents.restore(key,n.getLong("amount"));
        }
    }
    @Override public long insert(AEKey key,long amount,Actionable mode,IActionSource source){
        MEStorage.checkPreconditions(key,amount,mode,source);if(amount==0||!accepts(key))return 0;
        int types=contents.types()+(contents.get(key)==0?1:0);
        long capacity=CellMath.capacity(item.displayBytes(stack),item.overhead(stack),types,item.multiplier(stack),key.getAmountPerByte());
        long perType=Long.MAX_VALUE;
        if(CellUpgrades.value(upgrades,"equal_distribution_card_",0)>0)perType=CellMath.capacity(item.displayBytes(stack),item.overhead(stack),maximumTypes(),item.multiplier(stack),key.getAmountPerByte())/maximumTypes();
        long accepted=contents.insert(key,amount,capacity,maximumTypes(),perType,mode==Actionable.SIMULATE);
        if(accepted>0&&mode==Actionable.MODULATE)changed();
        return overflow()?amount:accepted;
    }
    @Override public long extract(AEKey key,long amount,Actionable mode,IActionSource source){
        MEStorage.checkPreconditions(key,amount,mode,source);long extracted=contents.extract(key,amount,mode==Actionable.SIMULATE);if(extracted>0&&mode==Actionable.MODULATE)changed();return extracted;
    }
    @Override public void getAvailableStacks(KeyCounter out){contents.entries().forEach((k,v)->out.set(k,CellMath.add(out.get(k),v)));}
    @Override public long usedBytes(){return CellMath.add(CellMath.multiply(item.overhead(stack),item.multiplier(stack),contents.types()),CellMath.ceilDivide(contents.total(),item.keyType(stack).getAmountPerByte()));}
    @Override public long storedUnits(){return contents.total();}
    @Override public int storedTypes(){return contents.types();}
    @Override protected void saveContents(CompoundTag n){var list=new ListTag();contents.entries().forEach((key,amount)->{var row=new CompoundTag();row.put("key",key.toTagGeneric());row.putLong("amount",amount);list.add(row);});n.put("contents",list);}
}
