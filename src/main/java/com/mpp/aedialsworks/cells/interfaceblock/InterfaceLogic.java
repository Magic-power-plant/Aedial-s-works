package com.mpp.aedialsworks.cells.interfaceblock;

import java.util.*;
import appeng.api.config.Actionable;
import appeng.api.stacks.*;
import appeng.api.storage.StorageHelper;
import com.mpp.aedialsworks.cells.*;
import com.mpp.aedialsworks.cells.api.CellsHost;
import com.mpp.aedialsworks.cells.cell.CellMath;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

public final class InterfaceLogic extends AbstractCellsLogic {
    public final List<ResourcePort> ports=new ArrayList<>();
    private LazyOptional<IItemHandler> itemCapability=LazyOptional.of(ItemView::new);
    private LazyOptional<IFluidHandler> fluidCapability=LazyOptional.of(FluidView::new);
    public long transferQuantity=64,keepQuantity;
    public int transferInterval=1;
    private long lastTransfer=-1;
    private final Map<AEKey,Long> pending=new LinkedHashMap<>();
    private void retain(AEKey key,long amount){if(amount>0){pending.merge(key,amount,Math::addExact);changed();}}
    public InterfaceLogic(CellsHost host){super(host);var kind=host.cellsKind();for(boolean output:new boolean[]{false,true})if(output?kind.output:kind.input){if(kind.items)ports.add(new ResourcePort(this,output,false));if(kind.fluids)ports.add(new ResourcePort(this,output,true));}}
    public void storageChanged(){changed();}
    public <T> LazyOptional<T> capability(Capability<T> cap){if(cap==ForgeCapabilities.ITEM_HANDLER&&host.cellsKind().items)return itemCapability.cast();if(cap==ForgeCapabilities.FLUID_HANDLER&&host.cellsKind().fluids)return fluidCapability.cast();return LazyOptional.empty();}
    @Override public void resume(){if(unloaded()){itemCapability=LazyOptional.of(ItemView::new);fluidCapability=LazyOptional.of(FluidView::new);}super.resume();}
    @Override public void unload(){super.unload();itemCapability.invalidate();fluidCapability.invalidate();}
    @Override public boolean tick(){
        if(!active()||grid()==null)return false;boolean worked=false;var storage=grid().getStorageService().getInventory();
        var retry=pending.entrySet().iterator();while(retry.hasNext()){var row=retry.next();long moved=StorageHelper.poweredInsert(grid().getEnergyService(),storage,row.getKey(),row.getValue(),source);if(moved>0){if(moved==row.getValue())retry.remove();else row.setValue(row.getValue()-moved);changed();worked=true;}}
        for(var port:ports)for(int i=0;i<ResourcePort.MAX_SLOTS;i++){
            var key=port.keys[i];long amount=port.amounts[i];
            boolean stale=port.output&&(i>=port.activeSlots()||port.filters[i]==null||key!=null&&!key.equals(port.filters[i]));
            if(key!=null&&amount>0&&(!port.output||stale)){
                long moved=StorageHelper.poweredInsert(grid().getEnergyService(),storage,key,amount,source);
                if(moved>0){port.extract(i,moved,false);worked=true;}
            }
            if(port.output&&i<port.activeSlots()&&port.filters[i]!=null&&(port.keys[i]==null||port.keys[i].equals(port.filters[i]))){
                key=port.filters[i];long room=Math.max(0,port.limit(i)-port.amounts[i]);
                if(room>0&&port.accepts(i,key)){long got=StorageHelper.poweredExtraction(grid().getEnergyService(),storage,key,room,source);if(got>0){port.insert(i,key,got,false,true);worked=true;}}
            }
        }
        long now=host.cellsLevel().getGameTime();if(now-lastTransfer>=transferInterval){lastTransfer=now;worked|=adjacentTransfers();}return worked;
    }
    private boolean adjacentTransfers(){
        if(!installed("pull_card")&&!installed("push_card"))return false;boolean work=false;
        for(var side:host.targetSides()){
            var pos=host.cellsPos().relative(side);if(!host.cellsLevel().hasChunkAt(pos))continue;var be=host.cellsLevel().getBlockEntity(pos);if(be==null||be==host.cellsLevel().getBlockEntity(host.cellsPos()))continue;
            for(var port:ports){if(!installed(port.output?"push_card":"pull_card")||!port.output&&!pending.isEmpty())continue;
                if(port.fluid){var handler=be.getCapability(ForgeCapabilities.FLUID_HANDLER,side.getOpposite()).orElse(null);if(handler==null)continue;
                    if(port.output){for(int i=0;i<ResourcePort.MAX_SLOTS;i++)if(port.keys[i] instanceof AEFluidKey key&&port.amounts[i]>keepQuantity){int amount=(int)Math.min(Integer.MAX_VALUE,Math.min(transferQuantity,port.amounts[i]-keepQuantity));int accepted=handler.fill(key.toStack(amount),IFluidHandler.FluidAction.SIMULATE);if(accepted>0){int actual=handler.fill(key.toStack(accepted),IFluidHandler.FluidAction.EXECUTE);port.extract(i,Math.max(0,Math.min(accepted,actual)),false);work|=actual>0;}}}
                    else {for(int i=0;i<handler.getTanks();i++){var available=handler.getFluidInTank(i);if(available.isEmpty()||available.getAmount()<=keepQuantity)continue;var key=AEFluidKey.of(available);int want=(int)Math.min(Integer.MAX_VALUE,Math.min(transferQuantity,available.getAmount()-keepQuantity));var probe=handler.drain(key.toStack(want),IFluidHandler.FluidAction.SIMULATE);int accept=(int)port.insert(key,probe.getAmount(),true,false);if(accept>0){var got=handler.drain(key.toStack(accept),IFluidHandler.FluidAction.EXECUTE);if(!got.isEmpty()){long stored=port.insert(AEFluidKey.of(got),got.getAmount(),false,false);retain(AEFluidKey.of(got),got.getAmount()-stored);work=true;}}}}
                }else {var handler=be.getCapability(ForgeCapabilities.ITEM_HANDLER,side.getOpposite()).orElse(null);if(handler==null)continue;
                    if(port.output){for(int i=0;i<ResourcePort.MAX_SLOTS;i++)if(port.keys[i] instanceof AEItemKey key&&port.amounts[i]>keepQuantity){int count=(int)Math.min(key.getMaxStackSize(),Math.min(transferQuantity,port.amounts[i]-keepQuantity));for(int j=0;j<handler.getSlots()&&count>0;j++){var rest=handler.insertItem(j,key.toStack(count),false);int moved=count-rest.getCount();port.extract(i,moved,false);count=rest.getCount();work|=moved>0;}}}
                    else {for(int i=0;i<handler.getSlots();i++){var available=handler.getStackInSlot(i);if(available.isEmpty()||available.getCount()<=keepQuantity)continue;int want=(int)Math.min(transferQuantity,available.getCount()-keepQuantity);var probe=handler.extractItem(i,want,true);if(probe.isEmpty())continue;var key=AEItemKey.of(probe);int accepted=(int)port.insert(key,probe.getCount(),true,false);if(accepted>0){var got=handler.extractItem(i,accepted,false);if(!got.isEmpty()){long stored=port.insert(AEItemKey.of(got),got.getCount(),false,false);retain(AEItemKey.of(got),got.getCount()-stored);work=true;}}}}
                }
            }
        }return work;
    }
    @Override protected void saveExtraSettings(CompoundTag n){var list=new ListTag();for(var port:ports)list.add(port.saveSettings());n.put("ports",list);n.putLong("transferQuantity",transferQuantity);n.putLong("keepQuantity",keepQuantity);n.putInt("transferInterval",transferInterval);}
    @Override protected void loadExtraSettings(CompoundTag n){var list=n.getList("ports",Tag.TAG_COMPOUND);for(int i=0;i<ports.size();i++)ports.get(i).loadSettings(list.getCompound(i));transferQuantity=n.contains("transferQuantity")?Math.max(1,n.getLong("transferQuantity")):64;keepQuantity=Math.max(0,n.getLong("keepQuantity"));transferInterval=Math.max(1,n.getInt("transferInterval"));}
    @Override protected void saveContents(CompoundTag n){var list=new ListTag();for(var port:ports)list.add(port.save());n.put("buffers",list);var queued=new ListTag();pending.forEach((key,amount)->{var row=new CompoundTag();row.put("key",key.toTagGeneric());row.putLong("amount",amount);queued.add(row);});n.put("pendingTransfers",queued);}
    @Override protected void loadContents(CompoundTag n){var list=n.getList("buffers",Tag.TAG_COMPOUND);for(int i=0;i<ports.size();i++)ports.get(i).load(list.getCompound(i));pending.clear();for(var raw:n.getList("pendingTransfers",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;var key=AEKey.fromTagGeneric(row.getCompound("key"));long amount=row.getLong("amount");if(key!=null&&amount>0)pending.put(key,amount);}}
    @Override public CompoundTag snapshot(int selected,int page){var n=super.snapshot(selected,page);n.remove("ports");n.putInt("portCount",ports.size());if(selected<0||selected>=ports.size())return n;var port=ports.get(selected);n.putBoolean("output",port.output);n.putBoolean("fluid",port.fluid);n.putLong("maxSlot",port.maxSlot);var rows=new ListTag();for(int i=page*36;i<Math.min(ResourcePort.MAX_SLOTS,page*36+36);i++){var row=new CompoundTag();row.putInt("slot",i);if(port.filters[i]!=null)row.put("filter",port.filters[i].toTagGeneric());if(port.keys[i]!=null)row.put("key",port.keys[i].toTagGeneric());row.putLong("amount",port.amounts[i]);row.putLong("limit",port.limit(i));rows.add(row);}n.put("rows",rows);return n;}
    @Override public boolean configure(Player p,String action,CompoundTag n){
        if(action.equals("port_filter")||action.equals("port_settings")){
            int selected=n.getInt("port");if(selected<0||selected>=ports.size())return false;var port=ports.get(selected);
            if(action.equals("port_filter")){int i=n.getInt("slot");if(i<0||i>=port.activeSlots())return false;var key=n.contains("key")?AEKey.fromTagGeneric(n.getCompound("key")):null;if(key!=null&&!port.channel(key))return false;port.filters[i]=key;port.limits[i]=Math.max(0,n.getLong("limit"));}
            else{port.maxSlot=Math.max(1,n.getLong("maxSlot"));transferQuantity=Math.max(1,n.getLong("transferQuantity"));keepQuantity=Math.max(0,n.getLong("keepQuantity"));transferInterval=Math.max(1,n.getInt("transferInterval"));}
            settingsChanged();return true;
        }return super.configure(p,action,n);
    }
    private List<ResourcePort> channelPorts(boolean fluid){return ports.stream().filter(p->p.fluid==fluid).toList();}
    private final class ItemView implements IItemHandler {
        private List<ResourcePort> list(){return channelPorts(false);}
        public int getSlots(){return list().size()*ResourcePort.MAX_SLOTS;}
        private ResourcePort port(int slot){return list().get(slot/ResourcePort.MAX_SLOTS);}
        public ItemStack getStackInSlot(int slot){if(slot<0||slot>=getSlots())return ItemStack.EMPTY;var p=port(slot);int i=slot%180;return p.keys[i] instanceof AEItemKey k?k.toStack((int)Math.min(Integer.MAX_VALUE,p.amounts[i])):ItemStack.EMPTY;}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){if(slot<0||slot>=getSlots()||stack.isEmpty()||unloaded())return stack;var p=port(slot);int i=slot%180;var key=AEItemKey.of(stack);long moved=p.insert(i,key,stack.getCount(),simulate,false);if(!p.output&&(installed("overflow_card")&&p.accepts(i,key)||installed("trash_unselected_card")&&!p.accepts(i,key)))moved=stack.getCount();var rest=stack.copy();rest.shrink((int)moved);return rest;}
        public ItemStack extractItem(int slot,int amount,boolean simulate){if(slot<0||slot>=getSlots()||unloaded())return ItemStack.EMPTY;var p=port(slot);int i=slot%180;if(!p.output||!(p.keys[i] instanceof AEItemKey key))return ItemStack.EMPTY;return key.toStack((int)p.extract(i,Math.min(amount,key.getMaxStackSize()),simulate));}
        public int getSlotLimit(int slot){return slot<0||slot>=getSlots()?0:(int)Math.min(Integer.MAX_VALUE,port(slot).limit(slot%180));}
        public boolean isItemValid(int slot,ItemStack stack){return slot>=0&&slot<getSlots()&&!port(slot).output&&!stack.isEmpty()&&port(slot).accepts(slot%180,AEItemKey.of(stack));}
    }
    private final class FluidView implements IFluidHandler {
        private List<ResourcePort> list(){return channelPorts(true);}
        public int getTanks(){return list().size()*180;}
        public FluidStack getFluidInTank(int tank){if(tank<0||tank>=getTanks())return FluidStack.EMPTY;var p=list().get(tank/180);return p.keys[tank%180] instanceof AEFluidKey key?key.toStack((int)Math.min(Integer.MAX_VALUE,p.amounts[tank%180])):FluidStack.EMPTY;}
        public int getTankCapacity(int tank){return tank<0||tank>=getTanks()?0:(int)Math.min(Integer.MAX_VALUE,list().get(tank/180).limit(tank%180));}
        public boolean isFluidValid(int tank,FluidStack stack){return tank>=0&&tank<getTanks()&&!stack.isEmpty()&&!list().get(tank/180).output&&list().get(tank/180).accepts(tank%180,AEFluidKey.of(stack));}
        public int fill(FluidStack resource,FluidAction action){if(resource.isEmpty()||unloaded())return 0;long rest=resource.getAmount();var key=AEFluidKey.of(resource);for(var p:list())if(!p.output){long moved=p.insert(key,rest,action.simulate(),false);rest-=moved;if(rest>0&&installed("overflow_card")){for(int i=0;i<p.activeSlots();i++)if(p.accepts(i,key)){rest=0;break;}}if(rest>0&&installed("trash_unselected_card")){boolean matches=false;for(int i=0;i<p.activeSlots();i++)matches|=p.accepts(i,key);if(!matches)rest=0;}}return resource.getAmount()-(int)rest;}
        public FluidStack drain(FluidStack resource,FluidAction action){if(resource.isEmpty()||unloaded())return FluidStack.EMPTY;var key=AEFluidKey.of(resource);long got=0;for(var p:list())if(p.output)for(int i=0;i<180&&got<resource.getAmount();i++)if(key.equals(p.keys[i]))got+=p.extract(i,resource.getAmount()-got,action.simulate());return key.toStack((int)got);}
        public FluidStack drain(int amount,FluidAction action){if(amount<=0||unloaded())return FluidStack.EMPTY;for(var p:list())if(p.output)for(int i=0;i<180;i++)if(p.keys[i] instanceof AEFluidKey key)return drain(key.toStack(amount),action);return FluidStack.EMPTY;}
    }
}
