package com.mpp.aedialsworks.cellterminal.menu;
import java.util.*;
import java.io.IOException;
import appeng.api.networking.IGrid;
import appeng.api.storage.StorageCells;
import appeng.api.stacks.*;
import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.cellterminal.integration.ae2.*;
import com.mpp.aedialsworks.cellterminal.network.*;
import com.mpp.aedialsworks.cellterminal.scanner.*;
import com.mpp.aedialsworks.common.config.AWConfigs;
import com.mpp.aedialsworks.common.network.*;
import com.mpp.aedialsworks.common.util.AWIds;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
/** One lifecycle for scanning, authority, temporary cells, deltas and client assembly. */
public final class CellTerminalMenu extends TerminalMenuAdapter implements ServerMenuReceiver,ClientMenuReceiver {
    private final TerminalHost host;
    private final UUID session;
    private final IStorageScanner scanner=new Ae2StorageScanner();
    private final SubnetDirectory networks=new SubnetDirectory(List.of(new Ae2SubnetScanner()),Ae2GridIdentity::id);
    private final Map<Long,AbstractTerminalTarget> targets=new LinkedHashMap<>();
    private final List<AbstractTerminalTarget> temp=new ArrayList<>();
    private final DeltaSnapshot delta=new DeltaSnapshot();
    private final Map<String,Long> revisions=new HashMap<>();
    private final ChunkAssembler assembler=new ChunkAssembler();
    private final Map<String,CompoundTag> clientData=new HashMap<>();
    private String subnetVisibility="DONT_SHOW";
    private final Set<Long> favorites=new HashSet<>();
    private TerminalTab tab=TerminalTab.TERMINAL;
    private IGrid selectedGrid;
    private long selectedNetwork,tick,lastRefresh=-200,lastBus=-200,lastManualRefresh=-200,actionTick=-1;
    private int actions;
    private boolean force=true,closed;
    private final int toolboxSlot;
    private ItemStack toolboxStack;
    private String feedback="";
    public CellTerminalMenu(int id,Inventory inventory,TerminalHost host,UUID session) {
        super(id,inventory,host);this.host=host;this.session=session;
        toolboxSlot=ToolboxAdapter.find(inventory.player);
        if(toolboxSlot>=0) {toolboxStack=inventory.getItem(toolboxSlot);lockPlayerInventorySlot(toolboxSlot);}
        for(int i=0;i<16;i++)temp.add(new Ae2StorageTarget(i+1,host,i,null));
    }
    public UUID session() {return session;}
    public TerminalTab activeTab() {return tab;}
    public String feedback() {return feedback;}
    public CompoundTag data(String channel) {return clientData.getOrDefault(channel,new CompoundTag());}
    @Override public boolean stillValid(Player player) {return !closed && super.stillValid(player) && host.canUseTerminal(player);}
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if(isClientSide() || !(getPlayer() instanceof ServerPlayer player) || closed)return;
        tick++;
        if(!stillValid(player)){player.closeContainer();return;}
        int interval=AWConfigs.SERVER.cellterminal.network.minRefreshIntervalTicks.get();
        if(tick-lastRefresh>=interval || (force && tick-lastRefresh>=1)) {
            force=false;lastRefresh=tick;
            try {refresh(player);}catch(IOException|IllegalArgumentException error){
                Aedialsworks.LOGGER.warn("Cell terminal snapshot rejected: {}",error.toString());feedback(player,"sync_too_large");
            }
        }
    }
    private void refresh(ServerPlayer player) throws IOException {
        networks.refresh(host.terminalGrid());
        IGrid next=selectedNetwork==0?host.terminalGrid():networks.resolve(selectedNetwork);
        if(next==null){selectedNetwork=0;next=host.terminalGrid();}
        if(next==null)return;
        if(next!=selectedGrid){selectedGrid=next;delta.reset();lastBus=-200;}
        var visible=new LinkedHashSet<IGrid>();visible.add(next);
        if(!subnetVisibility.equals("DONT_SHOW"))networks.grids().forEach((id,grid)->{if(subnetVisibility.equals("SHOW_ALL") || favorites.contains(id))visible.add(grid);});
        targets.clear();for(var target:scanner.scanAll(visible))targets.put(target.id(),target);
        for(var target:temp)targets.put(target.id(),target);
        if(!tab.enabled())tab=TerminalTab.SUBNETS;
        var meta=new CompoundTag();meta.putLong("networkId",networks.id(next));meta.putLong("rootNetworkId",networks.id(host.terminalGrid()));meta.putInt("tab",tab.ordinal());
        for(var value:TerminalTab.values())meta.putBoolean("tab"+value.ordinal(),value.enabled());
        var cfg=AWConfigs.SERVER.cellterminal;
        meta.putBoolean("eject",cfg.cellOperations.cellEjectEnabled.get());meta.putBoolean("insert",cfg.cellOperations.cellInsertEnabled.get());
        meta.putBoolean("swap",cfg.cellOperations.cellSwapEnabled.get());meta.putBoolean("partition",cfg.misc.partitionEditEnabled.get());
        meta.putBoolean("priority",cfg.misc.priorityEditEnabled.get());meta.putBoolean("upgradeInsert",cfg.misc.upgradeInsertEnabled.get());meta.putBoolean("upgradeExtract",cfg.misc.upgradeExtractEnabled.get());
        meta.putBoolean("toolbox",toolboxSlot>=0);var cards=new ListTag();
        if(toolboxSlot>=0 && player.getInventory().getItem(toolboxSlot)==toolboxStack){
            var inventory=ToolboxAdapter.inventory(player,toolboxSlot);
            for(int slot=0;slot<inventory.size();slot++)cards.add(inventory.getStackInSlot(slot).save(new CompoundTag()));
        }
        meta.put("toolboxCards",cards);send(player,TerminalChannels.META,meta);
        var storages=new ListTag();var buses=new ListTag();var temporary=new ListTag();
        boolean pollBus=tab.bus() || tab==TerminalTab.NETWORK_TOOLS || lastBus<0 || (cfg.polling.storageBusPollingEnabled.get() && tick-lastBus>=cfg.polling.pollingInterval.get());
        for(var target:targets.values()) {
            if(target.kind().equals("bus")){if(pollBus)buses.add(target.snapshot(true));}
            else if(target.kind().equals("temp"))temporary.add(target.snapshot(true));
            else storages.add(target.snapshot(true));
        }
        if(tab.bus() && pollBus){send(player,TerminalChannels.BUSES,list(buses));lastBus=tick;}
        send(player,TerminalChannels.STORAGES,list(storages));send(player,TerminalChannels.TEMP_CELLS,list(temporary));
        if(!tab.bus() && pollBus){send(player,TerminalChannels.BUSES,list(buses));lastBus=tick;}
        send(player,TerminalChannels.SUBNETS,networks.snapshot());
    }
    private static CompoundTag list(ListTag entries){var tag=new CompoundTag();tag.put("entries",entries);return tag;}
    private void send(ServerPlayer player,String channel,CompoundTag tag)throws IOException {
        var update=delta.prepare(channel,tag,AWConfigs.SERVER.cellterminal.network.enableDeltaUpdates.get());if(update==null)return;
        var chunks=ChunkCodec.encode(update.payload(),AWConfigs.SERVER.cellterminal.network.maxChunkBytes.get());
        long revision=revisions.merge(channel,1L,Long::sum);
        for(int i=0;i<chunks.size();i++)AWNetwork.sendToPlayer(player,new PacketNBTChunk(containerId,session,channel,revision,update.full(),i,chunks.size(),chunks.get(i)));
        delta.commit(channel,update);
    }
    public void acceptChunk(PacketNBTChunk packet) {
        if(!isClientSide() || !session.equals(packet.menuSession()) || packet.containerId()!=containerId)return;
        try {
            assembler.accept(packet,System.currentTimeMillis()).ifPresent(payload->{
                var prior=clientData.get(payload.channel());
                var result=DeltaSnapshot.apply(prior,payload.tag(),payload.full());
                if(payload.channel().equals(TerminalChannels.META)) {
                    if(prior!=null && prior.getLong("networkId")!=result.getLong("networkId"))clientData.clear();
                    int selected=result.getInt("tab");if(selected>=0 && selected<TerminalTab.values().length)tab=TerminalTab.values()[selected];
                }
                clientData.put(payload.channel(),result);
            });
        }catch(IOException|RuntimeException error){feedback="sync_error";clientData.clear();assembler.close();request("refresh",new CompoundTag());}
    }
    public void request(String action,CompoundTag payload) {
        if(!isClientSide())return;var data=payload.copy();data.putUUID("session",session);
        AWNetwork.sendToServer(new PacketMenuAction(containerId,AWIds.id("cellterminal/"+action),data));
    }
    @Override public void receiveServerData(ResourceLocation channel,CompoundTag payload) {
        if(channel.equals(AWIds.id("cellterminal/feedback")) && payload.hasUUID("session") && payload.getUUID("session").equals(session))feedback=payload.getString("message");
    }
    private void feedback(ServerPlayer player,String key) {
        var data=new CompoundTag();data.putString("message",key);data.putUUID("session",session);
        AWNetwork.sendToPlayer(player,new PacketMenuData(containerId,AWIds.id("cellterminal/feedback"),data));
    }
    @Override public void receiveClientAction(ServerPlayer player,ResourceLocation action,CompoundTag payload) {
        if(!action.getNamespace().equals(Aedialsworks.MODID) || !action.getPath().startsWith("cellterminal/")
            || !payload.hasUUID("session") || !session.equals(payload.getUUID("session")) || !stillValid(player) || player.isSpectator())return;
        long now=player.level().getGameTime();if(actionTick!=now){actionTick=now;actions=0;}if(++actions>8)return;
        String name=action.getPath().substring("cellterminal/".length());var cfg=AWConfigs.SERVER.cellterminal;
        if(name.equals("tab")){int n=payload.getInt("tab");if(n>=0 && n<TerminalTab.values().length && TerminalTab.values()[n].enabled()){tab=TerminalTab.values()[n];force=true;}return;}
        if(name.equals("refresh")){if(tick-lastManualRefresh>=cfg.network.minRefreshIntervalTicks.get()){lastManualRefresh=tick;delta.reset();force=true;lastBus=-200;}return;}
        if(name.equals("view")) {
            String visibility=payload.getString("visibility");var ids=payload.getLongArray("favorites");
            if(!Set.of("DONT_SHOW","SHOW_FAVORITES","SHOW_ALL").contains(visibility) || ids.length>SubnetDirectory.MAX_NETWORKS)return;
            subnetVisibility=visibility;favorites.clear();for(long id:ids)favorites.add(id);delta.reset();lastBus=-200;force=true;return;
        }
        if(name.equals("network")) {
            networks.refresh(host.terminalGrid());long id=payload.getLong("network");
            if(id==0 || networks.resolve(id)!=null){selectedNetwork=id;force=true;}return;
        }
        if(!player.mayBuild()){feedback(player,"denied");return;}
        networks.refresh(host.terminalGrid());
        if(selectedGrid==null || networks.resolve(networks.id(selectedGrid))!=selectedGrid){feedback(player,"stale");return;}
        if(name.equals("mass_partition") || name.equals("attribute_unique")) {
            if(!cfg.tabs.networkToolsTabEnabled.get() || !cfg.misc.partitionEditEnabled.get() || !payload.getBoolean("confirmed")){feedback(player,"denied");return;}
            var selected=new ArrayList<AbstractTerminalTarget>();var ids=payload.getLongArray("ids");
            var tokens=payload.getLongArray("tokens");if(ids.length==0 || ids.length>256 || tokens.length!=ids.length)return;var unique=new HashSet<Long>();
            for(int i=0;i<ids.length;i++){long id=ids[i];var target=targets.get(id);if(!unique.add(id) || target==null || !target.valid(player) || !reachable(target) || !target.matches(tokens[i]) || target.kind().equals("temp")){feedback(player,"stale");return;}selected.add(target);}
            boolean ok=true;
            if(name.equals("attribute_unique"))ok=NetworkToolOperations.attributeUnique(selected,getActionSource());else NetworkToolOperations.massPartition(selected);
            feedback(player,ok?"done":"insufficient_capacity");force=true;lastBus=-200;return;
        }
        var target=targets.get(payload.getLong("id"));
        if(target==null || !target.valid(player) || !reachable(target) || !target.matches(payload.getLong("token"))){feedback(player,"stale");force=true;return;}
        if(target.kind().equals("temp") && !cfg.tabs.tempAreaTabEnabled.get()){feedback(player,"denied");return;}
        boolean bus=target.kind().equals("bus");
        switch(name) {
            case "pickup" -> {
                if(bus || !cfg.tabs.terminalTabEnabled.get() && !target.kind().equals("temp"))break;
                var held=getCarried();target.persist();var old=target.cell();
                if(held.isEmpty() && !old.isEmpty() && cfg.cellOperations.cellEjectEnabled.get()){target.replaceCell(ItemStack.EMPTY);setCarried(old);}
                else if(!held.isEmpty() && old.isEmpty() && cfg.cellOperations.cellInsertEnabled.get() && StorageCells.isCellHandled(held)) {
                    var remaining=held.copy();target.replaceCell(remaining.split(1));setCarried(remaining);
                }else if(!old.isEmpty() && held.getCount()==1 && cfg.cellOperations.cellSwapEnabled.get() && StorageCells.isCellHandled(held)) {
                    target.replaceCell(held);setCarried(old);
                }else feedback(player,"denied");
            }
            case "priority" -> {if(cfg.misc.priorityEditEnabled.get())target.setPriority(payload.getInt("priority"));else feedback(player,"denied");}
            case "partition", "clear_partition", "partition_contents", "fuzzy" -> {
                if(!cfg.misc.partitionEditEnabled.get() || !(bus?cfg.tabs.storageBusPartitionTabEnabled.get():cfg.tabs.partitionTabEnabled.get())){feedback(player,"denied");break;}
                switch(name){
                    case "clear_partition" -> target.clearPartition();case "partition_contents" -> NetworkToolOperations.massPartition(List.of(target));case "fuzzy" -> target.cycleFuzzyMode();
                    default -> {
                        AEKey key=null;
                        if(!payload.getBoolean("clear")) {
                            if(payload.contains("contentIndex")) {
                                var contents=AbstractTerminalTarget.contents(target.storage());int index=payload.getInt("contentIndex");
                                if(index<0 || index>=contents.size())break;var stack=GenericStack.readTag(contents.getCompound(index));if(stack==null)break;key=stack.what();
                            }else {
                                var stack=GenericStack.fromItemStack(getCarried());if(stack==null)break;key=stack.what();
                                var contained=appeng.api.behaviors.ContainerItemStrategies.getContainedStack(getCarried());
                                if(contained!=null && (payload.getBoolean("fluid") || target.cell().getItem() instanceof appeng.api.storage.cells.IBasicCellItem cell && cell.getKeyType()==AEKeyType.fluids()))key=contained.what();
                            }
                        }
                        target.setPartition(payload.getInt("slot"),key);
                    }
                }
            }
            case "upgrade_insert" -> {
                if(!cfg.misc.upgradeInsertEnabled.get()){feedback(player,"denied");break;}
                int toolSlot=payload.contains("toolSlot")?payload.getInt("toolSlot"):-1;
                if(toolSlot>=0 && toolboxSlot>=0 && player.getInventory().getItem(toolboxSlot)==toolboxStack) {
                    var inv=ToolboxAdapter.inventory(player,toolboxSlot);if(toolSlot>=inv.size())break;
                    var stack=inv.getStackInSlot(toolSlot);if(stack.isEmpty())break;
                    var one=stack.copyWithCount(1);var remainder=target.insertUpgrade(one);
                    if(remainder.isEmpty())inv.extractItem(toolSlot,1,false);
                }else setCarried(target.insertUpgrade(getCarried()));
            }
            case "upgrade_extract" -> {
                if(!cfg.misc.upgradeExtractEnabled.get() || !getCarried().isEmpty()){feedback(player,"denied");break;}
                setCarried(target.extractUpgrade(payload.getInt("slot")));
            }
            case "highlight" -> {
                var location=new CompoundTag();location.putUUID("session",session);location.putLong("pos",target.position().asLong());location.putString("dimension",target.dimension());
                AWNetwork.sendToPlayer(player,new PacketMenuData(containerId,AWIds.id("cellterminal/highlight"),location));
            }
            default -> {return;}
        }
        force=true;lastBus=-200;host.saveTerminal();
    }
    private boolean reachable(AbstractTerminalTarget target){return target.grid()==null || networks.resolve(networks.id(target.grid()))==target.grid();}
    @Override public void clicked(int slot,int button,net.minecraft.world.inventory.ClickType type,Player player) {
        if(toolboxSlot>=0 && ((slot>=0 && slot<slots.size() && slots.get(slot).container==getPlayerInventory() && slots.get(slot).getContainerSlot()==toolboxSlot)
            || (type==net.minecraft.world.inventory.ClickType.SWAP && button==toolboxSlot)))return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(isPlayerInventorySlotLocked(slot.getContainerSlot()) || !slot.hasItem())return ItemStack.EMPTY;
        if(!AWConfigs.SERVER.cellterminal.tabs.tempAreaTabEnabled.get() || !AWConfigs.SERVER.cellterminal.cellOperations.cellInsertEnabled.get())return ItemStack.EMPTY;
        var original=slot.getItem().copy();if(!StorageCells.isCellHandled(original))return ItemStack.EMPTY;
        var remaining=host.temporaryCells().addItems(original.copy());if(remaining.getCount()==original.getCount())return ItemStack.EMPTY;
        slot.set(remaining);slot.setChanged();force=true;return original;
    }
    @Override public void removed(Player player) {
        if(!closed){closed=true;scanner.close();networks.close();temp.forEach(AbstractTerminalTarget::close);targets.clear();delta.reset();assembler.close();clientData.clear();}
        super.removed(player);
    }
}
