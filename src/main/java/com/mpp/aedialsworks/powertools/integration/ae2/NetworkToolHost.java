package com.mpp.aedialsworks.powertools.integration.ae2;
import java.util.*;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPartHost;
import appeng.helpers.IPriorityHost;
import com.mpp.aedialsworks.powertools.*;
import com.mpp.aedialsworks.powertools.items.NetworkToolItem;
import com.mpp.aedialsworks.powertools.scanner.NetworkScanner;
import com.mpp.aedialsworks.powertools.network.PacketPowerHud;
import com.mpp.aedialsworks.common.network.AWNetwork;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
public final class NetworkToolHost extends ItemMenuHost implements PowerMenuHost,PowerConfigurable {
    private final NetworkToolItem.Kind kind;
    private final BlockPos pos;
    private final String dimension;
    private final int face;
    private final Object identity;
    private final NetworkScanner scanner=new NetworkScanner();
    private CompoundTag data=new CompoundTag();
    private long lastScan=-100;
    public NetworkToolHost(Player player,int slot,ItemStack stack,NetworkToolItem.Kind kind){
        super(player,slot,stack);this.kind=kind;var n=stack.getOrCreateTag();pos=BlockPos.of(n.getLong("target"));dimension=n.getString("dimension");face=n.getInt("face");identity=resolve();refresh();
    }
    private Object resolve(){var level=getPlayer().level();if(!level.dimension().location().toString().equals(dimension)||!level.hasChunkAt(pos))return null;Object target=level.getBlockEntity(pos);if(target instanceof IPartHost parts)target=parts.getPart(face<0?null:Direction.from3DDataValue(face));return target;}
    @Override public IGridNode getActionableNode(){var target=resolve();return target==identity&&target instanceof IActionHost host?host.getActionableNode():null;}
    @Override public boolean canUsePower(Player player){return player==getPlayer()&&getSlot()!=null&&player.getInventory().getItem(getSlot())==getItemStack()&&player.distanceToSqr(Vec3.atCenterOf(pos))<=64&&(isClientSide()||getActionableNode()!=null);}
    @Override public PowerConfigurable configuration(){return this;}
    @Override public String powerKind(){return kind==NetworkToolItem.Kind.SCANNER?"network_health_scanner":kind==NetworkToolItem.Kind.TUNER?"priority_tuner":"network_component_locator";}
    private void refresh(){if(!isClientSide()&&getActionableNode()!=null){data=scanner.scan(getActionableNode().getGrid());lastScan=getPlayer().level().getGameTime();}}
    @Override public CompoundTag snapshot(){return data.copy();}
    @Override public boolean configure(ServerPlayer player,String action,CompoundTag value){
        if(!canUsePower(player))return false;
        if(action.equals("scan")){if(player.level().getGameTime()-lastScan<20)return false;refresh();return true;}
        int index=value.getInt("target");var nodes=scanner.targets();if(index<0||index>=nodes.size())return false;var target=nodes.get(index);
        if(!NetworkScanner.reachable(getActionableNode().getGrid()).contains(target.getGrid())||!target.getLevel().hasChunkAt(NetworkScanner.position(target)))return false;
        boolean live=false;for(var n:target.getGrid().getNodes())if(n==target){live=true;break;}if(!live)return false;
        if(action.equals("priority")&&kind==NetworkToolItem.Kind.TUNER&&target.getOwner() instanceof IPriorityHost host){host.setPriority(value.getInt("priority"));refresh();return true;}
        if(action.equals("locate")){var n=new CompoundTag();n.putString("kind","locate");long location=NetworkScanner.position(target).asLong();
            if(value.contains("pos"))for(var raw:data.getList("issues",net.minecraft.nbt.Tag.TAG_COMPOUND)){var issue=(CompoundTag)raw;if(issue.getInt("id")==index&&issue.getLong("pos")==value.getLong("pos")){location=issue.getLong("pos");break;}}
            n.putLong("pos",location);n.putString("dimension",target.getLevel().dimension().location().toString());AWNetwork.sendToPlayer(player,new PacketPowerHud(n));return true;}return false;
    }
}
