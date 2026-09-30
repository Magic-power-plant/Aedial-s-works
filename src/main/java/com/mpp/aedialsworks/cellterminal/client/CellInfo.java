package com.mpp.aedialsworks.cellterminal.client;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import appeng.api.stacks.*;
/** Immutable presentation snapshot; quantities remain long in GenericStack tags. */
public class CellInfo {
    protected final CompoundTag data;
    public CellInfo(CompoundTag data) { this.data = data.copy(); }
    public ItemStack getCellItem() { return ItemStack.of(data.getCompound("item")); }
    public String getDisplayName() { return data.getString("name"); }
    public boolean hasCustomName() { return data.getBoolean("customName"); }
    public List<ItemStack> getContents() { return items(data, "contents"); }
    public List<ItemStack> getPartition() { return items(data, "partition"); }
    public static List<ItemStack> items(CompoundTag tag, String key) {
        var result = new ArrayList<ItemStack>();
        for (var entry : tag.getList(key, Tag.TAG_COMPOUND)) {
            var stack = GenericStack.readTag((CompoundTag) entry);
            if (stack != null) result.add(stack.what() instanceof AEItemKey item
                    ? item.toStack() : GenericStack.wrapInItemStack(stack));
        }
        return result;
    }
}
