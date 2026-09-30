package com.mpp.aedialsworks.cells.cell.compacting;

import java.math.BigInteger;
import java.util.List;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.*;
import appeng.api.storage.cells.ISaveProvider;
import com.mpp.aedialsworks.cells.cell.*;
import com.mpp.aedialsworks.cells.upgrades.CellUpgrades;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class CompactingCellState extends AbstractCellState {
    private CompressionChain chain;
    private long baseUnits;
    private int tiersUp=-1,tiersDown=-1;
    public CompactingCellState(ItemStack stack,ISaveProvider provider){
        super(stack,provider);var root=stack.getTag();var n=root==null?new CompoundTag():root.getCompound("awCell");
        baseUnits=Math.max(0,n.getLong("baseUnits"));
        tiersUp=n.contains("tiersUp")?n.getInt("tiersUp"):-1;tiersDown=n.contains("tiersDown")?n.getInt("tiersDown"):-1;
        if(n.contains("chain"))chain=CompressionChain.load(n.getCompound("chain"));
        if(chain==null)baseUnits=0;
        else{
            int up=CellUpgrades.value(upgrades,"compression_tier_card_",1),down=CellUpgrades.value(upgrades,"decompression_tier_card_",1);
            refreshChain(chain.keys().get(chain.main()),up!=tiersUp||down!=tiersDown);
        }
    }
    private CompressionChain discover(AEItemKey key){
        var server=ServerLifecycleHooks.getCurrentServer();if(server==null)return new CompressionChain(List.of(key),List.of(1L),0);
        return CompactingRecipes.discover(server.overworld(),key,CellUpgrades.value(upgrades,"compression_tier_card_",1),CellUpgrades.value(upgrades,"decompression_tier_card_",1));
    }
    private void refreshChain(AEItemKey key,boolean persist){
        if(ServerLifecycleHooks.getCurrentServer()==null&&chain!=null)return;
        var next=discover(key);
        if(chain!=null&&baseUnits>0){
            var converted=BigInteger.valueOf(baseUnits).multiply(BigInteger.valueOf(next.mainRate())).divideAndRemainder(BigInteger.valueOf(chain.mainRate()));
            // A downgrade may leave indivisible low-tier remainders. Preserve its saved exchange rates until drained.
            if(converted[1].signum()!=0||converted[0].bitLength()>63)return;baseUnits=converted[0].longValueExact();
        }
        chain=next;tiersUp=CellUpgrades.value(upgrades,"compression_tier_card_",1);tiersDown=CellUpgrades.value(upgrades,"decompression_tier_card_",1);
        if(persist)changed();
    }
    private long capacity(){
        if(chain==null)return 0;
        long mainItems=CellMath.capacity(item.displayBytes(stack),item.overhead(stack),1,item.multiplier(stack),AEKeyType.items().getAmountPerByte());
        long max=CellMath.multiply(mainItems,chain.mainRate());
        long reserve=CellMath.add(chain.mainRate()-1,CellMath.multiply(AEKeyType.items().getAmountPerByte()-1,chain.mainRate()));
        return Math.max(0,max-reserve);
    }
    @Override public long insert(AEKey key,long amount,Actionable mode,IActionSource source){
        MEStorage.checkPreconditions(key,amount,mode,source);
        if(amount==0||!(key instanceof AEItemKey itemKey)||!item.family.enabled()||StorageCells.isCellHandled(itemKey.toStack()))return 0;
        var chosen=item.getConfigInventory(stack).getKey(0);
        AEItemKey anchor=chosen instanceof AEItemKey a?a:chain==null?itemKey:chain.keys().get(chain.main());
        CompressionChain candidate=chain;
        if(candidate==null||baseUnits==0&&!candidate.keys().get(candidate.main()).equals(anchor))candidate=discover(anchor);
        if(chosen instanceof AEItemKey selected&&baseUnits>0&&candidate.rate(selected)==0)return 0;
        long rate=candidate.rate(itemKey);
        if(rate<=0&&CellUpgrades.has(upgrades,"tag_card")){
            String tag=stack.hasTag()?stack.getTag().getString("filterTag"):"";
            // Unification is opt-in: both variants must belong to the explicitly configured tag.
            if(!tag.isBlank()&&ResourceFilters.matches(itemKey,List.of(),false,false,item.getFuzzyMode(stack),tag))
                for(int i=0;i<candidate.keys().size();i++)if(ResourceFilters.matches(candidate.keys().get(i),List.of(),false,false,item.getFuzzyMode(stack),tag)){rate=candidate.rates().get(i);break;}
        }
        if(rate<=0)return 0;
        long stored=chain!=null&&chain.rate(itemKey)>0?baseUnits/chain.rate(itemKey):0;
        // Simulate must not bind an empty cell or write NBT.
        var previous=chain;chain=candidate;long accepted=Math.min(amount,Math.max(0,capacity()-baseUnits)/rate);chain=previous;
        if(accepted>0&&mode==Actionable.MODULATE){chain=candidate;baseUnits+=accepted*rate;changed();}
        return canVoidOverflow(overflow(),stored)?amount:accepted;
    }
    @Override public long extract(AEKey key,long amount,Actionable mode,IActionSource source){
        MEStorage.checkPreconditions(key,amount,mode,source);if(!(key instanceof AEItemKey i)||chain==null)return 0;
        long rate=chain.rate(i);if(rate==0)return 0;long extracted=Math.min(amount,baseUnits/rate);
        if(extracted>0&&mode==Actionable.MODULATE){baseUnits-=extracted*rate;changed();}return extracted;
    }
    @Override public void getAvailableStacks(KeyCounter out){if(chain!=null)for(int i=0;i<chain.keys().size();i++){long count=baseUnits/chain.rates().get(i);if(count>0)out.set(chain.keys().get(i),CellMath.add(out.get(chain.keys().get(i)),count));}}
    public boolean withinCapacity(){return baseUnits<=capacity();}
    @Override public long usedBytes(){return baseUnits==0?0:CellMath.add(CellMath.multiply(item.overhead(stack),item.multiplier(stack)),CellMath.ceilDivide(CellMath.ceilDivide(baseUnits,chain.mainRate()),AEKeyType.items().getAmountPerByte()));}
    @Override public long storedUnits(){return baseUnits;}
    @Override public int storedTypes(){return baseUnits==0?0:1;}
    @Override protected void saveContents(CompoundTag n){n.putLong("baseUnits",baseUnits);n.putInt("tiersUp",tiersUp);n.putInt("tiersDown",tiersDown);if(chain!=null)n.put("chain",chain.save());}
    public CompressionChain chain(){return chain;}
}
