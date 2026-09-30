package com.mpp.aedialsworks.cells.cell;

import java.util.List;
import appeng.api.config.FuzzyMode;
import appeng.api.stacks.*;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.upgrades.*;
import appeng.items.contents.CellConfig;
import appeng.util.ConfigInventory;
import com.mpp.aedialsworks.cells.upgrades.CellUpgrades;
import com.mpp.aedialsworks.common.config.AWConfigs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Stack limit one closes the old workbench/chest copy exploit without mixins. */
public final class TieredCellItem extends Item implements IBasicCellItem {
    public final CellFamily family;
    public final CellTier tier;
    public TieredCellItem(CellFamily family,CellTier tier){super(new Properties().stacksTo(1));this.family=family;this.tier=tier;}
    public CellFamily storageFamily(ItemStack stack){
        if(family!=CellFamily.CONFIGURABLE)return family;
        var component=component(stack);
        return component.getItem() instanceof CellComponentItem c?c.family:CellFamily.CONFIGURABLE;
    }
    public ItemStack component(ItemStack stack){return stack.hasTag()?ItemStack.of(stack.getTag().getCompound("component")):ItemStack.EMPTY;}
    public CellTier storageTier(ItemStack stack){var spec=ComponentSpec.of(component(stack));return spec==null?tier:spec.tier();}
    public AEKeyType keyType(ItemStack stack){return storageFamily(stack)==CellFamily.CONFIGURABLE&&stack.hasTag()&&stack.getTag().getBoolean("fluidChannel")?AEKeyType.fluids():storageFamily(stack).keyType();}
    public long displayBytes(ItemStack stack){return family==CellFamily.CONFIGURABLE&&component(stack).isEmpty()?0:storageTier(stack).bytes;}
    public long multiplier(ItemStack stack){return storageFamily(stack).highDensity?Integer.MAX_VALUE:1;}
    public int maximumTypes(ItemStack stack){
        var f=storageFamily(stack);int max=f.maxTypes();
        if(family==CellFamily.CONFIGURABLE&&!f.compacting)max=keyType(stack)==AEKeyType.fluids()?AWConfigs.SERVER.cells.general.configurableCellFluidMaxTypes.get():AWConfigs.SERVER.cells.general.configurableCellItemMaxTypes.get();
        int equal=CellUpgrades.value(getUpgrades(stack),"equal_distribution_card_",0);
        if(equal>0){max=Math.min(max,equal);int selected=getConfigInventory(stack).keySet().size();if(selected>0)max=Math.min(max,selected);}
        return max;
    }
    public long overhead(ItemStack stack){return storageFamily(stack).compacting?displayBytes(stack)/128:displayBytes(stack)/2/maximumTypes(stack);}
    @Override public AEKeyType getKeyType(){return family.keyType();}
    @Override public int getBytes(ItemStack stack){return (int)Math.min(Integer.MAX_VALUE,displayBytes(stack));}
    @Override public int getBytesPerType(ItemStack stack){return (int)Math.min(Integer.MAX_VALUE,overhead(stack));}
    @Override public int getTotalTypes(ItemStack stack){return maximumTypes(stack);}
    // AE2's built-in handler uses int accounting. Our handler must be the only owner.
    @Override public boolean isStorageCell(ItemStack stack){return false;}
    @Override public boolean storableInStorageCell(){return false;}
    @Override public double getIdleDrain(){return family.idleDrain();}
    @Override public IUpgradeInventory getUpgrades(ItemStack stack){return UpgradeInventories.forItem(stack,family.upgradeSlots());}
    @Override public ConfigInventory getConfigInventory(ItemStack stack){return CellConfig.create(keyType(stack).filter(),stack,storageFamily(stack).compacting?1:63);}
    @Override public FuzzyMode getFuzzyMode(ItemStack stack){try{return FuzzyMode.valueOf(stack.hasTag()?stack.getTag().getString("FuzzyMode"):"");}catch(IllegalArgumentException ex){return FuzzyMode.IGNORE_ALL;}}
    @Override public void setFuzzyMode(ItemStack stack,FuzzyMode mode){stack.getOrCreateTag().putString("FuzzyMode",mode.name());}
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        var stack=player.getItemInHand(hand);if(hand!=net.minecraft.world.InteractionHand.MAIN_HAND)return net.minecraft.world.InteractionResultHolder.pass(stack);
        if(!level.isClientSide&&player instanceof net.minecraft.server.level.ServerPlayer server&&player.mayBuild()){
            if(player.isShiftKeyDown()&&!family.creative()){
                var state=CellHandler.open(stack,null);if(state==null||state.storedUnits()!=0)return net.minecraft.world.InteractionResultHolder.fail(stack);
                var component=com.mpp.aedialsworks.common.recipe.CellComponentRecipe.componentOf(stack);var upgrades=getUpgrades(stack);var returned=new java.util.ArrayList<ItemStack>();if(!component.isEmpty())returned.add(component);
                for(int i=0;i<upgrades.size();i++)if(!upgrades.getStackInSlot(i).isEmpty())returned.add(upgrades.getStackInSlot(i).copy());
                returned.add(family==CellFamily.CONFIGURABLE?new ItemStack(this):appeng.core.definitions.AEItems.ITEM_CELL_HOUSING.stack());stack.shrink(1);
                for(var result:returned)if(!player.getInventory().add(result))player.drop(result,false);
            }else com.mpp.aedialsworks.cells.menu.CellConfigurationMenu.open(server);
        }return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){
        var cell=CellHandler.open(stack,null);
        if(cell!=null){lines.add(Component.translatable("tooltip.aedialsworks.cells.bytes",cell.usedBytes(),cell.totalBytes()));lines.add(Component.translatable("tooltip.aedialsworks.cells.types",cell.storedTypes(),cell.maximumTypes()));}
        if(!family.enabled())lines.add(Component.translatable("tooltip.aedialsworks.cells.disabled"));
        if(family==CellFamily.CONFIGURABLE&&component(stack).isEmpty())lines.add(Component.translatable("tooltip.aedialsworks.cells.assemble"));
        if(AWConfigs.SERVER.cells.general.enableNbtSizeTooltip.get()&&stack.hasTag()){
            long bytes=stack.getTag().sizeInBytes();if(bytes>AWConfigs.SERVER.cells.general.nbtSizeWarningThresholdKB.get()*1024L)lines.add(Component.translatable("tooltip.aedialsworks.cells.nbt",bytes/1024));
        }
    }
}
