package com.mpp.aedialsworks.cellterminal.integration.ae2;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.*;
import appeng.api.storage.cells.*;
import appeng.api.stacks.*;
import appeng.api.upgrades.*;
import appeng.api.config.*;
import appeng.blockentity.storage.*;
import appeng.parts.storagebus.StorageBusPart;
import appeng.me.cells.BasicCellInventory;
import appeng.util.ConfigInventory;
import com.mpp.aedialsworks.cellterminal.menu.TerminalHost;
import com.mpp.aedialsworks.cellterminal.scanner.AbstractTerminalTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
/** All version-specific cell slots/configuration/mounted-cache invalidation live here. */
public final class Ae2StorageTarget extends AbstractTerminalTarget {
    private final Object owner; private final int slot; private final IGrid grid;
    public Ae2StorageTarget(long id,Object owner,int slot,IGrid grid) { super(id);this.owner=owner;this.slot=slot;this.grid=grid;observe(); }
    @Override public String kind() { return owner instanceof StorageBusPart?"bus":owner instanceof TerminalHost?"temp":owner instanceof ChestBlockEntity?"chest":"drive"; }
    @Override public IGrid grid() { return grid; }
    private BlockEntity block() { return owner instanceof BlockEntity be?be:owner instanceof StorageBusPart bus?bus.getBlockEntity():null; }
    @Override public boolean valid(Player player) {
        if(owner instanceof TerminalHost host) return host.canUseTerminal(player);
        var be=block(); if(be==null || be.isRemoved() || be.getLevel()==null || !be.getLevel().hasChunkAt(be.getBlockPos()) || be.getLevel().getBlockEntity(be.getBlockPos())!=be) return false;
        if(!(owner instanceof IActionHost host)) return false;
        var node=host.getActionableNode();
        if(owner instanceof StorageBusPart bus && bus.getHost().getPart(bus.getSide())!=bus) return false;
        return node!=null && node.getGrid()==grid && node.isActive();
    }
    @Override public ItemStack cell() {
        if(owner instanceof DriveBlockEntity drive) return drive.getInternalInventory().getStackInSlot(slot);
        if(owner instanceof ChestBlockEntity chest) return chest.getCell();
        if(owner instanceof TerminalHost host) return host.temporaryCells().getStackInSlot(slot);
        return ItemStack.EMPTY;
    }
    @Override public void replaceCell(ItemStack stack) {
        if(owner instanceof DriveBlockEntity drive) drive.getInternalInventory().setItemDirect(slot,stack);
        else if(owner instanceof ChestBlockEntity chest) chest.setCell(stack); // Never touch the chest's input slot (combined slot 0).
        else if(owner instanceof TerminalHost host) host.temporaryCells().setItemDirect(slot,stack);
        observe();
    }
    @Override public MEStorage storage() {
        if(owner instanceof StorageBusPart bus) return bus.getInternalHandler();
        if(owner instanceof DriveBlockEntity drive) { var mounted=drive.getOriginalCellInventory(slot); if(mounted!=null) return mounted; }
        if(owner instanceof ChestBlockEntity chest) { var mounted=chest.getOriginalCellInventory(0); if(mounted!=null) return mounted; }
        return StorageCells.getCellInventory(cell(),null);
    }
    @Override public void persist() { var inventory=storage(); if(inventory instanceof StorageCell cell) cell.persist(); }
    @Override public String name() { return owner instanceof StorageBusPart bus?bus.getName().getString():cell().isEmpty()?"—":cell().getHoverName().getString(); }
    @Override public String storageName() { return owner instanceof net.minecraft.world.Nameable named?named.getName().getString():"Temporary cells"; }
    @Override public String dimension() { var be=block(); return be==null?"":be.getLevel().dimension().location().toString(); }
    @Override public BlockPos position() { var be=block();return be==null?BlockPos.ZERO:be.getBlockPos(); }
    @Override public int slot() { return slot; }
    @Override public int priority() { return owner instanceof appeng.helpers.IPriorityHost host?host.getPriority():0; }
    @Override public void setPriority(int priority) { if(owner instanceof appeng.helpers.IPriorityHost host) host.setPriority(priority); }
    private ConfigInventory config(ItemStack stack) {
        if(owner instanceof StorageBusPart bus) return bus.getConfig();
        return stack.getItem() instanceof ICellWorkbenchItem item && item.isEditable(stack)?item.getConfigInventory(stack):ConfigInventory.EMPTY_TYPES;
    }
    @Override public int partitionSize() { return config(cell()).size(); }
    @Override public AEKey partitionKey(int slot) { return config(cell()).getKey(slot); }
    @Override public boolean setPartition(int slot,AEKey key) {
        persist(); var copy=cell().copy();var inv=config(copy);
        if(slot<0 || slot>=inv.size() || (key!=null && !inv.isAllowed(key))) return false;
        inv.setStack(slot,key==null?null:new GenericStack(key,1));
        if(!(owner instanceof StorageBusPart)) replaceCell(copy);
        return true;
    }
    @Override public void clearPartition() {
        persist();var copy=cell().copy();config(copy).clear();if(!(owner instanceof StorageBusPart)) replaceCell(copy);
    }
    @Override public void cycleFuzzyMode() {
        if(owner instanceof StorageBusPart bus) {
            var cm=bus.getConfigManager();var mode=cm.getSetting(Settings.FUZZY_MODE);
            cm.putSetting(Settings.FUZZY_MODE,FuzzyMode.values()[(mode.ordinal()+1)%FuzzyMode.values().length]);
        } else if(cell().getItem() instanceof ICellWorkbenchItem item) {
            persist();var copy=cell().copy();var mode=item.getFuzzyMode(copy);
            item.setFuzzyMode(copy,FuzzyMode.values()[(mode.ordinal()+1)%FuzzyMode.values().length]);replaceCell(copy);
        }
    }
    private IUpgradeInventory upgrades(ItemStack stack) {
        return owner instanceof StorageBusPart bus?bus.getUpgrades():stack.getItem() instanceof IUpgradeableItem item?item.getUpgrades(stack):null;
    }
    @Override public int upgradeSlots() { var inv=upgrades(cell());return inv==null?0:inv.size(); }
    @Override public ItemStack upgrade(int slot) { var inv=upgrades(cell());return inv==null?ItemStack.EMPTY:inv.getStackInSlot(slot); }
    @Override public ItemStack insertUpgrade(ItemStack stack) {
        persist();var copy=cell().copy();var inv=upgrades(copy);if(inv==null) return stack;
        var rest=inv.addItems(stack.copy()); if(!(owner instanceof StorageBusPart)) replaceCell(copy);return rest;
    }
    @Override public ItemStack extractUpgrade(int slot) {
        persist();var copy=cell().copy();var inv=upgrades(copy);
        if(inv==null || slot<0 || slot>=inv.size()) return ItemStack.EMPTY;
        var result=inv.extractItem(slot,1,false);if(!(owner instanceof StorageBusPart)) replaceCell(copy);return result;
    }
    @Override protected void describe(CompoundTag tag) {
        var inventory=storage();
        if(cell().getItem() instanceof IBasicCellItem basicItem)tag.putString("keyType",basicItem.getKeyType()==AEKeyType.fluids()?"fluid":"item");
        if(inventory instanceof StorageCell cell) tag.putString("status",cell.getStatus().name());
        if(inventory instanceof BasicCellInventory basic) { tag.putLong("usedBytes",basic.getUsedBytes()); tag.putLong("totalBytes",basic.getTotalBytes()); }
        if(inventory instanceof com.mpp.aedialsworks.cells.api.CellCapacity capacity) { tag.putLong("usedBytes",capacity.usedBytes()); tag.putLong("totalBytes",capacity.totalBytes()); }
        if(cell().getItem() instanceof com.mpp.aedialsworks.cells.cell.TieredCellItem item)tag.putString("keyType",item.keyType(cell())==AEKeyType.fluids()?"fluid":"item");
        if(owner instanceof net.minecraft.world.Nameable named) tag.putBoolean("storageCustomName",named.hasCustomName());
        if(owner instanceof StorageBusPart bus) tag.putString("fuzzy",bus.getConfigManager().getSetting(Settings.FUZZY_MODE).name());
        else if(cell().getItem() instanceof ICellWorkbenchItem item) tag.putString("fuzzy",item.getFuzzyMode(cell()).name());
    }
}
