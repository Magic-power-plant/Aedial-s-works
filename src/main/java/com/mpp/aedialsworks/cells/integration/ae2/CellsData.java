package com.mpp.aedialsworks.cells.integration.ae2;
import java.util.*;
import java.util.function.Consumer;
import appeng.core.definitions.AEItems;
import appeng.recipes.handlers.*;
import com.mpp.aedialsworks.cells.MachineKind;
import com.mpp.aedialsworks.cells.cell.*;
import com.mpp.aedialsworks.common.registry.*;
import com.mpp.aedialsworks.common.util.AWIds;
import com.mpp.aedialsworks.data.*;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.data.LanguageProvider;

public final class CellsData implements AWDataModule {
    public static String original(MachineKind k){return switch(k){case FLUID_IMPORT->"import_fluid_interface";case FLUID_EXPORT->"export_fluid_interface";case COMBINED_IMPORT->"import_combined_interface";case COMBINED_EXPORT->"export_combined_interface";case ITEM_IO->"io_item_interface";case FLUID_IO->"io_fluid_interface";default->k.id;};}
    @Override public void language(String locale,LanguageProvider p){
        try(var input=CellsData.class.getResourceAsStream("/assets/aedialsworks/cells/lang/"+locale+".json")){
            if(input==null)throw new IllegalStateException("Missing CELLS language source "+locale);
            var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();json.entrySet().forEach(e->p.add(e.getKey(),e.getValue().getAsString()));
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
    }
    @Override public void blockStates(BlockStateProvider p){for(var e:AWCells.BLOCKS.entrySet())p.simpleBlock(e.getValue().get(),p.models().getExistingFile(AWIds.id("block/cells/"+original(e.getKey())+"_fixed")));}
    @Override public void itemModels(ItemModelProvider p){
        AWCells.BLOCK_ITEMS.forEach((k,v)->p.withExistingParent(v.getId().getPath(),AWIds.id("block/cells/"+original(k)+"_fixed")));
        AWCells.PARTS.forEach((k,v)->p.withExistingParent(v.getId().getPath(),AWIds.id("item/cells/part/"+original(k)+(k.proxy()?"":"_fixed"))));
        AWCells.CELLS.forEach((id,v)->{var c=v.get();String path=c.family.creative()?"creative/creative_"+(c.family.fluid?"fluid_cell":"cell"):c.family==CellFamily.CONFIGURABLE?"configurable_cell":"cell_"+(c.family.highDensity?"hyper_density":"normal")+"/layered_"+(c.tier.ordinal()%6);p.withExistingParent(id,AWIds.id("item/cells/cells/"+path));});
        AWCells.COMPONENTS.forEach((id,v)->{var c=v.get();p.withExistingParent(id,AWIds.id("item/cells/cells/component_"+(c.family.highDensity?"hyper_density":"normal")+"/layered_"+c.tier.id));});
        AWCells.UPGRADES.forEach((id,v)->p.withExistingParent(id,AWIds.id("item/cells/upgrades/"+(id.equals("tag_card")?"oredict_card":id))));
        AWCells.MATERIALS.forEach((id,v)->p.withExistingParent(id,AWIds.id("item/cells/processors/"+id)));
    }
    @Override public void recipes(Consumer<FinishedRecipe> out){for(String type:List.of("calculation","engineering","logic"))for(boolean singularity:List.of(false,true)){
        String prefix=singularity?"singularity":"overclocked",tier=singularity?"quadruple_compressed":"compressed";
        InscriberRecipeBuilder.inscribe(singularity?AEItems.SINGULARITY:AEItems.MATTER_BALL,AWCells.MATERIALS.get(prefix+"_processor_"+type).get(),1)
            .setTop(Ingredient.of(AWCells.MATERIALS.get("compressed_"+type+"_print_"+tier).get())).setBottom(Ingredient.of(AWCells.MATERIALS.get("compressed_silicon_print_"+tier).get()))
            .setMode(InscriberProcessType.PRESS).save(out,AWIds.id("cells/inscriber/"+prefix+"_processor_"+type));
    }}
    @Override public void blockTags(AWBlockTags p){AWCells.BLOCKS.values().forEach(b->p.add(BlockTags.MINEABLE_WITH_PICKAXE,b.get()));}
    @Override public void blockLoot(AWBlockLoot p){AWCells.BLOCKS.values().forEach(b->p.selfDrop(b.get()));}
}
