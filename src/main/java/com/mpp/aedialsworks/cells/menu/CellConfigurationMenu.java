package com.mpp.aedialsworks.cells.menu;
import java.util.*;
import appeng.menu.*;
import appeng.api.stacks.*;
import appeng.util.inv.AppEngInternalInventory;
import com.mpp.aedialsworks.cells.cell.*;
import com.mpp.aedialsworks.cellterminal.network.*;
import com.mpp.aedialsworks.common.network.*;
import com.mpp.aedialsworks.common.registry.*;
import com.mpp.aedialsworks.common.util.AWIds;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkHooks;

/** Item identity is locked while editing; ghosts use the same session-checked transport as world machines. */
public final class CellConfigurationMenu extends AEBaseMenu implements ServerMenuReceiver {
    public static final String CHANNEL="cells:item";
    public final UUID session;public final int inventorySlot;public final ItemStack cell;public final TieredCellItem item;
    public CompoundTag data=new CompoundTag();public int page;
    private final AppEngInternalInventory icons=new AppEngInternalInventory(36);
    private final ChunkAssembler assembler=new ChunkAssembler();private CompoundTag last;private long revision,actionTick=-1;private int actions;
    public CellConfigurationMenu(int id,Inventory inv,int slot,UUID session){this(id,inv,slot,session,-1);}
    private CellConfigurationMenu(int id,Inventory inv,int slot,UUID session,int upgradeSlots){super(AWMenus.CELL_CONFIGURATION.get(),id,inv,null);this.session=session;inventorySlot=slot;cell=inv.getItem(slot);if(!(cell.getItem() instanceof TieredCellItem c))throw new IllegalArgumentException("Missing cell");item=c;lockPlayerInventorySlot(slot);
        for(int i=0;i<36;i++){var ghost=new appeng.menu.slot.FakeSlot(icons,i);ghost.setHideAmount(true);addSlot(ghost,CellsSlots.FILTER[i]);}
        if(upgradeSlots>16||upgradeSlots< -1)throw new IllegalArgumentException("Invalid cell upgrade slot count");
        var upgrades=inv.player.level().isClientSide&&upgradeSlots>=0?appeng.api.upgrades.UpgradeInventories.forMachine(item,upgradeSlots,()->{}):item.getUpgrades(cell);for(int i=0;i<upgrades.size();i++)addSlot(new SlotItemHandler(upgrades.toItemHandler(),i,186+(i/8)*18,25+(i%8)*18),CellsSlots.UPGRADE[i]);
        for(int i=0;i<36;i++)addSlot(new Slot(inv,i,8+18*(i%9),i<9?232:174+18*(i/9-1)){@Override public boolean mayPickup(Player p){return getContainerSlot()!=inventorySlot&&super.mayPickup(p);}@Override public boolean mayPlace(ItemStack s){return getContainerSlot()!=inventorySlot&&super.mayPlace(s);}},i<9?SlotSemantics.PLAYER_HOTBAR:SlotSemantics.PLAYER_INVENTORY);
    }
    @Override public boolean stillValid(Player p){return inventorySlot>=0&&inventorySlot<36&&(p.level().isClientSide?p.getInventory().getItem(inventorySlot).is(item):p.getInventory().getItem(inventorySlot)==cell)&&cell.getCount()==1;}
    private boolean authorized(Player p){return p==getPlayer()&&p.containerMenu==this&&p.mayBuild()&&stillValid(p);}
    public int filterIndex(Slot s){return Arrays.asList(CellsSlots.FILTER).indexOf(getSlotSemantic(s));}
    private boolean ghost(int slot){return slot>=0&&slot<slots.size()&&filterIndex(slots.get(slot))>=0;}
    private boolean locked(int index){return index>=0&&index<slots.size()&&slots.get(index).container==getPlayerInventory()&&slots.get(index).getContainerSlot()==inventorySlot;}
    @Override public ItemStack quickMoveStack(Player p,int slot){return locked(slot)?ItemStack.EMPTY:super.quickMoveStack(p,slot);}
    @Override public void clicked(int slot,int button,ClickType type,Player p){if(!ghost(slot)&&!locked(slot)&&!(type==ClickType.SWAP&&button==inventorySlot)&&authorized(p))super.clicked(slot,button,type,p);}
    @Override public void setFilter(int slot,ItemStack stack){if(!ghost(slot)&&!locked(slot)&&authorized(getPlayer()))super.setFilter(slot,stack);}
    @Override public void doAction(ServerPlayer p,appeng.helpers.InventoryAction a,int slot,long id){if(!ghost(slot)&&!locked(slot)&&authorized(p))super.doAction(p,a,slot,id);}
    @Override public void receiveClientAction(ServerPlayer p,ResourceLocation action,CompoundTag n){
        if(!authorized(p)||!action.getNamespace().equals("aedialsworks")||!n.hasUUID("session")||!n.getUUID("session").equals(session))return;
        long tick=p.level().getGameTime();if(tick!=actionTick){actionTick=tick;actions=0;}if(++actions>8)return;
        switch(action.getPath()){
            case "cell_view"->page=Math.max(0,Math.min(item.getConfigInventory(cell).size()>36?1:0,n.getInt("page")));
            case "cell_filter"->{int slot=n.getInt("slot");var filters=item.getConfigInventory(cell);if(slot<page*36||slot>=page*36+36||slot>=filters.size())return;var key=n.contains("key")?AEKey.fromTagGeneric(n.getCompound("key")):null;if(key!=null&&!item.keyType(cell).contains(key))return;filters.setStack(slot,key==null?null:new GenericStack(key,1));}
            case "cell_tag"->{String tag=n.getString("tag");if(tag.length()>128||!tag.isEmpty()&&ResourceLocation.tryParse(tag)==null)return;cell.getOrCreateTag().putString("filterTag",tag);}
            case "cell_channel"->{if(item.family!=CellFamily.CONFIGURABLE||item.storageFamily(cell)!=CellFamily.CONFIGURABLE||CellHandler.open(cell,null).storedUnits()!=0)return;boolean fluid=item.keyType(cell)==AEKeyType.fluids();cell.getOrCreateTag().putBoolean("fluidChannel",!fluid);item.getConfigInventory(cell).clear();}
            case "cell_fuzzy"->{var modes=appeng.api.config.FuzzyMode.values();item.setFuzzyMode(cell,modes[(item.getFuzzyMode(cell).ordinal()+1)%modes.length]);}
            default->{return;}
        }last=null;p.getInventory().setChanged();
    }
    public void action(String action,CompoundTag n){n.putUUID("session",session);AWNetwork.sendToServer(new PacketMenuAction(containerId,AWIds.id("cell_"+action),n));}
    private CompoundTag snapshot(){var n=new CompoundTag();n.putInt("page",page);n.putBoolean("fluid",item.keyType(cell)==AEKeyType.fluids());var filters=item.getConfigInventory(cell);n.putInt("pages",filters.size()>36?2:1);n.putString("tag",cell.getOrCreateTag().getString("filterTag"));n.putString("fuzzy",item.getFuzzyMode(cell).name());var rows=new ListTag();for(int i=page*36;i<Math.min(filters.size(),page*36+36);i++){var row=new CompoundTag();var k=filters.getKey(i);if(k!=null)row.put("filter",k.toTagGeneric());rows.add(row);}n.put("rows",rows);var state=CellHandler.open(cell,null);n.putLong("used",state.usedBytes());n.putLong("capacity",state.totalBytes());n.putInt("types",state.storedTypes());return n;}
    private void refresh(){var rows=data.getList("rows",Tag.TAG_COMPOUND);for(int i=0;i<36;i++){var row=rows.getCompound(i);var k=row.contains("filter")?AEKey.fromTagGeneric(row.getCompound("filter")):null;icons.setItemDirect(i,k==null?ItemStack.EMPTY:GenericStack.wrapInItemStack(k,1));}}
    @Override public void broadcastChanges(){if(getPlayer() instanceof ServerPlayer p&&stillValid(p)){var next=snapshot();data=next;refresh();if(!next.equals(last)){try{var chunks=ChunkCodec.encode(next,32768);long rev=++revision;for(int i=0;i<chunks.size();i++)AWNetwork.sendToPlayer(p,new PacketNBTChunk(containerId,session,CHANNEL,rev,true,i,chunks.size(),chunks.get(i)));last=next;}catch(java.io.IOException ex){p.closeContainer();}}}super.broadcastChanges();}
    public void acceptChunk(PacketNBTChunk p){if(p.containerId()!=containerId||!p.menuSession().equals(session)||!p.channel().equals(CHANNEL))return;try{assembler.accept(p,System.currentTimeMillis()).ifPresent(n->{data=n.tag();page=data.getInt("page");refresh();});}catch(java.io.IOException ex){assembler.close();}}
    @Override public void removed(Player p){super.removed(p);assembler.close();}
    public static void open(ServerPlayer p){if(!(p.getMainHandItem().getItem() instanceof TieredCellItem item)||p.getMainHandItem().getCount()!=1)return;int upgradeSlots=item.getUpgrades(p.getMainHandItem()).size();p.inventoryMenu.broadcastChanges();int slot=p.getInventory().selected;UUID nonce=UUID.randomUUID();NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,who)->new CellConfigurationMenu(id,inv,slot,nonce),p.getMainHandItem().getHoverName()),b->{b.writeVarInt(slot);b.writeUUID(nonce);b.writeVarInt(upgradeSlots);});}
    public static CellConfigurationMenu fromNetwork(int id,Inventory inv,FriendlyByteBuf b){return new CellConfigurationMenu(id,inv,b.readVarInt(),b.readUUID(),b.readVarInt());}
}
