package com.mpp.aedialsworks.cells.cell.compacting;

import java.util.*;
import appeng.api.stacks.AEItemKey;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** Immutable validated exchange rates; persisted to avoid deleting items when datapacks change. */
public record CompressionChain(List<AEItemKey> keys,List<Long> rates,int main) {
    public CompressionChain {
        keys=List.copyOf(keys);rates=List.copyOf(rates);
        if(keys.isEmpty()||keys.size()!=rates.size()||main<0||main>=keys.size()||new HashSet<>(keys).size()!=keys.size())throw new IllegalArgumentException("Invalid chain");
        for(long rate:rates)if(rate<=0)throw new IllegalArgumentException("Invalid rate");
    }
    public long rate(AEItemKey key){int i=keys.indexOf(key);return i<0?0:rates.get(i);}
    public long mainRate(){return rates.get(main);}
    public CompoundTag save(){var n=new CompoundTag();var rows=new ListTag();for(int i=0;i<keys.size();i++){var row=new CompoundTag();row.put("key",keys.get(i).toTag());row.putLong("rate",rates.get(i));rows.add(row);}n.put("rows",rows);n.putInt("main",main);return n;}
    public static CompressionChain load(CompoundTag n){var keys=new ArrayList<AEItemKey>();var rates=new ArrayList<Long>();for(var raw:n.getList("rows",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;var k=AEItemKey.fromTag(row.getCompound("key"));long rate=row.getLong("rate");if(k==null||rate<=0||keys.contains(k))continue;keys.add(k);rates.add(rate);}if(keys.isEmpty())return null;int main=n.getInt("main");if(main<0||main>=keys.size())main=0;return new CompressionChain(keys,rates,main);}
}
