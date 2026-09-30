package com.mpp.aedialsworks.powertools.crafter;
import java.util.*;
import appeng.api.config.*;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.*;
import appeng.crafting.pattern.AECraftingPattern;
import com.mpp.aedialsworks.powertools.*;
import com.mpp.aedialsworks.powertools.items.CrafterSpeedUpgrade;
import com.mpp.aedialsworks.common.config.AWConfigs;
import com.mpp.aedialsworks.cells.cell.CellMath;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.items.ItemStackHandler;
/** Twelve real pattern slots, fair scheduling, and bounded work per server tick. */
public final class CrafterLogic extends AbstractPowerLogic {
    public static final int CAPACITY=12;
    public static final int MIN_SPEED_TICKS=20;
    public final ItemStackHandler patterns=new ItemStackHandler(CAPACITY){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return PatternDetailsHelper.isEncodedPattern(stack);}
        @Override protected void onContentsChanged(int slot){credit[slot]=0;changed();}
    };
    public final ItemStackHandler upgrades=new ItemStackHandler(4){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return stack.getItem() instanceof CrafterSpeedUpgrade;}
        @Override protected void onContentsChanged(int slot){changed();}
    };
    private final LinkedHashMap<AEItemKey,Long> pending=new LinkedHashMap<>();
    public final boolean[] enabled=new boolean[CAPACITY];
    public final long[] target=new long[CAPACITY];
    public final String[] status=new String[CAPACITY];
    private final long[] credit=new long[CAPACITY];
    public int speedTicks=20,batch=1;
    private long clock;
    private int cursor;
    public CrafterLogic(PowerHost host){super(host);Arrays.fill(enabled,true);Arrays.fill(status,"IDLE");}
    public int multiplier(){int n=1;for(int i=0;i<4;i++)if(upgrades.getStackInSlot(i).getItem() instanceof CrafterSpeedUpgrade card)n=Math.max(n,card.multiplier());return n;}
    public long effectiveBatch(){return ResourceTransaction.saturatedMultiply(ResourceTransaction.saturatedMultiply(batch,AWConfigs.SERVER.powertools.crafter.baseCraftsPerOperation.get()),multiplier());}
    @Override protected void tick(int elapsed){
        if(grid()==null)return;flush();if(!pending.isEmpty())return;
        clock+=elapsed;long period=ResourceTransaction.saturatedMultiply(speedTicks,batch);
        if(clock>=period){clock%=period;for(int i=0;i<CAPACITY;i++)if(credit[i]==0)credit[i]=effectiveBatch();}
        int idle=0;
        for(int budget=0;budget<256 && idle<CAPACITY;budget++){
            int i=cursor;cursor=(cursor+1)%CAPACITY;
            if(!enabled[i]||credit[i]==0||patterns.getStackInSlot(i).isEmpty()){idle++;continue;}
            if(craft(i)){credit[i]--;idle=0;}else{credit[i]=0;idle++;}
            if(!pending.isEmpty())break;
        }
    }
    public boolean craft(int slot){
        if(grid()==null||!(host.powerLevel() instanceof ServerLevel level)||!host.powerActive())return false;
        var details=PatternDetailsHelper.decodePattern(patterns.getStackInSlot(slot),level);
        if(!(details instanceof AECraftingPattern pattern)){status[slot]="INVALID_PATTERN";return false;}
        var frame=new TransientCraftingContainer(new AbstractContainerMenu(null,-1){
            @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
            @Override public boolean stillValid(Player player){return false;}
        },3,3);
        var requested=new LinkedHashMap<AEItemKey,Long>();
        var inputs=pattern.getSparseInputs();
        for(int i=0;i<Math.min(9,inputs.length);i++)if(inputs[i]!=null){
            if(!(inputs[i].what() instanceof AEItemKey key)){status[slot]="ITEM_RECIPE_REQUIRED";return false;}
            frame.setItem(i,key.toStack(1));requested.merge(key,1L,Long::sum);
        }
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,frame,level).orElse(null);
        if(recipe==null){status[slot]="INVALID_RECIPE";return false;}
        ItemStack result;net.minecraft.core.NonNullList<ItemStack> remaining;
        var fake=FakePlayerFactory.getMinecraft(level);ForgeHooks.setCraftingPlayer(fake);
        try {result=recipe.assemble(frame,level.registryAccess());remaining=recipe.getRemainingItems(frame);}
        finally {ForgeHooks.setCraftingPlayer(null);}
        if(result.isEmpty()){status[slot]="EMPTY_RESULT";return false;}
        var resultKey=AEItemKey.of(result);long stored=grid().getStorageService().getCachedInventory().get(resultKey);
        if(target[slot]>0 && stored>=target[slot]){status[slot]="SATISFIED";return false;}
        var energy=grid().getEnergyService();double cost=1;
        if(energy.extractAEPower(cost,Actionable.SIMULATE,PowerMultiplier.CONFIG)<cost){status[slot]="NO_POWER";return false;}
        var storage=grid().getStorageService().getInventory();
        boolean extracted=ResourceTransaction.extract(requested,new ResourceTransaction.Port<AEItemKey>(){
            public long available(AEItemKey key,long n){return storage.extract(key,n,Actionable.SIMULATE,source);}
            public long extract(AEItemKey key,long n){return storage.extract(key,n,Actionable.MODULATE,source);}
        },this::buffer);
        if(!extracted){flush();status[slot]="MISSING_INPUT";return false;}
        energy.extractAEPower(cost,Actionable.MODULATE,PowerMultiplier.CONFIG);
        buffer(resultKey,result.getCount());for(var stack:remaining)if(!stack.isEmpty())buffer(AEItemKey.of(stack),stack.getCount());
        changed();flush();status[slot]=pending.isEmpty()?"CRAFTED":"OUTPUT_FULL";return true;
    }
    private void buffer(AEItemKey key,long amount){if(amount>0){pending.merge(key,amount,CellMath::add);changed();}}
    private void flush(){if(grid()==null)return;var it=pending.entrySet().iterator();while(it.hasNext()){
        var e=it.next();long n=grid().getStorageService().getInventory().insert(e.getKey(),e.getValue(),Actionable.MODULATE,source);
        if(n>0){long left=e.getValue()-n;if(left==0)it.remove();else e.setValue(left);changed();}
    }}
    public void drops(List<ItemStack> drops){
        for(var inv:List.of(patterns,upgrades))for(int i=0;i<inv.getSlots();i++)if(!inv.getStackInSlot(i).isEmpty())drops.add(inv.getStackInSlot(i).copy());
        for(var e:pending.entrySet()){long left=e.getValue();while(left>0){int n=(int)Math.min(left,e.getKey().getMaxStackSize());drops.add(e.getKey().toStack(n));left-=n;}}
    }
    public void clear(){for(var inv:List.of(patterns,upgrades))for(int i=0;i<inv.getSlots();i++)inv.setStackInSlot(i,ItemStack.EMPTY);pending.clear();}
    @Override public void save(CompoundTag tag){
        tag.put("patterns",patterns.serializeNBT());tag.put("upgrades",upgrades.serializeNBT());tag.putInt("speed",speedTicks);tag.putInt("batch",batch);tag.putLong("clock",clock);tag.putInt("cursor",cursor);
        var list=new ListTag();for(var e:pending.entrySet()){var n=new CompoundTag();n.put("key",e.getKey().toTagGeneric());n.putLong("amount",e.getValue());list.add(n);}tag.put("pending",list);
        var entries=new ListTag();for(int i=0;i<CAPACITY;i++){var n=new CompoundTag();n.putBoolean("enabled",enabled[i]);n.putLong("target",target[i]);n.putLong("credit",credit[i]);n.putString("state",status[i]);entries.add(n);}tag.put("entries",entries);
    }
    @Override public void load(CompoundTag tag){
        var savedPatterns=tag.getCompound("patterns").copy();savedPatterns.putInt("Size",CAPACITY);patterns.deserializeNBT(savedPatterns);
        var savedUpgrades=tag.getCompound("upgrades").copy();savedUpgrades.putInt("Size",4);upgrades.deserializeNBT(savedUpgrades);
        speedTicks=tag.contains("speed")?Math.max(MIN_SPEED_TICKS,Math.min(72000,tag.getInt("speed"))):MIN_SPEED_TICKS;batch=Math.max(1,Math.min(1000000,tag.getInt("batch")));clock=Math.max(0,tag.getLong("clock"));cursor=Math.floorMod(tag.getInt("cursor"),CAPACITY);
        pending.clear();for(var raw:tag.getList("pending",Tag.TAG_COMPOUND)){var n=(CompoundTag)raw;var key=AEKey.fromTagGeneric(n.getCompound("key"));if(key instanceof AEItemKey item && n.getLong("amount")>0)pending.put(item,n.getLong("amount"));}
        var entries=tag.getList("entries",Tag.TAG_COMPOUND);for(int i=0;i<CAPACITY;i++){var n=entries.getCompound(i);enabled[i]=!n.contains("enabled")||n.getBoolean("enabled");target[i]=Math.max(0,n.getLong("target"));credit[i]=Math.max(0,n.getLong("credit"));var s=n.getString("state");status[i]=s.isEmpty()?"IDLE":s;}
    }
    @Override public boolean configure(ServerPlayer player,String action,CompoundTag n){
        if(action.equals("settings")){speedTicks=Math.max(MIN_SPEED_TICKS,Math.min(72000,n.getInt("speed")));batch=Math.max(1,Math.min(1000000,n.getInt("batch")));changed();return true;}
        int i=n.getInt("slot");if(i<0||i>=CAPACITY)return false;
        if(action.equals("entry")){enabled[i]=n.getBoolean("enabled");target[i]=Math.max(0,n.getLong("threshold"));credit[i]=0;changed();return true;}return false;
    }
}
