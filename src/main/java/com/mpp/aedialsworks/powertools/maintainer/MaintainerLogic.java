package com.mpp.aedialsworks.powertools.maintainer;
import java.util.concurrent.*;
import com.google.common.collect.ImmutableSet;
import appeng.api.config.Actionable;
import appeng.api.networking.*;
import appeng.api.networking.crafting.*;
import appeng.api.stacks.AEKey;
import com.mpp.aedialsworks.powertools.*;
import com.mpp.aedialsworks.powertools.monitor.ResourceEntry;
import com.mpp.aedialsworks.common.config.AWConfigs;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
public final class MaintainerLogic extends AbstractPowerLogic implements ICraftingRequester {
    public static final int CAPACITY=24;
    public final ResourceEntry[] entries=new ResourceEntry[CAPACITY];
    private final MaintainerTask[] tasks=new MaintainerTask[CAPACITY];
    public final long[] batch=new long[CAPACITY];
    public final int[] interval=new int[CAPACITY];
    private IGrid calculationGrid;
    public MaintainerLogic(PowerHost host) {
        super(host);host.powerNode().addService(ICraftingRequester.class,this);
        for(int i=0;i<CAPACITY;i++) {entries[i]=new ResourceEntry();tasks[i]=new MaintainerTask();batch[i]=64;interval[i]=20;}
    }
    @Override protected void tick(int elapsed) {
        var grid=grid(); if(grid==null) return;
        if(calculationGrid!=grid) { for(var t:tasks)t.unload();calculationGrid=grid; }
        long now=host.powerLevel().getGameTime(); int calculating=0;
        for(var t:tasks) if(t.future!=null) calculating++;
        var service=grid.getCraftingService();var stock=grid.getStorageService().getCachedInventory();
        for(int i=0;i<CAPACITY;i++) {
            var e=entries[i];var t=tasks[i];e.quantity=e.key==null?0:stock.get(e.key);
            if(t.link!=null) { if(t.link.isDone()||t.link.isCanceled()){t.link=null;t.state=AbstractCraftingTask.State.IDLE;changed();} else continue; }
            if(t.future!=null && t.future.isDone()) {
                try { t.plan=t.future.get(); t.state=AbstractCraftingTask.State.WAITING_CPU; }
                catch(InterruptedException ex){Thread.currentThread().interrupt();t.unload();return;}
                catch(ExecutionException|CancellationException ex){t.state=AbstractCraftingTask.State.MISSING_RESOURCES;t.nextAttempt=now+100;}
                finally {t.future=null;calculating--;}
            }
            if(t.plan!=null && now>=t.nextAttempt) {
                if(t.plan.simulation()) {t.plan=null;t.state=AbstractCraftingTask.State.MISSING_RESOURCES;t.nextAttempt=now+100;}
                else {
                    var result=service.submitJob(t.plan,this,null,false,source);
                    if(result.successful()) {t.link=result.link();t.plan=null;t.retries=0;t.state=AbstractCraftingTask.State.CRAFTING;changed();}
                    else {t.nextAttempt=now+20;if(++t.retries>=AWConfigs.SERVER.powertools.maintainer.maxCpuRetryCount.get()){t.plan=null;t.retries=0;t.nextAttempt=now+100;}}
                }
            }
            if(!e.enabled||e.key==null||e.quantity>=e.threshold||t.busy()||now<t.nextAttempt) continue;
            if(calculating>=AWConfigs.SERVER.powertools.maintainer.maxConcurrentCalculations.get()) continue;
            if(!service.isCraftable(e.key)) {t.state=AbstractCraftingTask.State.NOT_CRAFTABLE;t.nextAttempt=now+100;continue;}
            long needed=Math.min(batch[i],e.threshold-e.quantity);
            t.future=service.beginCraftingCalculation(host.powerLevel(),()->source,e.key,needed,CalculationStrategy.CRAFT_LESS);
            t.state=AbstractCraftingTask.State.CALCULATING;t.nextAttempt=now+interval[i];calculating++;
        }
    }
    @Override public IGridNode getActionableNode(){return host.getActionableNode();}
    @Override public ImmutableSet<ICraftingLink> getRequestedJobs(){var b=ImmutableSet.<ICraftingLink>builder();for(var t:tasks)if(t.link!=null)b.add(t.link);return b.build();}
    @Override public long insertCraftedItems(ICraftingLink link,AEKey key,long amount,Actionable mode){
        if(!getRequestedJobs().contains(link)||grid()==null||!host.powerActive())return 0;
        return grid().getStorageService().getInventory().insert(key,amount,mode,source);
    }
    @Override public void jobStateChange(ICraftingLink link){for(var t:tasks)if(t.link==link){t.link=null;t.state=AbstractCraftingTask.State.IDLE;changed();}}
    @Override public void unload(){super.unload();for(var t:tasks)t.unload();calculationGrid=null;}
    @Override public void removed(){for(var t:tasks)t.cancel();super.removed();}
    @Override public void save(CompoundTag tag){
        var list=new ListTag();for(int i=0;i<CAPACITY;i++){var n=entries[i].save();n.putLong("batch",batch[i]);n.putInt("interval",interval[i]);tasks[i].save(n);list.add(n);}tag.put("entries",list);
    }
    @Override public void load(CompoundTag tag){var list=tag.getList("entries",Tag.TAG_COMPOUND);for(int i=0;i<CAPACITY;i++){
        var n=i<list.size()?list.getCompound(i):new CompoundTag();entries[i]=ResourceEntry.load(n);batch[i]=Math.max(1,n.getLong("batch"));interval[i]=Math.max(20,Math.min(72000,n.getInt("interval")));tasks[i].load(n,this);
    }}
    @Override public boolean configure(ServerPlayer player,String action,CompoundTag value){
        int slot=value.getInt("slot");if(slot<0||slot>=CAPACITY)return false;
        if(action.equals("entry")){tasks[slot].cancel();entries[slot]=ResourceEntry.load(value);batch[slot]=Math.max(1,value.getLong("batch"));interval[slot]=Math.max(20,Math.min(72000,value.getInt("interval")));changed();return true;}
        if(action.equals("run")){tasks[slot].nextAttempt=0;return true;}
        if(action.equals("cancel")){tasks[slot].cancel();changed();return true;}return false;
    }
}
