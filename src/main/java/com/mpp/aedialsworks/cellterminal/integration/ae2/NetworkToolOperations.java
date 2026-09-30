package com.mpp.aedialsworks.cellterminal.integration.ae2;
import java.util.*;
import appeng.api.storage.*;
import appeng.api.storage.cells.*;
import appeng.api.stacks.*;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.me.cells.BasicCellInventory;
import com.mpp.aedialsworks.cellterminal.scanner.AbstractTerminalTarget;
import net.minecraft.world.item.ItemStack;
/** Mutate detached NBT copies, verify conservation, then atomically install on the server thread. */
public final class NetworkToolOperations {
    private NetworkToolOperations() {}
    public static boolean massPartition(List<AbstractTerminalTarget> targets) {
        var staged=new ArrayList<List<AEKey>>();var active=new ArrayList<AbstractTerminalTarget>();
        for(var target:targets) {
            var contents=new KeyCounter();var inventory=target.storage();
            if(inventory==null)return false;
            inventory.getAvailableStacks(contents);
            var keys=new ArrayList<>(contents.keySet());keys.sort(Comparator.comparing(key->key.toTagGeneric().toString()));
            if(keys.size()>target.partitionSize())return false;
            for(var key:keys)if(!target.isPartitionAllowed(key))return false;
            staged.add(keys);active.add(target);
        }
        for(int i=0;i<active.size();i++) {
            var target=active.get(i);target.clearPartition();int slot=0;
            for(var key:staged.get(i)) { if(slot>=target.partitionSize())return false; if(!target.setPartition(slot++,key))return false; }
        }
        return true;
    }
    public static boolean attributeUnique(List<AbstractTerminalTarget> targets,IActionSource source) {
        var working=new ArrayList<ItemStack>();var originals=new ArrayList<ItemStack>();var active=new ArrayList<AbstractTerminalTarget>();
        var totals=new LinkedHashMap<AEKey,Long>();
        for(var target:targets) {
            if(!(target.cell().getItem() instanceof IBasicCellItem))continue;
            target.persist();var original=target.cell();var copy=original.copy();
            var item=(IBasicCellItem)copy.getItem();item.getConfigInventory(copy).clear();
            var storage=StorageCells.getCellInventory(copy,null);
            if(!(storage instanceof BasicCellInventory))return false;
            var contents=new KeyCounter();storage.getAvailableStacks(contents);
            for(var entry:contents) {
                var key=entry.getKey();long amount=entry.getLongValue();
                if(amount<0)return false;
                try { totals.put(key,Math.addExact(totals.getOrDefault(key,0L),amount)); } catch(ArithmeticException e) { return false; }
                if(storage.extract(key,amount,Actionable.MODULATE,source)!=amount)return false;
            }
            storage.persist();var empty=new KeyCounter();storage.getAvailableStacks(empty);if(!empty.isEmpty())return false;
            originals.add(original);working.add(copy);active.add(target);
        }
        if(totals.size()>working.size() || working.isEmpty())return false;
        var ordered=new ArrayList<>(totals.entrySet());ordered.sort(Map.Entry.<AEKey,Long>comparingByValue().reversed());
        var assigned=new HashSet<Integer>();
        for(var entry:ordered) {
            int chosen=-1;long best=Long.MAX_VALUE;
            for(int i=0;i<working.size();i++) if(!assigned.contains(i)) {
                var copy=working.get(i);var storage=StorageCells.getCellInventory(copy,null);
                if(storage.insert(entry.getKey(),entry.getValue(),Actionable.SIMULATE,source)==entry.getValue()) {
                    long bytes=((IBasicCellItem)copy.getItem()).getBytes(copy);if(bytes<best){best=bytes;chosen=i;}
                }
            }
            if(chosen<0)return false;
            var copy=working.get(chosen);var storage=StorageCells.getCellInventory(copy,null);
            if(storage.insert(entry.getKey(),entry.getValue(),Actionable.MODULATE,source)!=entry.getValue())return false;
            storage.persist();var config=((ICellWorkbenchItem)copy.getItem()).getConfigInventory(copy);
            if(config.size()==0)return false;config.setStack(0,new GenericStack(entry.getKey(),1));assigned.add(chosen);
        }
        // Also catches destructive/voiding upgrades that report insertion without retaining resources.
        var result=new HashMap<AEKey,Long>();
        for(var copy:working) {var contents=new KeyCounter();StorageCells.getCellInventory(copy,null).getAvailableStacks(contents);
            if(contents.size()>1)return false;for(var entry:contents)result.merge(entry.getKey(),entry.getLongValue(),Math::addExact);}
        if(!totals.equals(result))return false;
        for(int i=0;i<active.size();i++)if(active.get(i).cell()!=originals.get(i))return false;
        for(int i=0;i<active.size();i++)active.get(i).replaceCell(working.get(i));
        return true;
    }
}
