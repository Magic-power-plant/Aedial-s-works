package com.mpp.aedialsworks.powertools.monitor;
import appeng.api.stacks.AEKey;
import net.minecraft.nbt.CompoundTag;
/** A resource identity is an AEKey; counts remain long all the way to the screen. */
public final class ResourceEntry {
    public AEKey key;
    public long threshold = 64, resetThreshold = 64, quantity;
    public Comparison comparison = Comparison.LESS;
    public boolean enabled = true, met;
    public boolean evaluate(long amount, boolean hysteresis) {
        quantity = Math.max(0, amount);
        long bound = hysteresis && met ? resetThreshold : threshold;
        return met = enabled && key != null && comparison.test(quantity, bound);
    }
    public CompoundTag save() {
        var tag = new CompoundTag();
        if (key != null) tag.put("key", key.toTagGeneric());
        tag.putLong("threshold", threshold); tag.putLong("reset", resetThreshold);
        tag.putLong("quantity", quantity); tag.putInt("comparison", comparison.ordinal());
        tag.putBoolean("enabled", enabled); tag.putBoolean("met", met); return tag;
    }
    public static ResourceEntry load(CompoundTag tag) {
        var e = new ResourceEntry(); e.key = tag.contains("key") ? AEKey.fromTagGeneric(tag.getCompound("key")) : null;
        e.threshold = Math.max(0,tag.getLong("threshold")); e.resetThreshold = Math.max(0,tag.getLong("reset"));
        e.quantity = Math.max(0,tag.getLong("quantity")); e.comparison = Comparison.byId(tag.getInt("comparison"));
        e.enabled = !tag.contains("enabled") || tag.getBoolean("enabled"); e.met = tag.getBoolean("met"); return e;
    }
}
