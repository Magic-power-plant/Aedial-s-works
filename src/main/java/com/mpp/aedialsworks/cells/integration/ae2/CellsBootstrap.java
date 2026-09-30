package com.mpp.aedialsworks.cells.integration.ae2;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import com.mpp.aedialsworks.common.registry.AWCells;
import com.mpp.aedialsworks.cells.cell.*;
public final class CellsBootstrap {
    private CellsBootstrap(){}
    public static void models(){for(var kind:AWCells.PARTS.keySet()){
        String base=CellsPart.modelPath(kind);
        String[] suffixes=kind.proxy()?new String[]{"base","status_off","status_on","status_active"}:new String[]{"base","base_fixed","off","on","has_channel","has_channel_fixed"};
        for(String suffix:suffixes)appeng.api.parts.PartModels.registerModels(com.mpp.aedialsworks.common.util.AWIds.id(base+"/"+suffix));
    }}
    public static void setup(){
        StorageCells.addCellHandler(new CellHandler());
        com.mpp.aedialsworks.cells.menu.CellsMenu.registerOpener();
        for(var entry:AWCells.BLOCKS.entrySet()){
            var type=AWCells.BLOCK_ENTITIES.get(entry.getKey()).get();
            entry.getValue().get().setBlockEntity(CellsBlockEntity.class,type,null,null);
            appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(type,AWCells.BLOCK_ITEMS.get(entry.getKey()).get());
        }
        AWCells.BLOCK_ITEMS.forEach((kind,machine)->machineUpgrades(kind,machine.get()));AWCells.PARTS.forEach((kind,machine)->machineUpgrades(kind,machine.get()));
        for(var entry:AWCells.CELLS.values()){
            var cell=entry.get();if(cell.family.creative())continue;
            if(!cell.family.compacting){Upgrades.add(AEItems.FUZZY_CARD,cell,1);Upgrades.add(AEItems.INVERTER_CARD,cell,1);}
            for(var upgrade:AWCells.UPGRADES.entrySet()){
                var name=upgrade.getKey();boolean compact=cell.family.compacting||cell.family==CellFamily.CONFIGURABLE;
                if(name.startsWith("compression_")||name.startsWith("decompression_")){if(compact)Upgrades.add(upgrade.getValue().get(),cell,1);}
                else if(name.startsWith("equal_")&&!cell.family.compacting||name.equals("overflow_card")||name.equals("tag_card"))Upgrades.add(upgrade.getValue().get(),cell,1);
            }
        }
    }
    private static void machineUpgrades(com.mpp.aedialsworks.cells.MachineKind kind,net.minecraft.world.level.ItemLike machine){
        if(kind.resourceInterface())Upgrades.add(AEItems.CAPACITY_CARD,machine,4);
        if(kind.resourceInterface()||kind.proxy())Upgrades.add(AEItems.FUZZY_CARD,machine,1);
        if(kind.input||kind==com.mpp.aedialsworks.cells.MachineKind.PROXY_FRONT){Upgrades.add(AEItems.INVERTER_CARD,machine,1);Upgrades.add(AWCells.UPGRADES.get("tag_card").get(),machine,1);}
        if(kind.resourceInterface()){if(kind.input)for(String id:java.util.List.of("pull_card","overflow_card","trash_unselected_card"))Upgrades.add(AWCells.UPGRADES.get(id).get(),machine,1);if(kind.output)Upgrades.add(AWCells.UPGRADES.get("push_card").get(),machine,1);}
        if(kind==com.mpp.aedialsworks.cells.MachineKind.PROXY_FRONT)Upgrades.add(AWCells.UPGRADES.get("insertion_card").get(),machine,1);
    }

}
