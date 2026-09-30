package com.mpp.aedialsworks.cellterminal.client;
import net.minecraft.nbt.*;
public final class StorageBusInfo extends CellInfo {
    public StorageBusInfo(CompoundTag data) { super(data); }
    public String getLocalizedName() { return getDisplayName(); }
    public int getPriority() { return data.getInt("priority"); }
    public int getContentTypeCount() { return data.getList("contents", Tag.TAG_COMPOUND).size(); }
    public int getPartitionCount() { return data.getList("partition", Tag.TAG_COMPOUND).size(); }
}
