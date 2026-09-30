package com.mpp.aedialsworks.common.recipe;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import com.mpp.aedialsworks.cells.cell.*;
import com.mpp.aedialsworks.cells.cell.compacting.CompactingCellState;
import com.mpp.aedialsworks.common.registry.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Atomic cell assembly/swap. Inputs are copied, all contents must fit, and the old component is returned. */
public final class CellComponentRecipe extends CustomRecipe {
    public CellComponentRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
    private record Inputs(int slot,ItemStack cell,ItemStack component){}
    private Inputs inputs(CraftingContainer inv){int slot=-1;ItemStack cell=ItemStack.EMPTY,component=ItemStack.EMPTY;
        for(int i=0;i<inv.getContainerSize();i++){var s=inv.getItem(i);if(s.isEmpty())continue;
            if(s.getItem() instanceof TieredCellItem c&&!c.family.creative()&&cell.isEmpty()&&s.getCount()==1){cell=s;slot=i;}
            else if(ComponentSpec.of(s)!=null&&component.isEmpty())component=s;
            else return null;
        }return cell.isEmpty()||component.isEmpty()?null:new Inputs(slot,cell,component);
    }
    public static ItemStack replaced(ItemStack cell,ItemStack component){
        var replacement=ComponentSpec.of(component);if(!(cell.getItem() instanceof TieredCellItem old)||replacement==null||cell.getCount()!=1||old.family.creative()||replacement.family()==CellFamily.CONFIGURABLE&&old.family!=CellFamily.CONFIGURABLE)return ItemStack.EMPTY;
        var previous=CellHandler.open(cell.copy(),null);if(previous==null)return ItemStack.EMPTY;
        ItemStack result;
        if(old.family==CellFamily.CONFIGURABLE){result=cell.copy();result.getOrCreateTag().put("component",component.copyWithCount(1).save(new net.minecraft.nbt.CompoundTag()));}
        else {result=new ItemStack(AWCells.cell(replacement.family(),replacement.tier()).get());if(cell.hasTag())result.setTag(cell.getTag().copy());}
        if(old.keyType(cell)!=((TieredCellItem)result.getItem()).keyType(result)&&previous.storedUnits()>0)return ItemStack.EMPTY;
        var nextItem=(TieredCellItem)result.getItem();var oldUpgrades=old.getUpgrades(cell);var newUpgrades=nextItem.getUpgrades(result);
        for(int i=0;i<oldUpgrades.size();i++){var card=oldUpgrades.getStackInSlot(i);if(!card.isEmpty()&&(i>=newUpgrades.size()||appeng.api.upgrades.Upgrades.getMaxInstallable(card.getItem(),nextItem)<oldUpgrades.getInstalledUpgrades(card.getItem())))return ItemStack.EMPTY;}
        // A compact pool cannot be expanded by enumerating every denomination (that would duplicate resources).
        if(previous instanceof CompactingCellState compact&&previous.storedUnits()>0){
            if(!replacement.family().compacting)return ItemStack.EMPTY;
            var next=CellHandler.open(result,null);if(next.usedBytes()>next.totalBytes()||next instanceof CompactingCellState pooled&&!pooled.withinCapacity())return ItemStack.EMPTY;return result;
        }
        result.getOrCreateTag().remove("awCell");
        var overflow=com.mpp.aedialsworks.common.registry.AWCells.UPGRADES.get("overflow_card").get();int overflowSlot=-1;
        for(int i=0;i<newUpgrades.size();i++)if(newUpgrades.getStackInSlot(i).is(overflow)){overflowSlot=i;newUpgrades.setItemDirect(i,ItemStack.EMPTY);break;}
        var next=CellHandler.open(result,null);var entries=new KeyCounter();previous.getAvailableStacks(entries);
        for(var entry:entries){long before=next.storedUnits();long accepted=next.insert(entry.getKey(),entry.getLongValue(),Actionable.MODULATE,IActionSource.empty());
            // Overflow cards may report a successful void: compare actual stored content as well.
            if(accepted!=entry.getLongValue()||next instanceof DenseCellState&&next.storedUnits()-before!=entry.getLongValue())return ItemStack.EMPTY;
        }next.persist();if(overflowSlot>=0)newUpgrades.setItemDirect(overflowSlot,new ItemStack(overflow));return result;
    }
    public static ItemStack componentOf(ItemStack cell){var c=(TieredCellItem)cell.getItem();return c.family==CellFamily.CONFIGURABLE?c.component(cell):new ItemStack(AWCells.component(c.family,c.tier).get());}
    @Override public boolean matches(CraftingContainer inv,Level level){var i=inputs(inv);return i!=null&&!replaced(i.cell,i.component).isEmpty();}
    @Override public ItemStack assemble(CraftingContainer inv,RegistryAccess access){var i=inputs(inv);return i==null?ItemStack.EMPTY:replaced(i.cell,i.component);}
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv){var result=NonNullList.withSize(inv.getContainerSize(),ItemStack.EMPTY);var i=inputs(inv);if(i!=null&&!replaced(i.cell,i.component).isEmpty())result.set(i.slot,componentOf(i.cell));return result;}
    @Override public boolean canCraftInDimensions(int w,int h){return w*h>=2;}
    @Override public RecipeSerializer<?> getSerializer(){return AWRecipeSerializers.CELL_COMPONENT.get();}
}
