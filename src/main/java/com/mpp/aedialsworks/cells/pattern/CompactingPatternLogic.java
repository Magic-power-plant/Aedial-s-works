package com.mpp.aedialsworks.cells.pattern;

import java.util.*;
import appeng.api.config.Actionable;
import appeng.api.crafting.*;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.*;
import appeng.api.storage.StorageHelper;
import com.mpp.aedialsworks.cells.*;
import com.mpp.aedialsworks.cells.api.CellsHost;
import com.mpp.aedialsworks.cells.cell.compacting.CompactingRecipes;
import net.minecraft.nbt.*;

/** Emits verified reversible recipes and persists crafted output before trying network delivery. */
public final class CompactingPatternLogic extends AbstractCellsLogic implements ICraftingProvider {
    private List<IPatternDetails> patterns=List.of();
    private final Map<AEItemKey,Long> pending=new LinkedHashMap<>();
    private long lastScan;
    public CompactingPatternLogic(CellsHost host){super(host);host.cellsNode().addService(ICraftingProvider.class,this);}
    @Override protected void onSettingsChanged(){patterns=List.of();if(host.cellsNode().isReady())ICraftingProvider.requestUpdate(host.cellsNode());lastScan=-100;}
    private void rebuild(){
        if(host.cellsLevel()==null||host.cellsLevel().isClientSide())return;var next=new LinkedHashMap<AEItemKey,IPatternDetails>();
        for(var key:filters.keySet())if(key instanceof AEItemKey item){
            var up=CompactingRecipes.higher(host.cellsLevel(),item);if(up!=null){add(next,item,up.ratio(),up.key(),1);add(next,up.key(),1,item,up.ratio());}
            var down=CompactingRecipes.lower(host.cellsLevel(),item);if(down!=null){add(next,item,1,down.key(),down.ratio());add(next,down.key(),down.ratio(),item,1);}
        }
        var result=List.copyOf(next.values());boolean changed=!patterns.stream().map(IPatternDetails::getDefinition).toList().equals(result.stream().map(IPatternDetails::getDefinition).toList());patterns=result;if(changed&&host.cellsNode().isReady())ICraftingProvider.requestUpdate(host.cellsNode());
    }
    private void add(Map<AEItemKey,IPatternDetails> list,AEItemKey input,long amount,AEItemKey output,long result){
        var encoded=PatternDetailsHelper.encodeProcessingPattern(new GenericStack[]{new GenericStack(input,amount)},new GenericStack[]{new GenericStack(output,result)});
        var pattern=PatternDetailsHelper.decodePattern(encoded,host.cellsLevel());if(pattern!=null)list.put(pattern.getDefinition(),pattern);
    }
    @Override public List<IPatternDetails> getAvailablePatterns(){return active()?patterns:List.of();}
    @Override public int getPatternPriority(){return priority;}
    @Override public boolean isBusy(){return !active()||!pending.isEmpty();}
    @Override public boolean pushPattern(IPatternDetails requested,KeyCounter[] inputs){
        if(isBusy()||inputs.length!=1)return false;
        // Revalidate against current recipes so a datapack reload cannot mint invalid products.
        rebuild();var pattern=patterns.stream().filter(p->p.getDefinition().equals(requested.getDefinition())).findFirst().orElse(null);if(pattern==null)return false;
        var expected=pattern.getInputs()[0];var choices=expected.getPossibleInputs();if(choices.length!=1)return false;var input=choices[0];long amount=Math.multiplyExact(input.amount(),expected.getMultiplier());
        if(inputs[0].size()!=1||inputs[0].get(input.what())!=amount)return false;
        var output=pattern.getOutputs();if(output.length!=1||!(output[0].what() instanceof AEItemKey key))return false;
        pending.put(key,output[0].amount());changed();return true;
    }
    @Override public boolean tick(){
        if(!active()||grid()==null)return false;long now=host.cellsLevel().getGameTime();if(now-lastScan>=20){lastScan=now;rebuild();}
        boolean worked=false;var iter=pending.entrySet().iterator();while(iter.hasNext()){var row=iter.next();long accepted=StorageHelper.poweredInsert(grid().getEnergyService(),grid().getStorageService().getInventory(),row.getKey(),row.getValue(),source);if(accepted>0){if(accepted==row.getValue())iter.remove();else row.setValue(row.getValue()-accepted);worked=true;}}
        if(worked)changed();return worked;
    }
    @Override protected void saveContents(CompoundTag n){var list=new ListTag();pending.forEach((key,amount)->{var row=new CompoundTag();row.put("key",key.toTag());row.putLong("amount",amount);list.add(row);});n.put("pending",list);}
    @Override protected void loadContents(CompoundTag n){pending.clear();for(var raw:n.getList("pending",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;var key=AEItemKey.fromTag(row.getCompound("key"));long amount=row.getLong("amount");if(key!=null&&amount>0)pending.put(key,amount);}}
    @Override public CompoundTag snapshot(int port,int page){var n=super.snapshot(port,page);n.putInt("patterns",patterns.size());n.putBoolean("busy",isBusy());return n;}
}
