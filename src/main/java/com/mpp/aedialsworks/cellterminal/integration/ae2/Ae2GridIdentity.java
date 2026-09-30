package com.mpp.aedialsworks.cellterminal.integration.ae2;
import java.nio.charset.StandardCharsets;
import java.util.*;
import appeng.api.networking.IGrid;
import appeng.parts.AEBasePart;
import net.minecraft.world.level.block.entity.BlockEntity;
/** Stable presentation identity derived from the first canonical in-world network anchor.
 * It is never an authorization token; every action resolves current reachability again.
 */
public final class Ae2GridIdentity {
    private static final Map<IGrid,Long> IDS=Collections.synchronizedMap(new WeakHashMap<>());
    private Ae2GridIdentity() {}
    public static long id(IGrid grid){Long cached=IDS.get(grid);if(cached!=null)return cached;long derived=derive(grid);if(derived!=0)IDS.put(grid,derived);return derived;}
    private static long derive(IGrid grid){
        String anchor=null;
        for(var node:grid.getNodes()){
            var owner=node.getOwner();var be=owner instanceof BlockEntity block?block:owner instanceof AEBasePart part?part.getBlockEntity():null;
            if(be==null || be.getLevel()==null)continue;
            String key=be.getLevel().dimension().location()+":"+be.getBlockPos().toShortString();
            if(anchor==null || key.compareTo(anchor)<0)anchor=key;
        }
        if(anchor==null)return 0;
        long result=UUID.nameUUIDFromBytes(anchor.getBytes(StandardCharsets.UTF_8)).getMostSignificantBits() & Long.MAX_VALUE;
        return result==0?1:result;
    }
}
