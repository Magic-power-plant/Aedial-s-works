package com.mpp.aedialsworks.cells.cell;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** In-memory domain state; storage adapters own persistence and resource matching. */
public final class LongLedger<K> {
    private final Map<K,Long> entries = new LinkedHashMap<>();
    private long total;
    public long get(K key) { return entries.getOrDefault(key,0L); }
    public long total() { return total; }
    public int types() { return entries.size(); }
    public Map<K,Long> entries() { return Collections.unmodifiableMap(entries); }
    public long insert(K key,long requested,long capacity,int typeLimit,long perType,boolean simulate) {
        if (key==null || requested<=0 || capacity<0 || perType<0) return 0;
        long current=get(key);
        if (current==0 && entries.size()>=typeLimit) return 0;
        long accepted=Math.min(requested,Math.min(Math.max(0,capacity-total),Math.max(0,perType-current)));
        if (!simulate && accepted>0) { entries.put(key,current+accepted); total+=accepted; }
        return accepted;
    }
    public long extract(K key,long requested,boolean simulate) {
        if (requested<=0) return 0;
        long current=get(key), extracted=Math.min(current,requested);
        if (!simulate && extracted>0) {
            if (current==extracted) entries.remove(key); else entries.put(key,current-extracted);
            total-=extracted;
        }
        return extracted;
    }
    /** Loading never reapplies a lowered configuration limit, so existing contents remain recoverable. */
    public void restore(K key,long amount) {
        if (key==null || amount<=0) return;
        if (entries.containsKey(key) || amount>Long.MAX_VALUE-total) return;
        entries.put(key,amount);total+=amount;
    }
}
