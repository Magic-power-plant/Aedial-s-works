package com.mpp.aedialsworks.cells.subnetproxy;

import java.util.*;
import appeng.api.config.Actionable;
import appeng.api.networking.*;
import appeng.api.networking.ticking.*;
import com.mpp.aedialsworks.Aedialsworks;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.stacks.*;
import appeng.api.storage.*;
import com.mpp.aedialsworks.cells.*;
import com.mpp.aedialsworks.cells.api.*;
import com.mpp.aedialsworks.cells.cell.CellMath;
import com.mpp.aedialsworks.cells.integration.ae2.CellsPart;
import com.mpp.aedialsworks.common.config.AWConfigs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** A filtered physical-storage view. Forwarded calls never re-enter another proxy (including writes). */
public final class ProxyLogic extends AbstractCellsLogic implements IStorageProvider,IStorageWatcherNode,SubnetView {
    private static final Set<ProxyLogic> LIVE=Collections.newSetFromMap(new IdentityHashMap<>());
    private final MEStorage view=new View();
    private IGrid lastOrigin,lastDestination;
    private String lastStatus="NO_PAIR";
    private long lastRefresh;
    private boolean refreshPending=true;
    private long churnStart=-1,cacheInvalidations,watcherSignals,providerUpdates,lastFault=-1;
    public ProxyLogic(CellsHost host){super(host);host.cellsNode().addService(IStorageProvider.class,this);host.cellsNode().addService(IStorageWatcherNode.class,this);}
    private boolean front(){return host.cellsKind()==MachineKind.PROXY_FRONT;}
    public ProxyLogic counterpart(){return host instanceof CellsPart part&&part.counterpart() != null&&part.counterpart().cellsLogic() instanceof ProxyLogic proxy?proxy:null;}
    @Override public IGrid originGrid(){var other=counterpart();return front()&&other!=null?other.grid():null;}
    @Override public IGrid destinationGrid(){return front()?grid():null;}
    private boolean pairActive(){var other=counterpart();return front()&&active()&&other!=null&&other.active()&&originGrid()!=null&&destinationGrid()!=null;}
    @Override public String guardStatus(){
        if(!front())return counterpart()==null?"NO_PAIR":"BACK_SIDE";
        if(counterpart()==null)return "NO_PAIR";if(!pairActive())return "OFFLINE";
        var origin=originGrid();var destination=destinationGrid();if(origin==destination)return "SAME_GRID";
        boolean cycle=ProxyGuard.reaches(origin,destination,node->{var next=new ArrayList<IGrid>();for(var p:LIVE)if(p.pairActive()&&p.destinationGrid()==node)next.add(p.originGrid());return next;});
        return cycle?"CYCLE":"READY";
    }
    private boolean elected(AEKey key){
        if(!accepts(key))return false;var origin=originGrid();var destination=destinationGrid();
        for(var peer:LIVE)if(peer!=this&&peer.pairActive()&&peer.originGrid()==origin&&peer.destinationGrid()==destination&&peer.accepts(key)){
            if(peer.priority>priority||peer.priority==priority&&peer.stableId().compareTo(stableId())<0)return false;
        }return true;
    }
    private String stableId(){return host.cellsLevel().dimension().location()+":"+host.cellsPos().asLong()+":"+(host instanceof CellsPart p?p.getSide():"");}
    private boolean usable(){return !ProxyGuard.Hop.nested()&&front()&&guardStatus().equals("READY");}
    @Override public void mountInventories(IStorageMounts mounts){if(front())mounts.mount(view,priority);}
    @Override public void resume(){if(serverSide())LIVE.add(this);super.resume();if(serverSide())refreshPeers(this);}
    @Override public void unload(){if(serverSide())LIVE.remove(this);super.unload();if(serverSide()){refreshPeers(this);invalidateView(false);}}
    private boolean serverSide(){return host.cellsLevel()!=null&&!host.cellsLevel().isClientSide;}
    @Override protected void onSettingsChanged(){if(!serverSide())return;if(host.cellsNode().isReady())IStorageProvider.requestUpdate(host.cellsNode());refreshPeers(this);}
    private static void refreshPeers(ProxyLogic changed){
        var origin=changed.originGrid();var destination=changed.destinationGrid();
        for(var p:List.copyOf(LIVE))if(p.front()&&p.originGrid()==origin&&p.destinationGrid()==destination)p.invalidateView(false);
    }
    private void invalidateView(boolean fromWatcher){
        refreshPending=true;
        if(AWConfigs.SERVER.cells.general.subnetProxyReportUpdateChurn.get()){
            beginObservation();if(fromWatcher)watcherSignals++;
        }
    }
    private void beginObservation(){if(churnStart<0)churnStart=host.cellsLevel().getGameTime();}
    private void reportRefresh(boolean providerChanged){
        var config=AWConfigs.SERVER.cells.general;
        if(!config.subnetProxyReportUpdateChurn.get()){churnStart=-1;cacheInvalidations=watcherSignals=providerUpdates=0;return;}
        beginObservation();if(providerChanged)providerUpdates++;
        long now=host.cellsLevel().getGameTime();
        if(now-churnStart>=config.subnetProxyUpdateChurnLogDelay.get()*1200L){
            Aedialsworks.LOGGER.info("CELLS proxy {}: {} cache invalidations, {} storage signals, {} provider updates in {} ticks",stableId(),cacheInvalidations,watcherSignals,providerUpdates,now-churnStart);
            churnStart=now;cacheInvalidations=watcherSignals=providerUpdates=0;
        }
    }
    @Override public TickingRequest getTickingRequest(IGridNode node){
        var config=AWConfigs.SERVER.cells.general;int min=config.subnetProxyMinTickRate.get();
        return new TickingRequest(min,Math.max(min,config.subnetProxyMaxTickRate.get()),false,true,min);
    }
    @Override public TickRateModulation tickingRequest(IGridNode node,int delta){
        return active()&&tick()?TickRateModulation.URGENT:TickRateModulation.SLOWER;
    }
    @Override public void updateWatcher(IStackWatcher watcher){watcher.reset();watcher.setWatchAll(!front());}
    @Override public void onStackChange(AEKey key,long amount){if(!front()){var other=counterpart();if(other!=null)other.invalidateView(true);}}
    @Override public boolean tick(){
        if(!front())return false;long now=host.cellsLevel().getGameTime();int min=AWConfigs.SERVER.cells.general.subnetProxyMinTickRate.get();
        if(now-lastRefresh<min)return false;lastRefresh=now;var origin=originGrid();var destination=destinationGrid();String status=guardStatus();
        boolean providerChanged=origin!=lastOrigin||destination!=lastDestination||!status.equals(lastStatus);
        if(providerChanged){lastOrigin=origin;lastDestination=destination;lastStatus=status;IStorageProvider.requestUpdate(host.cellsNode());changed();}
        boolean work=providerChanged||refreshPending;refreshPending=false;
        if(work&&destination!=null){destination.getStorageService().invalidateCache();if(AWConfigs.SERVER.cells.general.subnetProxyReportUpdateChurn.get())cacheInvalidations++;}
        reportRefresh(providerChanged);return work;
    }
    @Override public CompoundTag snapshot(int port,int page){var n=super.snapshot(port,page);n.putString("guard",guardStatus());n.putBoolean("insertion",installed("insertion_card"));return n;}
    private final class View implements MEStorage {
        @Override public Component getDescription(){return Component.translatable("item.aedialsworks.subnet_proxy_front");}
        @Override public long insert(AEKey key,long amount,Actionable mode,IActionSource source){
            MEStorage.checkPreconditions(key,amount,mode,source);if(!usable()||!installed("insertion_card")||!elected(key))return 0;
            try(var hop=ProxyGuard.Hop.enter()){return originGrid().getStorageService().getInventory().insert(key,amount,mode,source);}
        }
        @Override public long extract(AEKey key,long amount,Actionable mode,IActionSource source){
            MEStorage.checkPreconditions(key,amount,mode,source);if(!usable()||!elected(key))return 0;
            try(var hop=ProxyGuard.Hop.enter()){
                var inventory=originGrid().getStorageService().getInventory();long extracted=inventory.extract(key,amount,mode,source);
                if(mode==Actionable.MODULATE&&extracted<amount&&AWConfigs.SERVER.cells.general.subnetProxyReportExtractionFaults.get()){
                    long now=host.cellsLevel().getGameTime();
                    if(lastFault<0||now-lastFault>=20){
                        var visible=new KeyCounter();inventory.getAvailableStacks(visible);
                        if(visible.get(key)>0){lastFault=now;Aedialsworks.LOGGER.warn("CELLS proxy {} extraction mismatch for {}: requested {}, extracted {}, still visible {}",stableId(),key.getDisplayName().getString(),amount,extracted,visible.get(key));}
                    }
                }
                return extracted;
            }
        }
        @Override public void getAvailableStacks(KeyCounter out){
            if(!usable())return;var actual=new KeyCounter();try(var hop=ProxyGuard.Hop.enter()){originGrid().getStorageService().getInventory().getAvailableStacks(actual);}
            for(var entry:actual)if(entry.getLongValue()>0&&elected(entry.getKey()))out.set(entry.getKey(),CellMath.add(out.get(entry.getKey()),entry.getLongValue()));
        }
        @Override public boolean isPreferredStorageFor(AEKey key,IActionSource source){return usable()&&elected(key);}
    }
    public MEStorage storageView(){return view;}
}
