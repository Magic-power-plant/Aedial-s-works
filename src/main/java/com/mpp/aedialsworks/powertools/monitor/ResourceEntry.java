package com.mpp.aedialsworks.powertools.monitor;
import appeng.api.stacks.AEKey;
import net.minecraft.nbt.CompoundTag;
/** A resource identity is an AEKey; counts remain long all the way to the screen. */
public final class ResourceEntry {
    public static final long MAX_AMOUNT = 1_000_000_000L;
    public AEKey key;
    public long threshold = 64, resetThreshold = 64, quantity;
    public Comparison comparison = Comparison.LESS;
    public boolean enabled = true, met;
    public boolean evaluate(long amount, boolean hysteresis) {
        quantity = Math.max(0, amount);
        long bound = hysteresis ? activeThreshold() : threshold;
        return met = enabled && key != null && comparison.test(quantity, bound);
    }
    private long activeThreshold() {
        return switch (comparison) {
            case GREATER, GREATER_EQUAL -> met ? resetThreshold : threshold;
            case LESS, LESS_EQUAL -> met ? threshold : resetThreshold;
            default -> threshold;
        };
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
        e.threshold = Math.max(0, Math.min(MAX_AMOUNT, tag.getLong("threshold")));
        e.resetThreshold = Math.max(0, Math.min(tag.getLong("reset"), e.threshold));
        e.quantity = Math.max(0, tag.getLong("quantity")); e.comparison = Comparison.byId(tag.getInt("comparison"));
        e.enabled = !tag.contains("enabled") || tag.getBoolean("enabled"); e.met = tag.getBoolean("met"); return e;
    }
}
