package com.mpp.aedialsworks.cells.interfaceblock;

import appeng.api.stacks.*;
import com.mpp.aedialsworks.cells.cell.ResourceFilters;
import com.mpp.aedialsworks.common.config.AWConfigs;
import net.minecraft.nbt.*;

/** 36 slots/page, up to five pages; inactive page contents remain drainable after card removal. */
public final class ResourcePort {
    public static final int PAGE_SIZE=36,MAX_SLOTS=180;
    public final boolean output,fluid;
    public final AEKey[] filters=new AEKey[MAX_SLOTS],keys=new AEKey[MAX_SLOTS];
    public final long[] amounts=new long[MAX_SLOTS],limits=new long[MAX_SLOTS];
    public final java.util.BitSet occupied=new java.util.BitSet(MAX_SLOTS);
    public long maxSlot;
    private final InterfaceLogic owner;
    public ResourcePort(InterfaceLogic owner,boolean output,boolean fluid){this.owner=owner;this.output=output;this.fluid=fluid;maxSlot=fluid?16000:64;}
    public int activeSlots(){return PAGE_SIZE*owner.pages();}
    public boolean channel(AEKey key){return fluid?key instanceof AEFluidKey:key instanceof AEItemKey;}
    private static volatile String limitRaw;private static volatile long limitParsed;
    public long limit(int slot){
        String raw=AWConfigs.SERVER.cells.interfaces.interfaceMaxSlotSizeLimit.get();long parsed;
        if(raw.equals(limitRaw))parsed=limitParsed;else{parsed=Long.parseLong(raw);limitRaw=raw;limitParsed=parsed;}
        long max=parsed==-1?Long.MAX_VALUE:parsed;return Math.min(max,limits[slot]>0?limits[slot]:maxSlot);
    }
    public boolean accepts(int slot,AEKey key){
        if(slot<0||slot>=activeSlots()||!channel(key)||(fluid?!owner.fluids:!owner.items))return false;
        boolean any=false;for(int i=0;i<activeSlots();i++)if(filters[i]!=null){any=true;break;}
        boolean fuzzy=owner.upgrades.isInstalled(appeng.core.definitions.AEItems.FUZZY_CARD);
        if(!output&&(owner.inverse||owner.upgrades.isInstalled(appeng.core.definitions.AEItems.INVERTER_CARD)))return ResourceFilters.matches(key,java.util.Arrays.stream(filters).limit(activeSlots()).filter(java.util.Objects::nonNull).toList(),true,fuzzy,owner.fuzzyMode,owner.installed("tag_card")?owner.tag:"");
        if(!output&&owner.installed("tag_card")&&!owner.tag.isBlank()&&ResourceFilters.matches(key,java.util.List.of(),false,false,owner.fuzzyMode,owner.tag))return true;
        if(!any)return !output&&(!owner.installed("tag_card")||owner.tag.isBlank());
        return filters[slot]!=null&&ResourceFilters.matches(key,java.util.List.of(filters[slot]),false,owner.upgrades.isInstalled(appeng.core.definitions.AEItems.FUZZY_CARD),owner.fuzzyMode,"");
    }
    public long insert(int slot,AEKey key,long requested,boolean simulate,boolean fromNetwork){
        if(requested<=0||output!=fromNetwork||!accepts(slot,key)||keys[slot]!=null&&!keys[slot].equals(key))return 0;
        long accepted=Math.min(requested,Math.max(0,limit(slot)-amounts[slot]));
        if(!simulate&&accepted>0){keys[slot]=key;amounts[slot]+=accepted;occupied.set(slot);owner.storageChanged();}
        return accepted;
    }
    public long insert(AEKey key,long requested,boolean simulate,boolean fromNetwork){
        long rest=requested;for(int i=0;i<activeSlots()&&rest>0;i++)rest-=insert(i,key,rest,simulate,fromNetwork);return requested-rest;
    }
    public long extract(int slot,long requested,boolean simulate){
        if(slot<0||slot>=MAX_SLOTS||requested<=0)return 0;long got=Math.min(requested,amounts[slot]);
        if(!simulate&&got>0){amounts[slot]-=got;if(amounts[slot]==0){keys[slot]=null;occupied.clear(slot);}owner.storageChanged();}return got;
    }
    public CompoundTag saveSettings(){var n=new CompoundTag();n.putLong("maxSlot",maxSlot);var list=new ListTag();for(int i=0;i<MAX_SLOTS;i++)if(filters[i]!=null||limits[i]>0){var row=new CompoundTag();row.putInt("slot",i);if(filters[i]!=null)row.put("filter",filters[i].toTagGeneric());row.putLong("limit",limits[i]);list.add(row);}n.put("slots",list);return n;}
    public void loadSettings(CompoundTag n){if(!n.getBoolean("preserveFilters"))java.util.Arrays.fill(filters,null);java.util.Arrays.fill(limits,0);maxSlot=n.contains("maxSlot")?Math.max(1,n.getLong("maxSlot")):fluid?16000:64;for(var raw:n.getList("slots",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;int i=row.getInt("slot");if(i<0||i>=MAX_SLOTS)continue;var key=(row.contains("filter")?AEKey.fromTagGeneric(row.getCompound("filter")):null);if(!n.getBoolean("preserveFilters"))filters[i]=key!=null&&channel(key)?key:null;limits[i]=Math.max(0,row.getLong("limit"));}}
    public CompoundTag save(){var n=new CompoundTag();var list=new ListTag();for(int i=0;i<MAX_SLOTS;i++)if(keys[i]!=null&&amounts[i]>0){var row=new CompoundTag();row.putInt("slot",i);row.put("key",keys[i].toTagGeneric());row.putLong("amount",amounts[i]);list.add(row);}n.put("storage",list);return n;}
    public void load(CompoundTag n){java.util.Arrays.fill(keys,null);java.util.Arrays.fill(amounts,0);occupied.clear();for(var raw:n.getList("storage",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;int i=row.getInt("slot");if(i<0||i>=MAX_SLOTS)continue;var key=(row.contains("key")?AEKey.fromTagGeneric(row.getCompound("key")):null);long amount=row.getLong("amount");if(key!=null&&channel(key)&&amount>0){keys[i]=key;amounts[i]=amount;occupied.set(i);}}}
}
