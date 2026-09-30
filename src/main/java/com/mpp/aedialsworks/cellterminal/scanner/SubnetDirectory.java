package com.mpp.aedialsworks.cellterminal.scanner;
import java.util.*;
import appeng.api.networking.IGrid;
import net.minecraft.nbt.*;
/** Session-stable network ids; unreachable grids are removed before any action resolves them. */
public final class SubnetDirectory implements AutoCloseable {
    public static final int MAX_NETWORKS=128;
    private final List<ISubnetScanner> scanners;
    private final IdentityHashMap<IGrid,Long> ids=new IdentityHashMap<>();
    private final Map<Long,IGrid> reachable=new LinkedHashMap<>();
    private final Map<Long,ISubnetScanner.Link> links=new HashMap<>();
    private final java.util.function.ToLongFunction<IGrid> identity;
    public SubnetDirectory(List<ISubnetScanner> scanners,java.util.function.ToLongFunction<IGrid> identity) { this.scanners=List.copyOf(scanners);this.identity=identity; }
    public void refresh(IGrid root) {
        reachable.clear();links.clear();if(root==null)return;
        var queue=new ArrayDeque<IGrid>();queue.add(root);
        while(!queue.isEmpty() && reachable.size()<MAX_NETWORKS) {
            var grid=queue.remove();long id=ids.computeIfAbsent(grid,g->identity.applyAsLong(g));
            if(reachable.putIfAbsent(id,grid)!=null)continue;
            for(var scanner:scanners) for(var link:scanner.connections(grid)) {
                long target=ids.computeIfAbsent(link.to(),g->identity.applyAsLong(g));
                if(!reachable.containsKey(target) && queue.size()<MAX_NETWORKS*4) {queue.add(link.to());links.putIfAbsent(target,link);}
            }
        }
        ids.keySet().removeIf(grid->!reachable.containsValue(grid));
    }
    public long id(IGrid grid) { return ids.getOrDefault(grid,0L); }
    public Map<Long,IGrid> grids() { return Collections.unmodifiableMap(reachable); }
    public IGrid resolve(long id) { return reachable.get(id); }
    public CompoundTag snapshot() {
        var tag=new CompoundTag();var entries=new ListTag();
        reachable.forEach((id,grid)->{
            var entry=new CompoundTag();entry.putLong("id",id);entry.putString("name","Network "+Long.toString(id,36));entry.putInt("nodes",grid.size());entry.put("contents",AbstractTerminalTarget.contents(grid.getStorageService().getInventory()));
            var link=links.get(id);
            if(link!=null) {entry.putLong("pos",link.position().asLong());entry.putString("dimension",link.dimension());entry.putBoolean("outbound",link.outbound());}
            entries.add(entry);
        });tag.put("entries",entries);return tag;
    }
    @Override public void close() { ids.clear();reachable.clear();links.clear(); }
}
