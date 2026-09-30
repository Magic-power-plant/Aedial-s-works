package com.mpp.aedialsworks.powertools.scanner;
import java.util.*;
import appeng.api.networking.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPart;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEKey;
import appeng.helpers.IPriorityHost;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import com.mpp.aedialsworks.cellterminal.integration.ae2.Ae2SubnetScanner;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.entity.BlockEntity;
/** Bounded public-API traversal. It never loads chunks and never guesses private node NBT. */
public final class NetworkScanner {
    public static final List<String> TABS=List.of("loops","unloaded","bottlenecks","channels","fatal","patterns");
    public static final int MAX_NODES=4096,MAX_GRIDS=64;
    private final List<IGridNode> targets=new ArrayList<>();
    public List<IGridNode> targets(){return List.copyOf(targets);}
    public static List<IGrid> reachable(IGrid root){
        var found=Collections.newSetFromMap(new IdentityHashMap<IGrid,Boolean>());var queue=new ArrayList<IGrid>();
        if(root==null)return queue;found.add(root);queue.add(root);var scanner=new Ae2SubnetScanner();
        for(int i=0;i<queue.size() && queue.size()<MAX_GRIDS;i++)for(var link:scanner.connections(queue.get(i),MAX_NODES))if(found.add(link.to())){queue.add(link.to());if(queue.size()==MAX_GRIDS)break;}
        return queue;
    }
    public static BlockPos position(IGridNode node){Object owner=node.getOwner();return owner instanceof BlockEntity be?be.getBlockPos():owner instanceof appeng.parts.AEBasePart part?part.getBlockEntity().getBlockPos():null;}
    public CompoundTag scan(IGrid root){
        targets.clear();var out=new CompoundTag();var rows=new ListTag();var issues=new ListTag();var grids=reachable(root);
        Set<IGridConnection> edges=Collections.newSetFromMap(new IdentityHashMap<>());var parent=new IdentityHashMap<IGridNode,IGridNode>();
        Map<AEKey,IGridNode> outputs=new HashMap<>();Set<String> unloaded=new HashSet<>();boolean truncated=false;
        outer:for(var grid:grids)for(var node:grid.getNodes()){
            if(targets.size()>=MAX_NODES){truncated=true;break outer;}
            var pos=position(node);if(pos==null)continue;int index=targets.size();targets.add(node);var row=row(node,index);rows.add(row);
            if(!node.isPowered())issues.add(issue(row,"fatal","NO_POWER"));
            if(grid.getPathingService().getControllerState()==appeng.api.networking.pathing.ControllerState.CONTROLLER_CONFLICT)issues.add(issue(row,"fatal","CONTROLLER_CONFLICT"));
            if(node.hasFlag(GridFlags.REQUIRE_CHANNEL)&&!node.meetsChannelRequirements())issues.add(issue(row,"channels","MISSING_CHANNEL"));
            if(node.getMaxChannels()>0&&node.getUsedChannels()>=node.getMaxChannels())issues.add(issue(row,"bottlenecks","CHANNEL_SATURATED"));
            for(var edge:node.getConnections())if(edges.add(edge)){
                var a=find(parent,edge.a());var b=find(parent,edge.b());
                if(a==b)issues.add(issue(row,"loops","NETWORK_LOOP"));else parent.put(a,b);
            }
            for(var side:Direction.values()){
                var neighbor=pos.relative(side);if(!node.getLevel().hasChunkAt(neighbor)){
                    String id=node.getLevel().dimension().location()+":"+(neighbor.getX()>>4)+":"+(neighbor.getZ()>>4);
                    if(unloaded.add(id)){var r=row.copy();r.putLong("pos",neighbor.asLong());issues.add(issue(r,"unloaded","UNLOADED_BOUNDARY"));}
                }
            }
            if(node.getOwner() instanceof PatternProviderLogicHost provider)for(var stack:provider.getLogic().getPatternInv())if(!stack.isEmpty()){
                var details=PatternDetailsHelper.decodePattern(stack,node.getLevel());
                if(details==null){issues.add(issue(row,"patterns","INVALID_PATTERN"));continue;}
                for(var output:details.getOutputs())if(output!=null&&outputs.putIfAbsent(output.what(),node)!=null){issues.add(issue(row,"patterns","DUPLICATE_OUTPUT"));break;}
            }
        }
        out.put("nodes",rows);out.put("issues",issues);out.putInt("grids",grids.size());out.putBoolean("truncated",truncated||grids.size()==MAX_GRIDS);return out;
    }
    private static IGridNode find(Map<IGridNode,IGridNode> parent,IGridNode node){var root=node;while(parent.containsKey(root)&&parent.get(root)!=root)root=parent.get(root);while(parent.containsKey(node)&&parent.get(node)!=root){var next=parent.put(node,root);node=next;}return root;}
    private static CompoundTag row(IGridNode node,int id){
        var row=new CompoundTag();row.putInt("id",id);row.putLong("pos",position(node).asLong());row.putString("dimension",node.getLevel().dimension().location().toString());
        row.putString("name",node.getVisualRepresentation()==null?node.getOwner().getClass().getSimpleName():node.getVisualRepresentation().getDisplayName().getString());
        if(node.getVisualRepresentation()!=null)row.put("icon",node.getVisualRepresentation().toTagGeneric());
        row.putBoolean("active",node.isActive());row.putInt("channels",node.getUsedChannels());row.putInt("capacity",node.getMaxChannels());
        if(node.getOwner() instanceof IPriorityHost host){row.putBoolean("tunable",true);row.putInt("priority",host.getPriority());}return row;
    }
    private static CompoundTag issue(CompoundTag row,String tab,String code){var n=row.copy();n.putString("tab",tab);n.putString("code",code);return n;}
}
