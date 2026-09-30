package com.mpp.aedialsworks.cells.menu;

import java.util.*;
import appeng.menu.*;
import appeng.menu.locator.MenuLocators;
import appeng.api.stacks.*;
import appeng.util.inv.AppEngInternalInventory;
import com.mpp.aedialsworks.cells.api.CellsHost;
import com.mpp.aedialsworks.cells.interfaceblock.InterfaceLogic;
import com.mpp.aedialsworks.cellterminal.network.*;
import com.mpp.aedialsworks.common.network.*;
import com.mpp.aedialsworks.common.registry.AWMenus;
import com.mpp.aedialsworks.common.util.AWIds;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkHooks;

/** Native AE2 widgets with authoritative session/distance/identity/permission validation on every mutation. */
public final class CellsMenu extends AEBaseMenu implements ServerMenuReceiver {
    public static final String CHANNEL="cells:state";
    public final CellsHost host;
    public final UUID session;
    public CompoundTag data=new CompoundTag();
    public int page,port;
    private final AppEngInternalInventory filterIcons=new AppEngInternalInventory(36),bufferIcons=new AppEngInternalInventory(36);
    private final ChunkAssembler assembler=new ChunkAssembler();
    private CompoundTag last;
    private long revision,lastSent=-100,actionTick=-1;
    private int actions;
    public CellsMenu(int id,Inventory inv,CellsHost host,UUID session){this(id,inv,host,session,host.cellsLogic().upgrades.size());}
    private CellsMenu(int id,Inventory inv,CellsHost host,UUID session,int upgradeSlots){
        super(AWMenus.CELLS.get(),id,inv,host);this.host=host;this.session=session;
        for(int i=0;i<36;i++){
            var filter=new appeng.menu.slot.FakeSlot(filterIcons,i);filter.setHideAmount(true);addSlot(filter,CellsSlots.FILTER[i]);
            var buffer=new appeng.menu.slot.FakeSlot(bufferIcons,i);buffer.setHideAmount(true);addSlot(buffer,CellsSlots.BUFFER[i]);
        }
        if(upgradeSlots<0||upgradeSlots>CellsSlots.UPGRADE.length)throw new IllegalArgumentException("Invalid upgrade slot count");
        // A retained client part can predate a server configuration reload. Its inventory is not authoritative.
        var upgrades=inv.player.level().isClientSide?appeng.api.upgrades.UpgradeInventories.forMachine(host.cellsItem(),upgradeSlots,()->{}):host.cellsLogic().upgrades;for(int i=0;i<upgrades.size();i++)addSlot(new SlotItemHandler(upgrades.toItemHandler(),i,186+(i/8)*18,25+(i%8)*18),CellsSlots.UPGRADE[i]);
        for(int i=0;i<36;i++)addSlot(new Slot(inv,i,8+18*(i%9),i<9?232:174+18*(i/9-1)),i<9?SlotSemantics.PLAYER_HOTBAR:SlotSemantics.PLAYER_INVENTORY);
    }
    private boolean authorized(Player p){return p==getPlayer()&&p.containerMenu==this&&stillValid(p)&&p.mayBuild();}
    @Override public boolean stillValid(Player p){return super.stillValid(p)&&host.canUseCells(p);}
    public int filterIndex(Slot slot){return Arrays.asList(CellsSlots.FILTER).indexOf(getSlotSemantic(slot));}
    public int bufferIndex(Slot slot){return Arrays.asList(CellsSlots.BUFFER).indexOf(getSlotSemantic(slot));}
    private boolean ghost(int index){return index>=0&&index<slots.size()&&(filterIndex(slots.get(index))>=0||bufferIndex(slots.get(index))>=0);}
    @Override public void clicked(int slot,int button,ClickType type,Player p){if(!ghost(slot)&&authorized(p))super.clicked(slot,button,type,p);}
    @Override public void setFilter(int slot,ItemStack stack){if(!ghost(slot)&&authorized(getPlayer()))super.setFilter(slot,stack);}
    @Override public void doAction(ServerPlayer p,appeng.helpers.InventoryAction action,int slot,long id){if(!ghost(slot)&&authorized(p))super.doAction(p,action,slot,id);}
    @Override public void receiveClientAction(ServerPlayer p,ResourceLocation action,CompoundTag n){
        if(!authorized(p)||!action.getNamespace().equals("aedialsworks")||!n.hasUUID("session")||!session.equals(n.getUUID("session")))return;
        long tick=p.level().getGameTime();if(actionTick!=tick){actionTick=tick;actions=0;}if(++actions>8)return;
        String name=action.getPath();var logic=host.cellsLogic();
        if(name.equals("cells_view")){int ports=logic instanceof InterfaceLogic i?i.ports.size():1;port=Math.max(0,Math.min(ports-1,n.getInt("port")));int pages=logic instanceof InterfaceLogic?logic.pages():2;page=Math.max(0,Math.min(pages-1,n.getInt("page")));last=null;return;}
        if(name.equals("cells_buffer")){bufferAction(n);last=null;return;}
        if(!name.startsWith("cells_"))return;
        if(logic.configure(p,name.substring(6),n)){last=null;if(name.endsWith("settings"))AWNetwork.sendToPlayer(p,new com.mpp.aedialsworks.cells.network.PacketCellsFeedback(Component.translatable("gui.aedialsworks.cells.settings_saved")));}
    }
    private void bufferAction(CompoundTag n){
        if(!(host.cellsLogic() instanceof InterfaceLogic logic))return;int selected=n.getInt("port"),slot=n.getInt("slot");if(selected!=port||selected<0||selected>=logic.ports.size()||slot<page*36||slot>=page*36+36)return;
        var buffer=logic.ports.get(selected);var held=getCarried();
        if(buffer.fluid){
            if(held.getCount()!=1)return;var container=net.minecraftforge.fluids.FluidUtil.getFluidHandler(held.copy()).orElse(null);if(container==null)return;
            if(buffer.keys[slot] instanceof AEFluidKey key){int amount=(int)Math.min(Integer.MAX_VALUE,buffer.amounts[slot]);int accepted=container.fill(key.toStack(amount),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);if(accepted>0){buffer.extract(slot,accepted,false);setCarried(container.getContainer());return;}}
            if(!buffer.output){var available=container.drain(Integer.MAX_VALUE,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);if(!available.isEmpty()){var key=AEFluidKey.of(available);int accepted=(int)buffer.insert(slot,key,available.getAmount(),true,false);if(accepted==available.getAmount()){var drained=container.drain(available,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);if(drained.getAmount()==accepted&&key.equals(AEFluidKey.of(drained))){buffer.insert(slot,key,accepted,false,false);setCarried(container.getContainer());}}}}return;
        }
        if(buffer.keys[slot] instanceof AEItemKey key){
            if(!held.isEmpty()&&!key.matches(held))return;int room=key.getMaxStackSize()-(held.isEmpty()?0:held.getCount());int amount=(int)buffer.extract(slot,n.getBoolean("single")?Math.min(1,room):room,false);if(amount>0)setCarried(key.toStack((held.isEmpty()?0:held.getCount())+amount));
        }else if(!buffer.output&&!buffer.fluid&&!held.isEmpty()){
            long accepted=buffer.insert(slot,AEItemKey.of(held),held.getCount(),false,false);var rest=held.copy();rest.shrink((int)accepted);setCarried(rest);
        }
    }
    public void action(String name,CompoundTag n){n.putUUID("session",session);AWNetwork.sendToServer(new PacketMenuAction(containerId,AWIds.id("cells_"+name),n));}
    public void refreshIcons(){
        var rows=data.getList("rows",Tag.TAG_COMPOUND);var filters=data.getList("filters",Tag.TAG_COMPOUND);
        var inv=appeng.util.ConfigInventory.configTypes(63,()->{});inv.readFromTag(filters);
        for(int i=0;i<36;i++){
            var row=rows.getCompound(i);AEKey filter=null,key=null;
            if(data.contains("portCount")){filter=(row.contains("filter")?AEKey.fromTagGeneric(row.getCompound("filter")):null);key=(row.contains("key")?AEKey.fromTagGeneric(row.getCompound("key")):null);}
            else {int index=page*36+i;if(index<inv.size())filter=inv.getKey(index);}
            filterIcons.setItemDirect(i,filter==null?ItemStack.EMPTY:GenericStack.wrapInItemStack(filter,1));
            bufferIcons.setItemDirect(i,key==null?ItemStack.EMPTY:GenericStack.wrapInItemStack(key,row.getLong("amount")));
        }
    }
    @Override public void broadcastChanges(){
        if(!(getPlayer() instanceof ServerPlayer p)||!stillValid(p)){super.broadcastChanges();return;}
        long tick=p.level().getGameTime();if(last!=null&&tick-lastSent<5){super.broadcastChanges();return;}
        int pages=host.cellsLogic() instanceof InterfaceLogic?host.cellsLogic().pages():2;page=Math.min(page,pages-1);
        var next=host.cellsLogic().snapshot(port,page);next.putInt("page",page);next.putInt("port",port);next.putInt("pages",pages);data=next;refreshIcons();super.broadcastChanges();lastSent=tick;if(next.equals(last))return;
        try{var chunks=ChunkCodec.encode(next,32*1024);long rev=++revision;for(int i=0;i<chunks.size();i++)AWNetwork.sendToPlayer(p,new PacketNBTChunk(containerId,session,CHANNEL,rev,true,i,chunks.size(),chunks.get(i)));last=next;}catch(java.io.IOException ex){com.mpp.aedialsworks.Aedialsworks.LOGGER.error("CELLS menu sync failed",ex);p.closeContainer();}
    }
    public void acceptChunk(PacketNBTChunk packet){if(packet.containerId()!=containerId||!session.equals(packet.menuSession())||!CHANNEL.equals(packet.channel()))return;try{assembler.accept(packet,System.currentTimeMillis()).ifPresent(p->{data=p.tag();page=data.getInt("page");port=data.getInt("port");refreshIcons();});}catch(java.io.IOException ex){assembler.close();}}
    @Override public void removed(Player player){if(Boolean.getBoolean("aedialsworks.p3Smoke"))com.mpp.aedialsworks.Aedialsworks.LOGGER.info("P3 menu removed client={} valid={} host={} position={}",player.level().isClientSide,stillValid(player),host.cellsPos(),player.position());super.removed(player);assembler.close();}
    public static CellsMenu fromNetwork(int id,Inventory inv,FriendlyByteBuf b){var locator=MenuLocators.readFromPacket(b);var host=locator.locate(inv.player,CellsHost.class);if(host==null)throw new IllegalStateException("CELLS host missing");var menu=new CellsMenu(id,inv,host,b.readUUID(),b.readVarInt());menu.setLocator(locator);return menu;}
    public static void registerOpener(){MenuOpener.addOpener(AWMenus.CELLS.get(),(player,locator,returning)->{
        if(!(player instanceof ServerPlayer p))return false;var host=locator.locate(p,CellsHost.class);if(host==null||!host.canUseCells(p))return false;
        UUID session=UUID.randomUUID();NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,owner)->{var menu=new CellsMenu(id,inv,host,session);menu.setLocator(locator);return menu;},Component.translatable("item.aedialsworks."+host.cellsKind().id)),buf->{MenuLocators.writeToPacket(buf,locator);buf.writeUUID(session);buf.writeVarInt(host.cellsLogic().upgrades.size());});return true;
    });}
}
