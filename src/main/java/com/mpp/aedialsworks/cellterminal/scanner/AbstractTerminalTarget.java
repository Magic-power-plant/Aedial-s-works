package com.mpp.aedialsworks.cellterminal.scanner;
import appeng.api.networking.IGrid;
import appeng.api.storage.MEStorage;
import appeng.api.stacks.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** Owns the lifecycle of a server-resolved target; capabilities describe its supported operations. */
public abstract class AbstractTerminalTarget implements PartitionAccess, PriorityAccess, UpgradeAccess, AutoCloseable {
    private final long id;
    private ItemStack observed;
    private long token;
    private boolean closed;
    protected AbstractTerminalTarget(long id) { this.id=id; }
    public final long id() { return id; }
    public final long token() { return token; }
    public final void observe() { var current=cell(); if(current!=observed) { observed=current; token++; } }
    public final boolean matches(long token) { return !closed && this.token==token && observed==cell(); }
    @Override public final void close() { closed=true; observed=null; }
    public abstract String kind();
    public abstract IGrid grid();
    public abstract boolean valid(Player player);
    public abstract ItemStack cell();
    public abstract void replaceCell(ItemStack stack);
    public abstract MEStorage storage();
    public abstract String name();
    public abstract String storageName();
    public abstract String dimension();
    public abstract BlockPos position();
    public abstract int slot();
    public abstract void persist();
    protected void describe(CompoundTag tag) {}
    public final CompoundTag snapshot(boolean contents) {
        persist(); observe();
        var tag=new CompoundTag(); tag.putLong("id",id); tag.putLong("token",token); tag.putString("kind",kind());
        tag.putString("name",name()); tag.putString("storageName",storageName()); tag.putInt("priority",priority());
        tag.putInt("slot",slot()); tag.putLong("pos",position().asLong()); tag.putString("dimension",dimension());
        tag.putBoolean("customName",cell().hasCustomHoverName());
        var icon=cell().copy(); if(icon.hasTag()) { var display=icon.getTag().getCompound("display").copy(); icon.setTag(null); if(!display.isEmpty()) icon.getOrCreateTag().put("display",display); }
        tag.put("item",icon.save(new CompoundTag()));
        tag.putInt("partitionSize",partitionSize()); var partition=new ListTag();
        for(int i=0;i<partitionSize();i++) { var key=partitionKey(i); if(key!=null) { var entry=GenericStack.writeTag(new GenericStack(key,1)); entry.putInt("slot",i); partition.add(entry); } }
        tag.put("partition",partition);
        var upgrades=new ListTag();
        for(int i=0;i<upgradeSlots();i++) { var entry=upgrade(i).save(new CompoundTag()); entry.putInt("slot",i); upgrades.add(entry); }
        tag.put("upgrades",upgrades);
        if(contents) tag.put("contents",contents(storage()));
        describe(tag); return tag;
    }
    public static ListTag contents(MEStorage inventory) {
        var counter=new KeyCounter(); if(inventory!=null) inventory.getAvailableStacks(counter);
        return contents(counter);
    }
    public static ListTag contents(KeyCounter counter) {
        var list=new ListTag();
        var entries=new ArrayList<GenericStack>();
        for(var entry:counter) if(entry.getLongValue()>0) entries.add(new GenericStack(entry.getKey(),entry.getLongValue()));
        entries.sort(Comparator.comparing((GenericStack s)->s.what().getId().toString()).thenComparing(s->s.what().toTagGeneric().toString()));
        for(var stack:entries) list.add(GenericStack.writeTag(stack));
        return list;
    }
}
