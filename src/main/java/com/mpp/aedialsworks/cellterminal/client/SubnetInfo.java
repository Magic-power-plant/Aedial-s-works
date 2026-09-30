package com.mpp.aedialsworks.cellterminal.client;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
public final class SubnetInfo extends CellInfo {
    public SubnetInfo(CompoundTag data) { super(data); }
    public List<ItemStack> getInventory() { return getContents(); }
    public static final class ConnectionPoint extends CellInfo {
        public ConnectionPoint(CompoundTag data) { super(data); }
        public boolean isOutbound() { return data.getBoolean("outbound"); }
        public List<ItemStack> getContent() { return getContents(); }
    }
}
