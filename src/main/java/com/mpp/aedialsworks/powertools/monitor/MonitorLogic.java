package com.mpp.aedialsworks.powertools.monitor;
import java.util.*;
import appeng.api.networking.*;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.stacks.AEKey;
import com.mpp.aedialsworks.powertools.*;
import com.mpp.aedialsworks.powertools.network.PacketPowerHud;
import com.mpp.aedialsworks.common.network.AWNetwork;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
public final class MonitorLogic extends AbstractPowerLogic implements IStorageWatcherNode {
    public final MonitorSettings settings=new MonitorSettings();
    private final Set<UUID> subscribers=new LinkedHashSet<>();
    private final boolean alarm;
    private IStackWatcher watcher;
    private int elapsed;
    private boolean dirty=true;
    private long lastAlarm;
    public MonitorLogic(PowerHost host,boolean alarm){super(host);this.alarm=alarm;host.powerNode().addService(IStorageWatcherNode.class,this);}
    @Override public void updateWatcher(IStackWatcher next){watcher=next;watch();}
    private void watch(){if(watcher!=null){watcher.reset();for(var e:settings.entries)if(e.key!=null)watcher.add(e.key);}dirty=true;}
    @Override public void onStackChange(AEKey key,long amount){dirty=true;}
    @Override protected void tick(int ticks){elapsed+=ticks;if(elapsed<settings.refreshTicks&&!dirty)return;elapsed=0;refresh();}
    public void refresh(){
        if(host.powerLevel()==null || host.powerLevel().isClientSide())return;
        boolean previous=settings.condition;long[] before=Arrays.stream(settings.entries).mapToLong(e->e.quantity).toArray();
        var grid=grid();settings.evaluate(key->grid.getStorageService().getCachedInventory().get(key),grid!=null&&host.powerActive());dirty=false;
        if(previous!=settings.condition)host.conditionChanged(settings.condition);
        if(previous!=settings.condition||!Arrays.equals(before,Arrays.stream(settings.entries).mapToLong(e->e.quantity).toArray()))changed();
        long now=host.powerLevel().getGameTime();
        if(alarm && settings.condition && (now-lastAlarm>=100||!previous)){
            lastAlarm=now;var data=PacketPowerHud.monitor("alarm",settings).data();data.putLong("pos",host.powerPos().asLong());data.putString("dimension",host.powerLevel().dimension().location().toString());
            for(var id:subscribers){var p=host.powerLevel().getServer().getPlayerList().getPlayer(id);if(p!=null)AWNetwork.sendToPlayer(p,new PacketPowerHud(data));}
        }
    }
    public boolean toggleBinding(UUID id){if(subscribers.remove(id)){changed();return false;}if(subscribers.size()<64){subscribers.add(id);changed();return true;}return false;}
    @Override public void save(CompoundTag tag){settings.save(tag);var list=new ListTag();for(var id:subscribers){var n=new CompoundTag();n.putUUID("id",id);list.add(n);}tag.put("subscribers",list);}
    @Override public CompoundTag snapshot(){var n=new CompoundTag();settings.save(n);n.putInt("bound",subscribers.size());return n;}
    @Override public void load(CompoundTag tag){settings.load(tag);subscribers.clear();for(var raw:tag.getList("subscribers",Tag.TAG_COMPOUND)){var n=(CompoundTag)raw;if(n.hasUUID("id")&&subscribers.size()<64)subscribers.add(n.getUUID("id"));}watch();}
    @Override public boolean configure(ServerPlayer player,String action,CompoundTag value){
        if(action.equals("bind")&&alarm){toggleBinding(player.getUUID());return true;}
        if(action.equals("entry")){int i=value.getInt("slot");if(i<0||i>=24)return false;settings.entries[i]=ResourceEntry.load(value);watch();changed();refresh();return true;}
        if(action.equals("settings")){settings.any=value.getBoolean("any");settings.hysteresis=value.getBoolean("hysteresis");settings.strength=Math.max(1,Math.min(15,value.getInt("strength")));settings.refreshTicks=Math.max(20,Math.min(1200,value.getInt("refresh")));changed();refresh();host.conditionChanged(settings.condition);return true;}return false;
    }
}
