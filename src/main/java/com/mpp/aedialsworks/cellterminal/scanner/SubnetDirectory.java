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
    private final Map<Long,ListTag> contentsCache=new HashMap<>();
    private final java.util.function.ToLongFunction<IGrid> identity;
    public SubnetDirectory(List<ISubnetScanner> scanners,java.util.function.ToLongFunction<IGrid> identity) { this.scanners=List.copyOf(scanners);this.identity=identity; }
    public void refresh(IGrid root) {
        reachable.clear();links.clear();contentsCache.clear();if(root==null)return;
        var queue=new ArrayDeque<IGrid>();queue.add(root);
        while(!queue.isEmpty() && reachable.size()<MAX_NETWORKS) {
            var grid=queue.remove();long id=ids.computeIfAbsent(grid,g->identity.applyAsLong(g));
            if(reachable.putIfAbsent(id,grid)!=null)continue;
            for(var scanner:scanners) for(var link:scanner.connections(grid)) {
                long target=ids.computeIfAbsent(link.to(),g->identity.applyAsLong(g));
                if(!reachable.containsKey(target) && queue.size()<MAX_NETWORKS*4) {queue.add(link.to());links.putIfAbsent(target,link);}
            }
        }
        var live=Collections.newSetFromMap(new IdentityHashMap<IGrid,Boolean>());live.addAll(reachable.values());
        ids.keySet().removeIf(grid->!live.contains(grid));
    }
    public long id(IGrid grid) { return ids.getOrDefault(grid,0L); }
    public Map<Long,IGrid> grids() { return Collections.unmodifiableMap(reachable); }
    public IGrid resolve(long id) { return reachable.get(id); }
    /** AE2's cached inventory is watcher-maintained, invalidated on storage change, rebuilt at most once per tick. */
    private ListTag contents(long id,IGrid grid) { return contentsCache.computeIfAbsent(id,key->AbstractTerminalTarget.contents(grid.getStorageService().getCachedInventory())); }
    public CompoundTag snapshot() {
        var tag=new CompoundTag();var entries=new ListTag();
        for(var entry:reachable.entrySet()) {
            if(entries.size()>=MAX_NETWORKS)break;
            long id=entry.getKey();var grid=entry.getValue();
            var data=new CompoundTag();data.putLong("id",id);data.putString("name","Network "+Long.toString(id,36));data.putInt("nodes",grid.size());data.put("contents",contents(id,grid));
            var link=links.get(id);
            if(link!=null) {data.putLong("pos",link.position().asLong());data.putString("dimension",link.dimension());data.putBoolean("outbound",link.outbound());}
            entries.add(data);
        }
        tag.put("entries",entries);return tag;
    }
    @Override public void close() { ids.clear();reachable.clear();links.clear();contentsCache.clear(); }
}
