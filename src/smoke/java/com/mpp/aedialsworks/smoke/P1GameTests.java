package com.mpp.aedialsworks.smoke;
import java.util.*;
import appeng.api.config.Actionable;
import appeng.api.features.GridLinkables;
import appeng.api.networking.GridHelper;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.*;
import appeng.api.storage.StorageCells;
import appeng.blockentity.storage.*;
import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.core.definitions.*;
import appeng.me.helpers.PlayerSource;
import com.mpp.aedialsworks.cellterminal.integration.ae2.*;
import com.mpp.aedialsworks.cellterminal.menu.CellTerminalMenu;
import com.mpp.aedialsworks.cellterminal.network.*;
import com.mpp.aedialsworks.cellterminal.scanner.*;
import com.mpp.aedialsworks.common.registry.*;
import com.mpp.aedialsworks.common.config.AWConfigs;
import com.mpp.aedialsworks.common.util.AWIds;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.*;
@GameTestHolder("aedialsworks") @PrefixGameTestTemplate(false)
public final class P1GameTests {
    @GameTest(template="empty") public static void partPersistenceAndDropConservation(GameTestHelper h){
        var part=AWItems.CELL_TERMINAL.get().createPart();var cell=AEItems.ITEM_CELL_1K.stack();part.temporaryCells().setItemDirect(15,cell);
        var tag=new CompoundTag();part.writeToNBT(tag);var copy=AWItems.CELL_TERMINAL.get().createPart();copy.readFromNBT(tag);
        h.assertTrue(copy.temporaryCells().size()==16 && ItemStack.matches(cell,copy.temporaryCells().getStackInSlot(15)),"All sixteen temp slots persist");
        var drops=new ArrayList<ItemStack>();copy.addAdditionalDrops(drops,true);h.assertTrue(drops.size()==1,"Exactly one cell drops");copy.clearContent();h.assertTrue(copy.temporaryCells().isEmpty(),"Clearing prevents duplicate drops");h.succeed();
    }
    @GameTest(template="empty") public static void chestSlotAndPartitionAndUpgrades(GameTestHelper h){
        h.setBlock(1,1,1,AEBlocks.CHEST.block());var chest=(ChestBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));
        chest.getInternalInventory().setItemDirect(0,new ItemStack(Items.DIAMOND,3));
        var target=new Ae2StorageTarget(1,chest,0,null);target.replaceCell(AEItems.FLUID_CELL_1K.stack());
        h.assertTrue(chest.getInternalInventory().getStackInSlot(0).getCount()==3,"Never overwrite ME chest input slot");
        var water=AEFluidKey.of(Fluids.WATER);h.assertTrue(target.setPartition(0,water),"Fluid partition is accepted");h.assertTrue(water.equals(target.partitionKey(0)),"Fluid key survives roundtrip");
        target.clearPartition();h.assertTrue(target.partitionKey(0)==null,"Clear partition");
        target.replaceCell(AEItems.ITEM_CELL_1K.stack());
        h.assertTrue(target.insertUpgrade(AEItems.FUZZY_CARD.stack()).isEmpty(),"Compatible upgrade inserted");
        h.assertTrue(!target.extractUpgrade(0).isEmpty(),"Upgrade extracted once");h.assertTrue(target.extractUpgrade(0).isEmpty(),"No duplicate upgrade");
        target.setPriority(-42);h.assertTrue(chest.getPriority()==-42,"Signed priority");h.succeed();
    }
    @GameTest(template="empty") public static void uniqueToolPreservesCountsAndFailureIsAtomic(GameTestHelper h){
        h.setBlock(1,1,1,AEBlocks.DRIVE.block());var drive=(DriveBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));var player=h.makeMockPlayer();var source=new PlayerSource(player);
        var a=AEItems.ITEM_CELL_1K.stack();var storage=StorageCells.getCellInventory(a,null);var iron=AEItemKey.of(Items.IRON_INGOT);var gold=AEItemKey.of(Items.GOLD_INGOT);
        storage.insert(iron,120,Actionable.MODULATE,source);storage.insert(gold,80,Actionable.MODULATE,source);storage.persist();drive.getInternalInventory().setItemDirect(0,a);
        var first=new Ae2StorageTarget(1,drive,0,null);var before=a.copy();
        h.assertFalse(NetworkToolOperations.attributeUnique(List.of(first),source),"Insufficient cells reject atomically");h.assertTrue(ItemStack.matches(before,drive.getInternalInventory().getStackInSlot(0)),"Failed operation preserves original NBT");
        drive.getInternalInventory().setItemDirect(1,AEItems.ITEM_CELL_1K.stack());var second=new Ae2StorageTarget(2,drive,1,null);
        h.assertTrue(NetworkToolOperations.attributeUnique(List.of(first,second),source),"Unique distribution succeeds");var total=new KeyCounter();
        for(var target:List.of(first,second)){var contents=new KeyCounter();target.storage().getAvailableStacks(contents);h.assertTrue(contents.size()==1,"One resource type per cell");total.addAll(contents);h.assertTrue(target.partitionKey(0)!=null,"Automatically partition distributed cells");}
        h.assertTrue(total.get(iron)==120 && total.get(gold)==80,"All amounts conserved");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=160) public static void liveGridScanAndPartLifecycle(GameTestHelper h){
        h.setBlock(1,1,1,AEBlocks.DRIVE.block());h.setBlock(2,1,1,AEBlocks.CREATIVE_ENERGY_CELL.block());
        var part=PartHelper.setPart(h.getLevel(),h.absolutePos(new BlockPos(1,1,2)),Direction.SOUTH,h.makeMockPlayer(),AWItems.CELL_TERMINAL.get());
        h.runAfterDelay(45,()->{var drive=(DriveBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));h.assertTrue(part.getGridNode()!=null,"Placed part owns a grid node");
            // Explicit API connection avoids depending on host-block orientation in this fixture.
            if(part.terminalGrid()!=drive.getMainNode().getGrid())GridHelper.createConnection(part.getGridNode(),drive.getMainNode().getNode());
            h.runAfterDelay(45,()->{try(var scanner=new Ae2StorageScanner()){h.assertTrue(part.isActive(),"Part becomes active with power/channel");h.assertTrue(scanner.scan(part.terminalGrid()).size()==10,"Enumerate all ten drive cells");h.succeed();}});
        });
    }
    @GameTest(template="empty",timeoutTicks=160) public static void wirelessBindingRangeEnergyAndNbt(GameTestHelper h){
        h.setBlock(1,1,1,AEBlocks.WIRELESS_ACCESS_POINT.block());h.setBlock(1,0,1,AEBlocks.CREATIVE_ENERGY_CELL.block());
        h.runAfterDelay(30,()->{
            var wap=(WirelessAccessPointBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));
            var power=(appeng.blockentity.networking.CreativeEnergyCellBlockEntity)h.getBlockEntity(new BlockPos(1,0,1));
            if(wap.getMainNode().getGrid()!=power.getMainNode().getGrid())GridHelper.createConnection(wap.getMainNode().getNode(),power.getMainNode().getNode());
        });
        h.runAfterDelay(75,()->{var player=h.makeMockSurvivalPlayer();var pos=h.absolutePos(new BlockPos(1,1,1));player.setPos(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5);
            var item=AWItems.WIRELESS_CELL_TERMINAL.get();var stack=new ItemStack(item);item.injectAEPower(stack,2000,Actionable.MODULATE);player.getInventory().setItem(0,stack);
            GridLinkables.get(item).link(stack,GlobalPos.of(h.getLevel().dimension(),pos));h.assertTrue(item.getLinkedGrid(stack,h.getLevel(),null)!=null,"Link resolves access point grid");
            var host=new WirelessCellTerminalHost(player,0,stack,(p,m)->{});h.assertTrue(host.canUseTerminal(player),"Linked, charged terminal in range");
            double before=item.getAECurrentPower(stack);for(int i=0;i<12;i++)host.onBroadcastChanges(player.inventoryMenu);h.assertTrue(item.getAECurrentPower(stack)<before,"Wireless host consumes distance-dependent energy");
            host.temporaryCells().setItemDirect(4,AEItems.ITEM_CELL_1K.stack());var reopened=new WirelessCellTerminalHost(player,0,stack,(p,m)->{});h.assertTrue(!reopened.temporaryCells().getStackInSlot(4).isEmpty(),"Wireless temp NBT persists");
            player.setPos(pos.getX()+10000,pos.getY(),pos.getZ());h.assertFalse(host.canUseTerminal(player),"Out of range denied");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=180) public static void subnetDiscoveryInBothDirections(GameTestHelper h){
        h.setBlock(1,1,1,AEBlocks.INTERFACE.block());h.setBlock(1,1,0,AEBlocks.CREATIVE_ENERGY_CELL.block());
        var bus=PartHelper.setPart(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),Direction.WEST,h.makeMockPlayer(),AEParts.STORAGE_BUS.asItem());
        h.setBlock(3,1,1,AEBlocks.CREATIVE_ENERGY_CELL.block());
        h.runAfterDelay(45,()->{var iface=(InterfaceBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));
            var power=(appeng.blockentity.networking.CreativeEnergyCellBlockEntity)h.getBlockEntity(new BlockPos(3,1,1));
            if(bus.getGridNode().getGrid()!=power.getMainNode().getGrid())GridHelper.createConnection(bus.getGridNode(),power.getMainNode().getNode());
            h.runAfterDelay(45,()->{var scan=new Ae2SubnetScanner();h.assertTrue(bus.getGridNode().getGrid()!=iface.getMainNode().getGrid(),"Two isolated subnet grids");
                h.assertTrue(scan.connections(bus.getGridNode().getGrid()).stream().anyMatch(link->link.to()==iface.getMainNode().getGrid() && link.outbound()),"Outbound storage bus to interface");
                h.assertTrue(scan.connections(iface.getMainNode().getGrid()).stream().anyMatch(link->link.to()==bus.getGridNode().getGrid() && !link.outbound()),"Inbound interface to storage bus");h.succeed();});
        });
    }

    private static final class TestHost extends appeng.api.implementations.menuobjects.ItemMenuHost implements com.mpp.aedialsworks.cellterminal.menu.TerminalHost {
        private final appeng.api.networking.IGrid grid;
        private final TempCellInventory temp=new TempCellInventory(()->{});
        TestHost(net.minecraft.server.level.ServerPlayer player,appeng.api.networking.IGrid grid){super(player,null,new ItemStack(AWItems.WIRELESS_CELL_TERMINAL.get()));this.grid=grid;}
        public appeng.api.networking.IGrid terminalGrid(){return grid;}
        public appeng.api.inventories.InternalInventory temporaryCells(){return temp;}
        public boolean canUseTerminal(net.minecraft.world.entity.player.Player player){return player==getPlayer();}
        public void saveTerminal(){}
    }
    @GameTest(template="empty",timeoutTicks=180) public static void liveMenuPermissionsReloadAndStaleTargets(GameTestHelper h){
        h.setBlock(1,1,1,AEBlocks.DRIVE.block());h.setBlock(2,1,1,AEBlocks.CREATIVE_ENERGY_CELL.block());
        h.runAfterDelay(60,()->{
            var drive=(DriveBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));drive.getInternalInventory().setItemDirect(0,AEItems.ITEM_CELL_1K.stack());
            var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());var menu=new CellTerminalMenu(42,player.getInventory(),new TestHost(player,drive.getMainNode().getGrid()),UUID.randomUUID());player.containerMenu=menu;menu.broadcastChanges();
            // The first drive slot in a newly created scan has session id 100, token 1.
            var payload=new CompoundTag();payload.putUUID("session",menu.session());payload.putLong("id",100);payload.putLong("token",1);payload.putInt("priority",1234);
            boolean previous=AWConfigs.SERVER.cellterminal.misc.priorityEditEnabled.get();
            try {
                AWConfigs.SERVER.cellterminal.misc.priorityEditEnabled.set(false);menu.receiveClientAction(player,AWIds.id("cellterminal/priority"),payload);h.assertTrue(drive.getPriority()==0,"Disabled action rejected server-side");
                AWConfigs.SERVER.cellterminal.misc.priorityEditEnabled.set(true);menu.receiveClientAction(player,AWIds.id("cellterminal/priority"),payload);h.assertTrue(drive.getPriority()==1234,"Config change takes effect in already-open menu");
                var stale=payload.copy();stale.putUUID("session",UUID.randomUUID());stale.putInt("priority",999);menu.receiveClientAction(player,AWIds.id("cellterminal/priority"),stale);h.assertTrue(drive.getPriority()==1234,"Old menu nonce cannot act");
                drive.getInternalInventory().setItemDirect(0,AEItems.ITEM_CELL_4K.stack());menu.receiveClientAction(player,AWIds.id("cellterminal/pickup"),payload);h.assertTrue(menu.getCarried().isEmpty() && !drive.getInternalInventory().getStackInSlot(0).isEmpty(),"Stale cell cannot be ejected");
            }finally{AWConfigs.SERVER.cellterminal.misc.priorityEditEnabled.set(previous);menu.removed(player);player.containerMenu=player.inventoryMenu;}
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=180) public static void menuCellOperationsConserveCursorAndHonorPermissions(GameTestHelper h){
        h.setBlock(1,1,1,AEBlocks.DRIVE.block());h.setBlock(2,1,1,AEBlocks.CREATIVE_ENERGY_CELL.block());
        h.runAfterDelay(60,()->{
            var drive=(DriveBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));
            var original=AEItems.ITEM_CELL_1K.stack();drive.getInternalInventory().setItemDirect(0,original);
            var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"P1CellMoves"));
            var menu=new CellTerminalMenu(43,player.getInventory(),new TestHost(player,drive.getMainNode().getGrid()),UUID.randomUUID());player.containerMenu=menu;menu.broadcastChanges();
            var payload=new CompoundTag();payload.putUUID("session",menu.session());payload.putLong("id",100);payload.putLong("token",1);
            var cfg=AWConfigs.SERVER.cellterminal.cellOperations;boolean previous=cfg.cellEjectEnabled.get();
            try {
                cfg.cellEjectEnabled.set(false);menu.receiveClientAction(player,AWIds.id("cellterminal/pickup"),payload);
                h.assertTrue(menu.getCarried().isEmpty() && drive.getInternalInventory().getStackInSlot(0)==original,"Ejection denied without moving cursor or cell");
                cfg.cellEjectEnabled.set(true);menu.receiveClientAction(player,AWIds.id("cellterminal/pickup"),payload);
                h.assertTrue(menu.getCarried()==original && drive.getInternalInventory().getStackInSlot(0).isEmpty(),"Ejection moves exactly one original cell to cursor; held="+menu.getCarried()+", slot="+drive.getInternalInventory().getStackInSlot(0)+", active="+drive.getMainNode().getNode().isActive()+", build="+player.mayBuild()+", valid="+menu.stillValid(player)+", enabled="+AWConfigs.SERVER.cellterminal.tabs.terminalTabEnabled.get());
                payload.putLong("token",2);menu.receiveClientAction(player,AWIds.id("cellterminal/pickup"),payload);
                h.assertTrue(menu.getCarried().isEmpty() && drive.getInternalInventory().getStackInSlot(0).is(AEItems.ITEM_CELL_1K.asItem()),"Insertion returns the cell and clears cursor");
                menu.setCarried(AEItems.ITEM_CELL_4K.stack());payload.putLong("token",3);menu.receiveClientAction(player,AWIds.id("cellterminal/pickup"),payload);
                h.assertTrue(menu.getCarried().is(AEItems.ITEM_CELL_1K.asItem()) && drive.getInternalInventory().getStackInSlot(0).is(AEItems.ITEM_CELL_4K.asItem()),"Swap preserves both cells");
                menu.setCarried(new ItemStack(Items.DIAMOND));payload.putLong("token",4);payload.putInt("slot",0);
                menu.receiveClientAction(player,AWIds.id("cellterminal/partition"),payload);
                var target=new Ae2StorageTarget(200,drive,0,drive.getMainNode().getGrid());
                h.assertTrue(AEItemKey.of(Items.DIAMOND).equals(target.partitionKey(0)) && menu.getCarried().getCount()==1,"Ghost partition does not consume carried item");
            }finally{cfg.cellEjectEnabled.set(previous);menu.removed(player);player.containerMenu=player.inventoryMenu;}
            h.succeed();
        });
    }

}
