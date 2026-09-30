package com.mpp.aedialsworks.cellterminal.integration.ae2;
import appeng.api.networking.IGrid;
import appeng.blockentity.storage.*;
import appeng.parts.storagebus.StorageBusPart;
import com.mpp.aedialsworks.cellterminal.scanner.*;
import java.util.*;
public final class Ae2StorageScanner implements IStorageScanner {
    private record Key(Object owner,int slot) {}
    private final Map<Key,AbstractTerminalTarget> targets=new LinkedHashMap<>();
    private long nextId=100;
    @Override public List<AbstractTerminalTarget> scan(IGrid grid) {return scanAll(List.of(grid));}
    @Override public List<AbstractTerminalTarget> scanAll(Collection<IGrid> grids) {
        var live=new LinkedHashSet<Key>();
        for(var grid:grids) for(var type:grid.getMachineClasses()) {
            if(!DriveBlockEntity.class.isAssignableFrom(type) && !ChestBlockEntity.class.isAssignableFrom(type) && !StorageBusPart.class.isAssignableFrom(type)) continue;
            for(var node:grid.getMachineNodes(type)) {
                Object owner=node.getOwner();int count=owner instanceof DriveBlockEntity drive?drive.getInternalInventory().size():1;
                for(int slot=0;slot<count;slot++) {
                    var key=new Key(owner,slot);live.add(key);
                    var old=targets.get(key);
                    if(old==null || old.grid()!=grid) { if(old!=null)old.close();targets.put(key,new Ae2StorageTarget(nextId++,owner,slot,grid)); }
                }
            }
        }
        targets.entrySet().removeIf(e->{if(!live.contains(e.getKey())) {e.getValue().close();return true;}return false;});
        return List.copyOf(targets.values());
    }
    @Override public void close() { targets.values().forEach(AbstractTerminalTarget::close);targets.clear(); }
}
