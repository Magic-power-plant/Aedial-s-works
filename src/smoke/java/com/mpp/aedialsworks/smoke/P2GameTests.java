package com.mpp.aedialsworks.smoke;
import java.util.*;
import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.*;
import appeng.api.storage.StorageCells;
import appeng.core.definitions.*;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.blockentity.grid.AENetworkBlockEntity;
import com.mpp.aedialsworks.common.registry.*;
import com.mpp.aedialsworks.common.util.AWIds;
import com.mpp.aedialsworks.powertools.*;
import com.mpp.aedialsworks.powertools.integration.ae2.*;
import com.mpp.aedialsworks.powertools.crafter.*;
import com.mpp.aedialsworks.powertools.maintainer.*;
import com.mpp.aedialsworks.powertools.monitor.*;
import com.mpp.aedialsworks.powertools.menu.PowerToolsMenu;
import com.mpp.aedialsworks.powertools.scanner.NetworkScanner;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.*;
@GameTestHolder("aedialsworks") @PrefixGameTestTemplate(false)
public final class P2GameTests {
    private static final IActionSource SOURCE=IActionSource.empty();
    private static void network(GameTestHelper h,net.minecraft.world.level.block.Block machine){
        h.setBlock(1,1,1,machine);h.setBlock(2,1,1,AEBlocks.DRIVE.block());h.setBlock(2,0,1,AEBlocks.CREATIVE_ENERGY_CELL.block());
        var drive=(DriveBlockEntity)h.getBlockEntity(new BlockPos(2,1,1));drive.getInternalInventory().setItemDirect(0,AEItems.ITEM_CELL_64K.stack());
        h.runAfterDelay(25,()->{var main=(AENetworkBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));for(var pos:List.of(new BlockPos(2,1,1),new BlockPos(2,0,1))){var be=(appeng.me.helpers.IGridConnectedBlockEntity)h.getBlockEntity(pos);if(main.getMainNode().getGrid()!=be.getMainNode().getGrid())GridHelper.createConnection(main.getMainNode().getNode(),be.getMainNode().getNode());}});
    }
    private static PowerBlockEntity machine(GameTestHelper h){return (PowerBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));}
    private static ItemStack encode(GameTestHelper h,String id,ItemStack[] inputs){
        var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation(id)).orElseThrow();
        return PatternDetailsHelper.encodeCraftingPattern(recipe,inputs,recipe.getResultItem(h.getLevel().registryAccess()),false,false);
    }
    private static ItemStack[] emptyGrid(){var in=new ItemStack[9];Arrays.fill(in,ItemStack.EMPTY);return in;}
    @GameTest(template="empty") public static void registrationRecipesAndLongMonitorSemantics(GameTestHelper h){
        h.assertTrue(AWItems.ITEMS.getEntries().size()>=21,"P1 two items plus P2 nineteen items");
        for(var e:AWBlocks.BLOCKS.getEntries())if(!AWCells.BLOCKS.containsValue(e))h.assertTrue(h.getLevel().getRecipeManager().byKey(e.getId()).isPresent(),"Every machine has a recipe: "+e.getId());
        var m=new MonitorSettings();var a=m.entries[0];a.key=AEItemKey.of(Items.IRON_INGOT);a.threshold=1L<<40;a.resetThreshold=a.threshold+100;
        h.assertTrue(m.evaluate(k->0,true),"Default LESS triggers below threshold");h.assertFalse(m.evaluate(k->Long.MAX_VALUE,true),"Long quantities never overflow");
        m.entries[1].key=AEFluidKey.of(Fluids.WATER);m.entries[1].threshold=1000;
        h.assertFalse(m.evaluate(k->k.equals(a.key)?Long.MAX_VALUE:0,true),"AND across item/fluid");m.any=true;h.assertTrue(m.evaluate(k->k.equals(a.key)?Long.MAX_VALUE:0,true),"OR across item/fluid");
        h.assertFalse(m.evaluate(k->0,false),"Offline monitor never emits");
        m.any=false;m.entries[1].key=null;m.hysteresis=true;a.threshold=64;a.resetThreshold=96;a.met=false;
        h.assertTrue(m.evaluate(k->32,true)&&m.evaluate(k->80,true),"Hysteresis holds alarm until reset bound");h.assertFalse(m.evaluate(k->96,true),"Reset bound clears alarm");a.threshold=1L<<40;
        var blank=new PowerBlockEntity(AWBlockEntities.AUTO_CRAFTER.get(),BlockPos.ZERO,AWBlocks.AUTO_CRAFTER.get().defaultBlockState());blank.logic().load(new CompoundTag());h.assertTrue(((CrafterLogic)blank.logic()).patterns.getSlots()==12&&((CrafterLogic)blank.logic()).upgrades.getSlots()==4,"Empty legacy NBT preserves inventory topology");var n=new CompoundTag();m.save(n);var copy=new MonitorSettings();copy.load(n);h.assertTrue(copy.entries[0].threshold==1L<<40,"Long target persists");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=180) public static void crafterTwelveSlotsSpeedAndConservation(GameTestHelper h){
        network(h,AWBlocks.AUTO_CRAFTER.get());
        h.runAfterDelay(60,()->{var be=machine(h);var logic=(CrafterLogic)be.logic();var in=emptyGrid();Arrays.fill(in,new ItemStack(Items.IRON_INGOT));
            logic.patterns.setStackInSlot(11,encode(h,"minecraft:iron_block",in));logic.upgrades.setStackInSlot(3,new ItemStack(AWItems.CRAFTER_SPEED_UPGRADE_I.get()));
            h.assertTrue(logic.patterns.getSlots()==12&&logic.multiplier()==8,"Twelve real pattern slots and tier I multiplier");
            var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"P2Preview"));
            player.setPos(be.getBlockPos().getX()+.5,be.getBlockPos().getY()+.5,be.getBlockPos().getZ()+.5);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var menu=new PowerToolsMenu(61,player.getInventory(),be,UUID.randomUUID());player.containerMenu=menu;menu.refreshRecipePreview(11);
            var result=menu.getSlots(appeng.menu.SlotSemantics.CRAFTING_RESULT).get(0);
            h.assertTrue(menu.slots.size()==71&&GenericStack.fromItemStack(result.getItem()).what().equals(AEItemKey.of(Items.IRON_BLOCK)),"Preview represents actual selected recipe output");
            menu.clicked(result.index,0,net.minecraft.world.inventory.ClickType.PICKUP,player);menu.setFilter(result.index,new ItemStack(Items.DIRT));
            h.assertTrue(menu.getCarried().isEmpty()&&GenericStack.fromItemStack(result.getItem()).what().equals(AEItemKey.of(Items.IRON_BLOCK)),"Recipe previews cannot be extracted or changed through native slot packets");
            player.containerMenu=player.inventoryMenu;
            be.getMainNode().getGrid().getStorageService().getInventory().insert(AEItemKey.of(Items.IRON_INGOT),72,Actionable.MODULATE,SOURCE);
        });
        h.runAfterDelay(130,()->{var inv=machine(h).getMainNode().getGrid().getStorageService().getCachedInventory();h.assertTrue(inv.get(AEItemKey.of(Items.IRON_BLOCK))==8,"Slot 12 executes all eight crafts");h.assertTrue(inv.get(AEItemKey.of(Items.IRON_INGOT))==0,"Exactly 72 iron consumed");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=180) public static void reusableCatalystSurvivesAutoCrafting(GameTestHelper h){
        network(h,AWBlocks.AUTO_CRAFTER.get());
        h.runAfterDelay(60,()->{var be=machine(h);var logic=(CrafterLogic)be.logic();var in=emptyGrid();in[0]=new ItemStack(AWItems.STORAGE_LEVEL_ALARM.get());in[1]=new ItemStack(Items.COMPASS);in[2]=AEBlocks.SMOOTH_SKY_STONE_BLOCK.stack();
            logic.patterns.setStackInSlot(0,encode(h,"aedialsworks:storage_level_alarm_locator",in));var inv=be.getMainNode().getGrid().getStorageService().getInventory();
            for(int i=0;i<3;i++)inv.insert(AEItemKey.of(in[i]),i==0?1:3,Actionable.MODULATE,SOURCE);
        });
        h.runAfterDelay(150,()->{var inv=machine(h).getMainNode().getGrid().getStorageService().getCachedInventory();h.assertTrue(inv.get(AEItemKey.of(AWItems.STORAGE_LEVEL_ALARM_LOCATOR.get()))==3,"Three locator results returned");h.assertTrue(inv.get(AEItemKey.of(AWItems.STORAGE_LEVEL_ALARM.get()))==1,"Catalyst remains exactly once");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=180) public static void watcherSignalAndPartPersistence(GameTestHelper h){
        network(h,AWBlocks.STORAGE_LEVEL_EMITTER.get());
        h.runAfterDelay(60,()->{var be=machine(h);var monitor=(MonitorLogic)be.logic();monitor.settings.entries[0].key=AEItemKey.of(Items.IRON_INGOT);monitor.settings.entries[0].threshold=64;monitor.settings.strength=7;monitor.refresh();h.assertTrue(be.signal()==7,"Configured redstone strength");
            be.getMainNode().getGrid().getStorageService().getInventory().insert(AEItemKey.of(Items.IRON_INGOT),64,Actionable.MODULATE,SOURCE);
            for(var item:List.of(AWItems.STORAGE_LEVEL_EMITTER_PART.get(),AWItems.STORAGE_DISPLAY_PART.get(),AWItems.STORAGE_DISPLAY_PART_SMALLER.get(),AWItems.STORAGE_DISPLAY_PART_SMALLERER.get())){var part=item.createPart();part.logic.settings.entries[23].key=AEFluidKey.of(Fluids.WATER);part.logic.settings.entries[23].threshold=1L<<40;var n=new CompoundTag();part.writeToNBT(n);var copy=item.createPart();copy.readFromNBT(n);h.assertTrue(copy.logic.settings.entries[23].threshold==1L<<40,"All part types persist 24th fluid entry");}
        });
        h.runAfterDelay(70,()->{var p=h.makeMockPlayer();var part=PartHelper.setPart(h.getLevel(),h.absolutePos(new BlockPos(1,1,3)),Direction.NORTH,p,AWItems.STORAGE_LEVEL_EMITTER_PART.get());part.logic.settings.entries[0].key=AEItemKey.of(Items.IRON_INGOT);part.logic.settings.entries[0].threshold=64;part.logic.settings.strength=11;});
        h.runAfterDelay(85,()->{var part=(MonitorPart)PartHelper.getPart(h.getLevel(),h.absolutePos(new BlockPos(1,1,3)),Direction.NORTH);GridHelper.createConnection(machine(h).getMainNode().getNode(),part.getGridNode());});
        h.runAfterDelay(135,()->{h.assertTrue(machine(h).signal()==0,"Network update turns emitter off");var part=(MonitorPart)PartHelper.getPart(h.getLevel(),h.absolutePos(new BlockPos(1,1,3)),Direction.NORTH);part.logic.refresh();h.assertTrue(part.isProvidingWeakPower()==0,"Part reads stock threshold");part.logic.settings.entries[0].threshold=65;part.logic.refresh();h.assertTrue(part.isProvidingStrongPower()==11,"Part exposes configured strong redstone");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=180) public static void menuSessionPermissionsAndScanner(GameTestHelper h){
        network(h,AWBlocks.BETTER_LEVEL_MAINTAINER.get());h.runAfterDelay(65,()->{var be=machine(h);var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"P2MenuSecurity"));player.setPos(be.getBlockPos().getX()+.5,be.getBlockPos().getY()+.5,be.getBlockPos().getZ()+.5);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var nonce=UUID.randomUUID();var menu=new PowerToolsMenu(63,player.getInventory(),be,nonce);player.containerMenu=menu;var n=new CompoundTag();n.putInt("slot",23);n.put("key",AEItemKey.of(Items.DIAMOND).toTagGeneric());n.putLong("threshold",1L<<40);n.putLong("batch",1);n.putBoolean("enabled",true);n.putUUID("session",UUID.randomUUID());
            menu.receiveClientAction(player,AWIds.id("entry"),n);h.assertTrue(((MaintainerLogic)be.logic()).entries[23].key==null,"Stale nonce rejected");n.putUUID("session",nonce);menu.receiveClientAction(player,AWIds.id("entry"),n);h.assertTrue(((MaintainerLogic)be.logic()).entries[23].threshold==1L<<40,"Valid long configuration accepted");
            menu.data=be.configuration().snapshot();menu.refreshConfigSlots();
            var config=menu.getConfigurationSlots();h.assertTrue(config.size()==24,"Native AE2 configuration slots are present");
            var icon=GenericStack.fromItemStack(config.get(23).getItem());h.assertTrue(icon!=null&&icon.what().equals(AEItemKey.of(Items.DIAMOND)),"Config slot reflects authoritative entry");
            menu.setFilter(config.get(23).index,new ItemStack(Items.DIRT));
            menu.doAction(player,appeng.helpers.InventoryAction.SET_FILTER,config.get(23).index,0);
            menu.clicked(config.get(23).index,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
            h.assertTrue(GenericStack.fromItemStack(config.get(23).getItem()).what().equals(AEItemKey.of(Items.DIAMOND))&&menu.getCarried().isEmpty(),"Native slot actions cannot mutate or extract snapshot resources");
            player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);n.putLong("threshold",1);menu.receiveClientAction(player,AWIds.id("entry"),n);h.assertTrue(((MaintainerLogic)be.logic()).entries[23].threshold==1L<<40,"Live permission checked");player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var scan=new NetworkScanner().scan(be.getMainNode().getGrid());h.assertTrue(scan.getList("nodes",Tag.TAG_COMPOUND).size()>=3&&NetworkScanner.TABS.size()==6,"Public graph scanner lists devices and six diagnostics");
            player.setPos(100000,100,100000);h.assertFalse(menu.stillValid(player),"Distance rechecked");player.containerMenu=player.inventoryMenu;h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=500) public static void maintainerCraftingReturnsToNetwork(GameTestHelper h){
        network(h,AWBlocks.BETTER_LEVEL_MAINTAINER.get());
        h.setBlock(3,1,1,AEBlocks.PATTERN_PROVIDER.block());h.setBlock(3,1,2,AEBlocks.MOLECULAR_ASSEMBLER.block());h.setBlock(4,1,1,AEBlocks.CRAFTING_STORAGE_1K.block());
        h.runAfterDelay(45,()->{var node=machine(h).getMainNode().getNode();for(var pos:List.of(new BlockPos(3,1,1),new BlockPos(4,1,1))){var be=(appeng.me.helpers.IGridConnectedBlockEntity)h.getBlockEntity(pos);if(be.getMainNode().getGrid()!=node.getGrid())GridHelper.createConnection(node,be.getMainNode().getNode());}});
        h.runAfterDelay(80,()->{var in=emptyGrid();Arrays.fill(in,new ItemStack(Items.IRON_INGOT));var provider=(appeng.helpers.patternprovider.PatternProviderLogicHost)h.getBlockEntity(new BlockPos(3,1,1));provider.getLogic().getPatternInv().setItemDirect(0,encode(h,"minecraft:iron_block",in));
            var be=machine(h);be.getMainNode().getGrid().getStorageService().getInventory().insert(AEItemKey.of(Items.IRON_INGOT),18,Actionable.MODULATE,SOURCE);
            var logic=(MaintainerLogic)be.logic();logic.entries[0].key=AEItemKey.of(Items.IRON_BLOCK);logic.entries[0].threshold=2;logic.batch[0]=2;
        });
        h.succeedWhen(()->{h.assertTrue(h.getTick()>100,"Wait for network crafting");var be=machine(h);h.assertTrue(be.getMainNode().getGrid().getStorageService().getCachedInventory().get(AEItemKey.of(Items.IRON_BLOCK))==2,"Actual CPU crafted and returned two iron blocks");h.assertTrue(((MaintainerLogic)be.logic()).getRequestedJobs().isEmpty(),"Completed crafting link is retired");});
    }
    @GameTest(template="empty",timeoutTicks=600) public static void maintainerLinkRejoinsCpuAfterReload(GameTestHelper h){
        network(h,AWBlocks.BETTER_LEVEL_MAINTAINER.get());h.setBlock(3,1,1,AEBlocks.PATTERN_PROVIDER.block());h.setBlock(3,1,2,net.minecraft.world.level.block.Blocks.CHEST);h.setBlock(4,1,1,AEBlocks.CRAFTING_STORAGE_1K.block());
        h.runAfterDelay(45,()->{var node=machine(h).getMainNode().getNode();for(var pos:List.of(new BlockPos(3,1,1),new BlockPos(4,1,1))){var be=(appeng.me.helpers.IGridConnectedBlockEntity)h.getBlockEntity(pos);if(be.getMainNode().getGrid()!=node.getGrid())GridHelper.createConnection(node,be.getMainNode().getNode());}});
        final UUID[] craftId={null};
        h.startSequence().thenExecuteAfter(80,()->{var provider=(appeng.helpers.patternprovider.PatternProviderLogicHost)h.getBlockEntity(new BlockPos(3,1,1));provider.getLogic().getPatternInv().setItemDirect(0,PatternDetailsHelper.encodeProcessingPattern(new GenericStack[]{new GenericStack(AEItemKey.of(Items.IRON_INGOT),1)},new GenericStack[]{new GenericStack(AEItemKey.of(Items.DIAMOND),1)}));
            var be=machine(h);be.getMainNode().getGrid().getStorageService().getInventory().insert(AEItemKey.of(Items.IRON_INGOT),1,Actionable.MODULATE,SOURCE);var logic=(MaintainerLogic)be.logic();logic.entries[0].key=AEItemKey.of(Items.DIAMOND);logic.entries[0].threshold=1;logic.batch[0]=1;
        }).thenWaitUntil(()->h.assertTrue(!((MaintainerLogic)machine(h).logic()).getRequestedJobs().isEmpty(),"CPU submitted a live processing job"))
        .thenExecute(()->{var be=machine(h);var logic=(MaintainerLogic)be.logic();craftId[0]=logic.getRequestedJobs().iterator().next().getCraftingID();var saved=be.saveWithoutMetadata();be.setRemoved();
            var replacement=new PowerBlockEntity(AWBlockEntities.BETTER_LEVEL_MAINTAINER.get(),be.getBlockPos(),be.getBlockState());replacement.load(saved);h.getLevel().setBlockEntity(replacement);
            h.assertTrue(((MaintainerLogic)replacement.logic()).getRequestedJobs().iterator().next().getCraftingID().equals(craftId[0]),"Persistent link UUID restored");
        }).thenExecuteAfter(45,()->{var be=machine(h);var drive=(DriveBlockEntity)h.getBlockEntity(new BlockPos(2,1,1));if(be.getMainNode().getGrid()!=drive.getMainNode().getGrid())GridHelper.createConnection(be.getMainNode().getNode(),drive.getMainNode().getNode());})
        .thenExecuteAfter(35,()->{var be=machine(h);be.getMainNode().getGrid().getStorageService().getInventory().insert(AEItemKey.of(Items.DIAMOND),1,Actionable.MODULATE,SOURCE);})
        .thenWaitUntil(()->{var be=machine(h);h.assertTrue(((MaintainerLogic)be.logic()).getRequestedJobs().isEmpty(),"Restored requester rejoined and completed CPU link");h.assertTrue(be.getMainNode().getGrid().getStorageService().getCachedInventory().get(AEItemKey.of(Items.DIAMOND))==1,"Completed output returned once");}).thenSucceed();
    }
    @GameTest(template="empty") public static void reusableRecipeCraftingGridAndWireRoundtrip(GameTestHelper h){
        var recipe=(com.mpp.aedialsworks.common.recipe.ShapelessReusableRecipe)h.getLevel().getRecipeManager().byKey(AWIds.id("storage_level_alarm_locator")).orElseThrow();
        var frame=new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player p){return false;}},3,3);
        frame.setItem(8,new ItemStack(AWItems.STORAGE_LEVEL_ALARM.get()));frame.setItem(4,new ItemStack(Items.COMPASS));frame.setItem(0,AEBlocks.SMOOTH_SKY_STONE_BLOCK.stack());
        h.assertTrue(recipe.matches(frame,h.getLevel()),"Shapeless catalyst matches arbitrary grid positions");h.assertTrue(recipe.getRemainingItems(frame).get(8).is(AWItems.STORAGE_LEVEL_ALARM.get()),"Catalyst is returned in its actual slot");
        var buf=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{AWRecipeSerializers.SHAPELESS_REUSABLE.get().toNetwork(buf,recipe);var copy=AWRecipeSerializers.SHAPELESS_REUSABLE.get().fromNetwork(recipe.getId(),buf);h.assertTrue(copy.getRemainingItems(frame).get(8).is(AWItems.STORAGE_LEVEL_ALARM.get()),"Recipe sync preserves reusable indices");}finally{buf.release();}h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=200) public static void fullStorageBuffersAndPersistsCraftedOutput(GameTestHelper h){
        network(h,AWBlocks.AUTO_CRAFTER.get());h.runAfterDelay(60,()->{var be=machine(h);var drive=(DriveBlockEntity)h.getBlockEntity(new BlockPos(2,1,1));new com.mpp.aedialsworks.cellterminal.integration.ae2.Ae2StorageTarget(1,drive,0,null).setPartition(0,AEItemKey.of(Items.IRON_INGOT));
            var in=emptyGrid();Arrays.fill(in,new ItemStack(Items.IRON_INGOT));((CrafterLogic)be.logic()).patterns.setStackInSlot(0,encode(h,"minecraft:iron_block",in));be.getMainNode().getGrid().getStorageService().getInventory().insert(AEItemKey.of(Items.IRON_INGOT),9,Actionable.MODULATE,SOURCE);
        });
        h.runAfterDelay(105,()->{var be=machine(h);var saved=be.saveWithoutMetadata();h.assertTrue(saved.getCompound("power").getList("pending",Tag.TAG_COMPOUND).size()==1,"Rejected output is durably buffered");var restored=new PowerBlockEntity(AWBlockEntities.AUTO_CRAFTER.get(),be.getBlockPos(),be.getBlockState());restored.load(saved);var drops=new ArrayList<ItemStack>();((CrafterLogic)restored.logic()).drops(drops);h.assertTrue(drops.stream().filter(x->x.is(Items.IRON_BLOCK)).mapToInt(ItemStack::getCount).sum()==1,"Reloaded buffer drops exactly one crafted output");
            var drive=(DriveBlockEntity)h.getBlockEntity(new BlockPos(2,1,1));drive.getInternalInventory().setItemDirect(1,AEItems.ITEM_CELL_1K.stack());
        });
        h.runAfterDelay(155,()->{var be=machine(h);h.assertTrue(be.getMainNode().getGrid().getStorageService().getCachedInventory().get(AEItemKey.of(Items.IRON_BLOCK))==1,"Buffered output flushes when storage recovers");h.assertTrue(be.saveWithoutMetadata().getCompound("power").getList("pending",Tag.TAG_COMPOUND).isEmpty(),"No duplicate remains in buffer");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=180) public static void cardsDistributionConservesInventory(GameTestHelper h){
        network(h,AWBlocks.STORAGE_DISPLAY.get());h.setBlock(3,1,1,AEBlocks.MOLECULAR_ASSEMBLER.block());
        h.runAfterDelay(45,()->{var a=(appeng.blockentity.crafting.MolecularAssemblerBlockEntity)h.getBlockEntity(new BlockPos(3,1,1));var node=machine(h).getMainNode().getNode();if(a.getMainNode().getGrid()!=node.getGrid())GridHelper.createConnection(a.getMainNode().getNode(),node);});
        h.runAfterDelay(75,()->{var p=h.makeMockSurvivalPlayer();p.getInventory().setItem(0,AEItems.SPEED_CARD.stack(12));int n=com.mpp.aedialsworks.powertools.items.NetworkToolItem.distribute(p,machine(h));var assembler=(appeng.blockentity.crafting.MolecularAssemblerBlockEntity)h.getBlockEntity(new BlockPos(3,1,1));h.assertTrue(n==5&&assembler.getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD)==5,"Assembler receives all five supported cards");h.assertTrue(p.getInventory().getItem(0).getCount()==7,"Only inserted cards leave player inventory");h.assertTrue(com.mpp.aedialsworks.powertools.items.NetworkToolItem.distribute(p,machine(h))==0,"Second distribution cannot duplicate cards");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=180) public static void remoteRangePowerAndAlarmBindings(GameTestHelper h){
        network(h,AWBlocks.STORAGE_LEVEL_ALARM.get());h.setBlock(3,1,1,AEBlocks.WIRELESS_ACCESS_POINT.block());
        h.runAfterDelay(45,()->{var a=(appeng.blockentity.networking.WirelessAccessPointBlockEntity)h.getBlockEntity(new BlockPos(3,1,1));var node=machine(h).getMainNode().getNode();if(a.getMainNode().getGrid()!=node.getGrid())GridHelper.createConnection(a.getMainNode().getNode(),node);});
        h.runAfterDelay(80,()->{var p=h.makeMockSurvivalPlayer();var pos=h.absolutePos(new BlockPos(3,1,1));p.setPos(pos.getX(),pos.getY()+1,pos.getZ());var item=AWItems.REMOTE_STORAGE_MONITOR.get();var stack=new ItemStack(item);item.injectAEPower(stack,2000,Actionable.MODULATE);p.getInventory().setItem(0,stack);appeng.api.features.GridLinkables.get(item).link(stack,GlobalPos.of(h.getLevel().dimension(),pos));
            var host=new WirelessPowerHost(p,0,stack,(player,menu)->{});h.assertTrue(host.canUsePower(p),"Bound remote works within access-point range");double before=item.getAECurrentPower(stack);h.assertTrue(host.pollHud()&&item.getAECurrentPower(stack)<before,"HUD polling consumes distance-based power");p.setPos(pos.getX()+10000,pos.getY(),pos.getZ());h.assertFalse(host.canUsePower(p),"Out-of-range remote cannot inspect network");
            var logic=(MonitorLogic)machine(h).logic();h.assertTrue(logic.toggleBinding(p.getUUID()),"Alarm binds current player");var tag=new CompoundTag();logic.save(tag);h.assertTrue(tag.getList("subscribers",Tag.TAG_COMPOUND).size()==1,"Binding persists");logic.load(tag);h.assertFalse(logic.toggleBinding(p.getUUID()),"Same player unbinds without duplicating subscription");h.succeed();
        });
    }
}
