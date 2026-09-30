package com.mpp.aedialsworks.cells.client;
import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.cells.cell.*;
import com.mpp.aedialsworks.cells.integration.ae2.CellsData;
import com.mpp.aedialsworks.common.registry.AWCells;
import com.mpp.aedialsworks.common.util.AWIds;
import net.minecraft.client.resources.model.*;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=Aedialsworks.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class CellsModels {
    @SubscribeEvent public static void register(ModelEvent.RegisterAdditional e){AWCells.BLOCKS.keySet().forEach(k->e.register(AWIds.id("block/cells/"+CellsData.original(k))));AWCells.PARTS.keySet().stream().filter(k->!k.proxy()).forEach(k->e.register(AWIds.id("item/cells/part/"+CellsData.original(k))));}
    @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult e){
        if(!com.mpp.aedialsworks.common.config.AWConfigs.CLIENT.cells.interfaces.useFixedInterfaceTextures.get())for(var entry:e.getModels().entrySet())if(entry.getKey() instanceof ModelResourceLocation key&&key.getNamespace().equals(Aedialsworks.MODID)){
            AWCells.BLOCKS.keySet().stream().filter(k->k.id.equals(key.getPath())).findFirst().ifPresent(k->{var model=e.getModels().get(AWIds.id("block/cells/"+CellsData.original(k)));if(model!=null)entry.setValue(model);});
            AWCells.PARTS.forEach((k,item)->{if(!k.proxy()&&item.getId().getPath().equals(key.getPath())){var model=e.getModels().get(AWIds.id("item/cells/part/"+CellsData.original(k)));if(model!=null)entry.setValue(model);}});
        }
        var id=new ModelResourceLocation(AWIds.id("configurable_cell"),"inventory");var model=e.getModels().get(id);if(model==null)return;
        e.getModels().put(id,new BakedModelWrapper<BakedModel>(model){@Override public ItemOverrides getOverrides(){return new ItemOverrides(){@Override public BakedModel resolve(BakedModel original,ItemStack stack,ClientLevel level,LivingEntity entity,int seed){var item=(TieredCellItem)stack.getItem();var spec=ComponentSpec.of(item.component(stack));if(spec==null)return original;var family=spec.family()==CellFamily.CONFIGURABLE?CellFamily.COMPACTING:spec.family();var target=AWCells.cell(family,spec.tier());return target==null?original:e.getModels().getOrDefault(new ModelResourceLocation(target.getId(),"inventory"),original);}};}});
    }
}
