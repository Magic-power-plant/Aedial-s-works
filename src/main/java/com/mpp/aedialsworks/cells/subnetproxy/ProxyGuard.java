package com.mpp.aedialsworks.cells.subnetproxy;

import java.util.*;
import java.util.function.Function;

/** Domain graph checks are independent of AE2 and are exercised with ordinary unit tests. */
public final class ProxyGuard {
    private ProxyGuard(){}
    public static <N> boolean reaches(N start,N target,Function<N,? extends Collection<N>> edges){
        var visited=new HashSet<N>();var pending=new ArrayDeque<N>();pending.add(start);
        while(!pending.isEmpty()){
            var node=pending.removeFirst();if(node.equals(target))return true;if(!visited.add(node))continue;
            if(visited.size()>4096)return true; // Fail closed for an unbounded topology.
            for(var next:edges.apply(node))if(next!=null)pending.addLast(next);
        }return false;
    }
    public static final class Hop implements AutoCloseable {
        private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
        private boolean closed;
        private Hop(){DEPTH.set(DEPTH.get()+1);}
        public static boolean nested(){return DEPTH.get()>0;}
        public static Hop enter(){return new Hop();}
        @Override public void close(){if(!closed){closed=true;int depth=DEPTH.get()-1;if(depth==0)DEPTH.remove();else DEPTH.set(depth);}}
    }
}
