package com.mpp.aedialsworks.cellterminal.client;
import net.minecraft.nbt.CompoundTag;
public final class StorageInfo {
    private final CompoundTag data;
    public StorageInfo(CompoundTag data) { this.data = data.copy(); }
    public int getPriority() { return data.getInt("priority"); }
    public String getName() { return data.getString("storageName"); }
    public boolean hasCustomName() { return data.getBoolean("storageCustomName"); }
}
