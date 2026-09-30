package com.mpp.aedialsworks.cellterminal.integration.ae2;
import java.util.*;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPartHost;
import appeng.helpers.InterfaceLogicHost;
import appeng.parts.storagebus.StorageBusPart;
import com.mpp.aedialsworks.cellterminal.scanner.ISubnetScanner;
import net.minecraft.core.Direction;
/** Discover both directions without forcing chunks or traversing unloaded block entities. */
public final class Ae2SubnetScanner implements ISubnetScanner {
    @Override public List<Link> connections(IGrid grid) { return connections(grid,Integer.MAX_VALUE); }
    /** Optional inspection budget for tools that must cap work on very large networks. */
    public List<Link> connections(IGrid grid,int nodeLimit) {
        var links=new ArrayList<Link>();int inspected=0;
        for(var type:grid.getMachineClasses()) for(var node:grid.getMachineNodes(type)) {
            if(inspected++>=nodeLimit)return links;
            Object owner=node.getOwner();
            if(!node.isActive()) continue;
            if(owner instanceof com.mpp.aedialsworks.cells.integration.ae2.CellsPart part && part.logic instanceof com.mpp.aedialsworks.cells.subnetproxy.ProxyLogic proxy){
                var other=part.counterpart();if(other!=null&&other.isActive()&&other.getGridNode().getGrid()!=grid)links.add(new Link(grid,other.getGridNode().getGrid(),other.cellsPos(),part.getLevel().dimension().location().toString(),part.kind==com.mpp.aedialsworks.cells.MachineKind.PROXY_FRONT));
            } else if(owner instanceof StorageBusPart bus) {
                var level=bus.getLevel();var pos=bus.getBlockEntity().getBlockPos().relative(bus.getSide());
                if(!level.hasChunkAt(pos)) continue;
                Object other=level.getBlockEntity(pos);
                if(other instanceof IPartHost host) other=host.getPart(bus.getSide().getOpposite());
                if(other instanceof InterfaceLogicHost && other instanceof IActionHost action) {
                    var target=action.getActionableNode();
                    if(target!=null && target.isActive() && target.getGrid()!=grid) links.add(new Link(grid,target.getGrid(),pos,level.dimension().location().toString(),true));
                }
            } else if(owner instanceof InterfaceLogicHost iface) {
                var be=iface.getBlockEntity();var level=be.getLevel();
                for(var direction:Direction.values()) {
                    var pos=be.getBlockPos().relative(direction);
                    if(!level.hasChunkAt(pos)) continue;
                    if(level.getBlockEntity(pos) instanceof IPartHost host && host.getPart(direction.getOpposite()) instanceof StorageBusPart bus) {
                        // A part interface only exposes its own face.
                        if(owner instanceof appeng.parts.AEBasePart part && part.getSide()!=direction) continue;
                        var target=bus.getGridNode();
                        if(target!=null && target.isActive() && target.getGrid()!=grid) links.add(new Link(grid,target.getGrid(),pos,level.dimension().location().toString(),false));
                    }
                }
            }
        }
        return links;
    }
}
