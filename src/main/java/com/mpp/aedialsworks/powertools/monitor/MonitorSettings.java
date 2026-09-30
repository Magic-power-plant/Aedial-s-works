package com.mpp.aedialsworks.powertools.monitor;
import java.util.function.ToLongFunction;
import appeng.api.stacks.AEKey;
import net.minecraft.nbt.*;
/** Shared 24-entry AND/OR and hysteresis semantics for blocks, parts and handheld monitors. */
public final class MonitorSettings {
    public static final int CAPACITY = 24;
    public final ResourceEntry[] entries = new ResourceEntry[CAPACITY];
    public boolean any, hysteresis, condition;
    public int strength = 15, refreshTicks = 20;
    public MonitorSettings() { for (int i=0;i<CAPACITY;i++) entries[i]=new ResourceEntry(); }
    public boolean evaluate(ToLongFunction<AEKey> count, boolean online) {
        boolean seen=false, result=!any;
        for (var e:entries) {
            e.evaluate(online && e.key!=null? count.applyAsLong(e.key):0,hysteresis);
            if (!online || !e.enabled || e.key==null) continue;
            seen=true; result=any?result||e.met:result&&e.met;
        }
        return condition=online && seen && result;
    }
    public void save(CompoundTag tag) {
        var list=new ListTag(); for(var e:entries) list.add(e.save()); tag.put("entries",list);
        tag.putBoolean("any",any); tag.putBoolean("hysteresis",hysteresis); tag.putBoolean("condition",condition);
        tag.putInt("strength",strength); tag.putInt("refresh",refreshTicks);
    }
    public void load(CompoundTag tag) {
        var list=tag.getList("entries",Tag.TAG_COMPOUND);
        for(int i=0;i<CAPACITY;i++) entries[i]=i<list.size()?ResourceEntry.load(list.getCompound(i)):new ResourceEntry();
        any=tag.getBoolean("any"); hysteresis=tag.getBoolean("hysteresis"); condition=tag.getBoolean("condition");
        strength=tag.contains("strength")?Math.max(1,Math.min(15,tag.getInt("strength"))):15;
        refreshTicks=Math.max(20,Math.min(1200,tag.getInt("refresh")));
    }
}
