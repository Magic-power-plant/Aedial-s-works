package com.mpp.aedialsworks.common.registry;

import java.util.*;
import com.mpp.aedialsworks.cells.cell.*;
import appeng.api.upgrades.Upgrades;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

/** CELLS declarations use the existing central DeferredRegisters, preserving one registration lifecycle. */
public final class AWCells {
    public static final Map<String,RegistryObject<TieredCellItem>> CELLS=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<CellComponentItem>> COMPONENTS=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> UPGRADES=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> MATERIALS=new LinkedHashMap<>();
    public static final Map<com.mpp.aedialsworks.cells.MachineKind,RegistryObject<com.mpp.aedialsworks.cells.integration.ae2.CellsMachineBlock>> BLOCKS=new LinkedHashMap<>();
    public static final Map<com.mpp.aedialsworks.cells.MachineKind,RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<com.mpp.aedialsworks.cells.integration.ae2.CellsBlockEntity>>> BLOCK_ENTITIES=new LinkedHashMap<>();
    public static final Map<com.mpp.aedialsworks.cells.MachineKind,RegistryObject<net.minecraft.world.item.BlockItem>> BLOCK_ITEMS=new LinkedHashMap<>();
    public static final Map<com.mpp.aedialsworks.cells.MachineKind,RegistryObject<com.mpp.aedialsworks.cells.integration.ae2.CellsPartItem>> PARTS=new LinkedHashMap<>();
    static {
        for(var kind:com.mpp.aedialsworks.cells.MachineKind.values()){
            if(!kind.proxy()){
                var block=AWBlocks.BLOCKS.register(kind.id,()->new com.mpp.aedialsworks.cells.integration.ae2.CellsMachineBlock(kind));BLOCKS.put(kind,block);
                BLOCK_ITEMS.put(kind,AWItems.ITEMS.register(kind.id,()->new net.minecraft.world.item.BlockItem(block.get(),new Item.Properties())));
                BLOCK_ENTITIES.put(kind,AWBlockEntities.BLOCK_ENTITY_TYPES.register(kind.id,()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of((pos,state)->new com.mpp.aedialsworks.cells.integration.ae2.CellsBlockEntity(BLOCK_ENTITIES.get(kind).get(),pos,state),block.get()).build(null)));
            }
            if(kind!=com.mpp.aedialsworks.cells.MachineKind.EXPOSER){String id=kind.id+(kind.proxy()?"":"_part");PARTS.put(kind,AWItems.ITEMS.register(id,()->new com.mpp.aedialsworks.cells.integration.ae2.CellsPartItem(kind)));}
        }

        for(var family:List.of(CellFamily.COMPACTING,CellFamily.HD_ITEM,CellFamily.HD_COMPACTING,CellFamily.HD_FLUID))for(var tier:CellTier.values()){
            if(family.highDensity&&tier==CellTier.G2)continue;
            String cell=family.id+"_cell_"+tier.id, component=family.id+"_component_"+tier.id;
            CELLS.put(cell,AWItems.ITEMS.register(cell,()->new TieredCellItem(family,tier)));
            COMPONENTS.put(component,AWItems.ITEMS.register(component,()->new CellComponentItem(family,tier)));
        }
        for(var family:List.of(CellFamily.CONFIGURABLE,CellFamily.CREATIVE_ITEM,CellFamily.CREATIVE_FLUID)){
            String id=family.id+"_cell";CELLS.put(id,AWItems.ITEMS.register(id,()->new TieredCellItem(family,CellTier.K1)));
        }
        for(String name:List.of("overflow_card","tag_card","pull_card","push_card","insertion_card","trash_unselected_card"))upgrade(name);
        for(String tier:List.of("1x","2x","4x","8x","16x","32x","63x","infinite"))upgrade("equal_distribution_card_"+tier);
        for(String type:List.of("compression","decompression"))for(int tier:List.of(3,6,9,12,15))upgrade(type+"_tier_card_"+tier+"x");
        for(String name:List.of("compressed_calculation_print_compressed","compressed_calculation_print_double_compressed","compressed_calculation_print_triple_compressed","compressed_calculation_print_quadruple_compressed","compressed_engineering_print_compressed","compressed_engineering_print_double_compressed","compressed_engineering_print_triple_compressed","compressed_engineering_print_quadruple_compressed","compressed_logic_print_compressed","compressed_logic_print_double_compressed","compressed_logic_print_triple_compressed","compressed_logic_print_quadruple_compressed","compressed_silicon_print_compressed","compressed_silicon_print_double_compressed","compressed_silicon_print_triple_compressed","compressed_silicon_print_quadruple_compressed","overclocked_processor_calculation","overclocked_processor_engineering","overclocked_processor_logic","singularity_processor_calculation","singularity_processor_engineering","singularity_processor_logic"))MATERIALS.put(name,AWItems.ITEMS.register(name,()->new Item(new Item.Properties())));
    }
    private static void upgrade(String id){UPGRADES.put(id,AWItems.ITEMS.register(id,()->Upgrades.createUpgradeCardItem(new Item.Properties())));}
    public static RegistryObject<TieredCellItem> cell(CellFamily family,CellTier tier){return CELLS.get(family.id+"_cell_"+tier.id);}
    public static RegistryObject<CellComponentItem> component(CellFamily family,CellTier tier){return COMPONENTS.get(family.id+"_component_"+tier.id);}
    public static void initialize(){}
    private AWCells(){}
}
