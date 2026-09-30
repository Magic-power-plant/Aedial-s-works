package com.mpp.aedialsworks.smoke;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import appeng.api.parts.PartHelper;
import appeng.api.networking.GridHelper;
import appeng.api.features.GridLinkables;
import appeng.api.config.Actionable;
import appeng.api.stacks.*;
import appeng.api.storage.StorageCells;
import appeng.core.definitions.*;
import appeng.blockentity.storage.*;
import appeng.blockentity.networking.*;
import appeng.me.helpers.PlayerSource;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.cellterminal.menu.*;
import com.mpp.aedialsworks.cellterminal.network.TerminalChannels;
import com.mpp.aedialsworks.cellterminal.part.PartCellTerminal;
import com.mpp.aedialsworks.cellterminal.integration.ae2.*;
import com.mpp.aedialsworks.common.registry.*;
import com.mpp.aedialsworks.common.config.AWConfigs;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.server.level.ServerPlayer;
/** Only included by -Pp1Smoke. Creates its own superflat save and exits after rendering both terminal hosts. */
public final class P1ClientSmoke {
    private static int phase,ticks,elapsed,tab,lastLoggedPhase=-1;
    private static long switchTarget,rootNetwork;
    private static volatile Throwable failure;
    private static volatile boolean setupDone;
    private static boolean languageReady, tempDetailsChecked;
    private static final BlockPos DRIVE=new BlockPos(0,64,0),TERMINAL=new BlockPos(0,64,1),CHEST=new BlockPos(0,64,2),WAP=new BlockPos(2,64,0);
    private static final Path REPORT=Path.of(System.getProperty("aedialsworks.smokeReportDir"),"p1");
    private static void require(boolean pass,String message){if(!pass)throw new IllegalStateException(message);}
    public static void tick() throws Exception {
        var mc=Minecraft.getInstance();if(++elapsed>4800)throw new IllegalStateException("P1 client smoke timeout at phase "+phase);
        if(failure!=null)throw new IllegalStateException("P1 server fixture failed",failure);
        ticks++;
        if(phase!=lastLoggedPhase){Aedialsworks.LOGGER.info("P1 smoke phase {}",phase);lastLoggedPhase=phase;}
        if(phase==0){
            if(!(mc.screen instanceof TitleScreen) || mc.getOverlay()!=null)return;
            if(!languageReady){
                languageReady=true;String language=System.getProperty("aedialsworks.smokeLanguage","en_us");
                if(!mc.getLanguageManager().getSelected().equals(language)){mc.getLanguageManager().setSelected(language);mc.options.languageCode=language;mc.reloadResourcePacks();return;}
            }
            Files.createDirectories(REPORT);
            var gui=AWConfigs.CLIENT.cellterminal.gui;gui.terminalStyle.set("SMALL");gui.lastViewedNetworkId.set("0");gui.subnetVisibility.set("DONT_SHOW");gui.subnetFavorites.set(List.of());gui.searchFilter.set("");gui.selectedTab.set(0);
            require(mc.getItemRenderer().getModel(new ItemStack(AWItems.CELL_TERMINAL.get()),null,null,0)!=mc.getModelManager().getMissingModel(),"Cell terminal item model missing");
            require(mc.getItemRenderer().getModel(new ItemStack(AWItems.WIRELESS_CELL_TERMINAL.get()),null,null,0)!=mc.getModelManager().getMissingModel(),"Wireless item model missing");
            for(var model:List.of(TerminalDisplayPartAdapter.ON,TerminalDisplayPartAdapter.OFF,TerminalDisplayPartAdapter.DIM))require(mc.getModelManager().getModel(model)!=mc.getModelManager().getMissingModel(),"Part model missing: "+model);
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getToasts().clear();mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(2);mc.resizeDisplay();
            var flow=mc.createWorldOpenFlows();
            if(Files.exists(Path.of("saves/P1Verification/level.dat")))flow.loadLevel(mc.screen,"P1Verification");
            else flow.createFreshLevel("P1Verification",new LevelSettings("P1Verification",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(42,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());
            phase=1;ticks=0;return;
        }
        if(phase==1){if(mc.player==null || mc.getSingleplayerServer()==null || ticks<80)return;
            mc.getSingleplayerServer().execute(()->{try{setup(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));setupDone=true;}catch(Throwable e){failure=e;}});phase=2;ticks=0;return;
        }
        if(phase==2){if(!setupDone || ticks<100)return;
            mc.getSingleplayerServer().execute(()->{try{connectAndOpen(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));}catch(Throwable e){failure=e;}});phase=3;ticks=0;return;
        }
        if(phase==3){
            if(!(mc.player.containerMenu instanceof CellTerminalMenu)){
                if(ticks%200==0)Aedialsworks.LOGGER.info("P1 client menu={}, screen={}",mc.player.containerMenu.getClass().getName(),mc.screen==null?"null":mc.screen.getClass().getName());
                if(ticks%20==0)mc.getSingleplayerServer().execute(()->{try{
                    var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    var part=(PartCellTerminal)PartHelper.getPart(player.serverLevel(),TERMINAL,Direction.SOUTH);
                    if(ticks%200==0)Aedialsworks.LOGGER.info("P1 waiting for menu: active={}, powered={}, usable={}, player={}",part.isActive(),part.isPowered(),part.canUseTerminal(player),player.position());
                    if(part.canUseTerminal(player)){
                        require(MenuOpener.open(AWMenus.CELL_TERMINAL.get(),player,MenuLocators.forPart(part)),"Native menu opener rejected an active, usable part");
                        if(ticks%200==0)Aedialsworks.LOGGER.info("P1 server menu after open={}",player.containerMenu.getClass().getName());
                    }
                }catch(Throwable e){failure=e;}});
                return;
            }
            if(ticks<25)return;org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),10,mc.getWindow().getHeight()-10);var menu=(CellTerminalMenu)mc.player.containerMenu;
            require(menu.data(TerminalChannels.STORAGES).getList("entries",Tag.TAG_COMPOUND).size()>=11,"Drive/chest snapshot did not arrive over chunked protocol");
            require(menu.data(TerminalChannels.BUSES).getList("entries",Tag.TAG_COMPOUND).size()>=2,"Storage buses missing");
            require(menu.data(TerminalChannels.TEMP_CELLS).getList("entries",Tag.TAG_COMPOUND).size()==16,"Temporary slots missing");
            require(menu.data(TerminalChannels.SUBNETS).getList("entries",Tag.TAG_COMPOUND).size()>=2,"Subnet snapshot missing");
            var screen=(com.mpp.aedialsworks.cellterminal.screen.CellTerminalScreen)mc.screen;
            var search=(net.minecraft.client.gui.components.EditBox)screen.children().stream().filter(child->child instanceof net.minecraft.client.gui.components.EditBox).findFirst().orElseThrow();
            screen.setFocused(search);search.setFocused(true);screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_E,0,0);screen.charTyped('e',0);
            require(mc.screen==screen && search.getValue().equals("e"),"Inventory key must type into search instead of closing screen");
            search.setValue("");search.setFocused(false);screen.setFocused(null);
            var cards=menu.data(TerminalChannels.META).getList("toolboxCards",Tag.TAG_COMPOUND);
            require(cards.size()==9 && ItemStack.of(cards.getCompound(1)).isEmpty() && !ItemStack.of(cards.getCompound(4)).isEmpty(),"Sparse toolbox slots must preserve their indices");
            rootNetwork=menu.data(TerminalChannels.META).getLong("rootNetworkId");
            switchTarget=menu.data(TerminalChannels.SUBNETS).getList("entries",Tag.TAG_COMPOUND).stream().map(value->(CompoundTag)value).filter(value->value.getLong("id")!=rootNetwork).findFirst().orElseThrow().getLong("id");
            var payload=new CompoundTag();payload.putInt("tab",0);menu.request("tab",payload);phase=16;ticks=0;return;
        }
        if(phase==16){
            if(ticks<20)return;
            var screen=(com.mpp.aedialsworks.cellterminal.screen.CellTerminalScreen)mc.screen;
            require(screen.panelHeight()<300,"Compact original layout should fit below 300 GUI pixels");
            require(guiButton("tab.terminal").getWidth()==22,"Original icon tabs must be 22px");
            clickGui(145,42,0);
            var priority=(net.minecraft.client.gui.components.EditBox)screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox e && e.visible).skip(1).findFirst().orElseThrow();
            priority.setValue("-7");screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0);
            phase=17;ticks=0;return;
        }
        if(phase==17){
            if(ticks<20)return;var menu=(CellTerminalMenu)mc.player.containerMenu;
            require(menu.data(TerminalChannels.STORAGES).getList("entries",Tag.TAG_COMPOUND).getCompound(0).getInt("priority")==-7,"Inline priority editor must send signed values");
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(REPORT.resolve("settings.png"));}
            var screen=(com.mpp.aedialsworks.cellterminal.screen.CellTerminalScreen)mc.screen;
            var priority=(net.minecraft.client.gui.components.EditBox)screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox e && e.visible).skip(1).findFirst().orElseThrow();
            priority.setValue("0");screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0);screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0);
            clickGui(160,61,0);phase=18;ticks=0;return;
        }
        if(phase==18){
            if(ticks<20)return;require(((CellTerminalMenu)mc.player.containerMenu).activeTab()==TerminalTab.INVENTORY,"Inline contents icon should navigate to the cell contents tab");
            guiButton("tab.terminal").onPress();phase=19;ticks=0;return;
        }
        if(phase==19){
            if(ticks<20)return;clickGui(146,61,0);phase=20;ticks=0;return;
        }
        if(phase==20){
            if(ticks<20)return;require(!mc.player.containerMenu.getCarried().isEmpty(),"Inline eject icon must pick up the mounted cell");
            clickGui(146,61,0);phase=21;ticks=0;return;
        }
        if(phase==21){
            if(ticks<20)return;require(mc.player.containerMenu.getCarried().isEmpty(),"Inline eject icon must reinsert the carried cell");
            clickGui(173,42,0);phase=22;ticks=0;return;
        }
        if(phase==22){
            if(ticks<10)return;try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(REPORT.resolve("collapsed.png"));}
            clickGui(173,42,0);phase=4;ticks=0;return;
        }
        if(phase==4){if(ticks<20)return;require(mc.screen instanceof com.mpp.aedialsworks.cellterminal.screen.CellTerminalScreen,"Terminal screen closed while drawing");
            var menu=(CellTerminalMenu)mc.player.containerMenu;require(menu.activeTab().ordinal()==tab,"Tab change did not reach server");
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(REPORT.resolve(String.format("tab-%02d-%s.png",tab,menu.activeTab().name().toLowerCase(Locale.ROOT))));}
            if(tab==3 && !tempDetailsChecked){tempDetailsChecked=true;clickGui(172,43,0);phase=23;ticks=0;return;}
            if(++tab<TerminalTab.values().length){var button=guiButton("tab."+TerminalTab.values()[tab].name().toLowerCase(Locale.ROOT));mc.screen.mouseClicked(button.getX()+11,button.getY()+11,0);mc.screen.mouseReleased(button.getX()+11,button.getY()+11,0);ticks=0;return;}
            var payload=new CompoundTag();payload.putLong("network",switchTarget);menu.request("network",payload);phase=8;ticks=0;return;
        }
        if(phase==23){
            if(ticks<15)return;require(((CellTerminalMenu)mc.player.containerMenu).activeTab()==TerminalTab.TEMP,"Temporary-cell partition must remain in its temporary inventory");
            require(guiButton("contents").visible,"Temporary partition view must offer return to contents");
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(REPORT.resolve("temp-partition.png"));}
            mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().get(0).containerMenu.setCarried(new ItemStack(Items.REDSTONE,16)));
            phase=24;ticks=0;return;
        }
        if(phase==24){
            if(ticks<15)return;require(mc.player.containerMenu.getCarried().getCount()==16,"Ghost slot fixture cursor arrived");clickGui(43,61,0);phase=25;ticks=0;return;
        }
        if(phase==25){
            if(ticks<15)return;var menu=(CellTerminalMenu)mc.player.containerMenu;
            require(!menu.data(TerminalChannels.TEMP_CELLS).getList("entries",Tag.TAG_COMPOUND).getCompound(0).getList("partition",Tag.TAG_COMPOUND).isEmpty(),"Temporary ghost slot click must update the intended temporary cell");
            require(menu.getCarried().getCount()==16,"Ghost filtering must preserve cursor count");clickGui(43,61,1);phase=26;ticks=0;return;
        }
        if(phase==26){
            if(ticks<15)return;var menu=(CellTerminalMenu)mc.player.containerMenu;
            require(menu.data(TerminalChannels.TEMP_CELLS).getList("entries",Tag.TAG_COMPOUND).getCompound(0).getList("partition",Tag.TAG_COMPOUND).isEmpty(),"Right-click must clear the temporary ghost slot");
            require(menu.getCarried().getCount()==16,"Clearing ghost filter must preserve cursor count");
            mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().get(0).containerMenu.setCarried(ItemStack.EMPTY));
            tab=4;guiButton("tab.bus_inventory").onPress();phase=4;ticks=0;return;
        }
        if(phase==8){
            if(ticks<25)return;var menu=(CellTerminalMenu)mc.player.containerMenu;
            require(menu.data(TerminalChannels.META).getLong("networkId")==switchTarget,"Subnet switch failed");
            require(menu.data(TerminalChannels.STORAGES).getList("entries",Tag.TAG_COMPOUND).size()==10,"Subnet must replace root storage rows");
            var payload=new CompoundTag();payload.putLong("network",0);menu.request("network",payload);phase=9;ticks=0;return;
        }
        if(phase==9){
            if(ticks<25)return;var menu=(CellTerminalMenu)mc.player.containerMenu;
            require(menu.data(TerminalChannels.META).getLong("networkId")==rootNetwork,"Return to root failed");
            require(menu.data(TerminalChannels.STORAGES).getList("entries",Tag.TAG_COMPOUND).size()>=11,"Root storage rows must be restored");
            var view=new CompoundTag();view.putString("visibility","SHOW_ALL");menu.request("view",view);phase=10;ticks=0;return;
        }
        if(phase==10){
            if(ticks<25)return;var menu=(CellTerminalMenu)mc.player.containerMenu;
            require(menu.data(TerminalChannels.STORAGES).getList("entries",Tag.TAG_COMPOUND).size()==21,"All-network view must combine root and subnet cells");
            var view=new CompoundTag();view.putString("visibility","SHOW_FAVORITES");menu.request("view",view);phase=11;ticks=0;return;
        }
        if(phase==11){
            if(ticks<25)return;var menu=(CellTerminalMenu)mc.player.containerMenu;
            require(menu.data(TerminalChannels.STORAGES).getList("entries",Tag.TAG_COMPOUND).size()==11,"No favorites must exclude subnet cells");
            var view=new CompoundTag();view.putString("visibility","SHOW_FAVORITES");view.putLongArray("favorites",new long[]{switchTarget});menu.request("view",view);phase=12;ticks=0;return;
        }
        if(phase==12){
            if(ticks<25)return;var menu=(CellTerminalMenu)mc.player.containerMenu;
            require(menu.data(TerminalChannels.STORAGES).getList("entries",Tag.TAG_COMPOUND).size()==21,"Favorite subnet must contribute cells");
            var view=new CompoundTag();view.putString("visibility","DONT_SHOW");menu.request("view",view);phase=13;ticks=0;return;
        }
        if(phase==13){
            if(ticks<25)return;AWConfigs.CLIENT.cellterminal.gui.terminalStyle.set("TALL");mc.options.guiScale().set(1);mc.resizeDisplay();phase=14;ticks=0;return;
        }
        if(phase==14){
            if(ticks<20)return;var screen=(com.mpp.aedialsworks.cellterminal.screen.CellTerminalScreen)mc.screen;
            require(screen.panelHeight()>312,"Tall layout must expand to available height");
            for(var slot:mc.player.containerMenu.slots)require(slot.y+16<screen.panelHeight(),"Player inventory must remain inside tall panel");
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(REPORT.resolve("tall.png"));}
            var limit=screen.children().stream().filter(child->child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text && text.getKey().equals("gui.aedialsworks.cellterminal.slots")).map(child->(net.minecraft.client.gui.components.Button)child).findFirst().orElseThrow();
            AWConfigs.CLIENT.cellterminal.gui.subnetSlotLimit.set("LIMIT_8");limit.onPress();require(AWConfigs.CLIENT.cellterminal.gui.subnetSlotLimit.get().equals("LIMIT_32"),"Slot limit button must update active-tab preference");
            AWConfigs.CLIENT.cellterminal.gui.terminalStyle.set("SMALL");mc.options.guiScale().set(2);mc.resizeDisplay();
            var payload=new CompoundTag();payload.putLong("network",switchTarget);((CellTerminalMenu)mc.player.containerMenu).request("network",payload);phase=15;ticks=0;return;
        }
        if(phase==15){
            if(ticks<25)return;require(((CellTerminalMenu)mc.player.containerMenu).data(TerminalChannels.META).getLong("networkId")==switchTarget,"Subnet selection before reopening");
            mc.player.closeContainer();require(Long.parseLong(AWConfigs.CLIENT.cellterminal.gui.lastViewedNetworkId.get())==switchTarget,"Save last viewed network");phase=5;ticks=0;return;
        }
        if(phase==5){if(ticks<30)return;mc.getSingleplayerServer().execute(()->{try{var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);AWItems.WIRELESS_CELL_TERMINAL.get().openFromInventory(player,0);}catch(Throwable e){failure=e;}});phase=6;ticks=0;return;}
        if(phase==6){if(ticks<30 || !(mc.player.containerMenu instanceof CellTerminalMenu menu))return;
            require(menu.getTarget() instanceof WirelessCellTerminalHost,"Wireless menu host not selected");require(menu.data(TerminalChannels.META).getLong("networkId")==switchTarget,"New wireless menu restores saved reachable subnet");require(!menu.data(TerminalChannels.STORAGES).isEmpty(),"Wireless snapshot missing");
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(REPORT.resolve("wireless.png"));}
            Files.writeString(REPORT.resolve("client.txt"),"PASS: both item models, all three part models, powered cable terminal, Drive/Chest/StorageBus snapshots, all 16 temp slots, bidirectional subnet discovery, eight original-style icon tabs, inline priority editing, cell eject/reinsert clicks, group collapse, temporary-cell ghost partition clicks with cursor conservation, editable search keyboard focus, sparse toolbox indices, subnet switch/return, all/favorite subnet aggregation, tall layout, slot-limit control, saved subnet restoration, wireless access-point binding and remote menu.\n",StandardCharsets.UTF_8);
            Aedialsworks.LOGGER.info("P1_CLIENT_SMOKE_PASS: eight tabs, models, chunked data and wireless menu verified");phase=7;mc.stop();
        }
    }
    private static net.minecraft.client.gui.components.Button guiButton(String key){
        return Minecraft.getInstance().screen.children().stream().filter(child->child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text && text.getKey().equals("gui.aedialsworks.cellterminal."+key)).map(child->(net.minecraft.client.gui.components.Button)child).findFirst().orElseThrow();
    }
    private static void clickGui(int x,int y,int mouseButton){
        var tab=guiButton("tab.terminal");var screen=Minecraft.getInstance().screen;
        int screenX=tab.getX()-4+x,screenY=tab.getY()+22+y;
        screen.mouseClicked(screenX,screenY,mouseButton);screen.mouseReleased(screenX,screenY,mouseButton);
    }
    private static void setup(ServerPlayer player){
        var level=player.serverLevel();player.getInventory().clearContent();player.containerMenu.setCarried(ItemStack.EMPTY);player.teleportTo(level,0.5,65,4.5,180,15);player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
        for(int x=-2;x<=8;x++)for(int z=-2;z<=6;z++)level.setBlockAndUpdate(new BlockPos(x,63,z),Blocks.SMOOTH_STONE.defaultBlockState());
        for(int x=0;x<=6;x++)for(int z=0;z<=3;z++)level.setBlockAndUpdate(new BlockPos(x,64,z),Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(DRIVE,AEBlocks.DRIVE.block().defaultBlockState());level.setBlockAndUpdate(new BlockPos(1,64,0),AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        level.setBlockAndUpdate(CHEST,AEBlocks.CHEST.block().defaultBlockState());level.setBlockAndUpdate(WAP,AEBlocks.WIRELESS_ACCESS_POINT.block().defaultBlockState());
        var part=PartHelper.setPart(level,TERMINAL,Direction.SOUTH,player,AWItems.CELL_TERMINAL.get());
        PartHelper.setPart(level,new BlockPos(2,64,2),Direction.EAST,player,AEParts.STORAGE_BUS.asItem());level.setBlockAndUpdate(new BlockPos(3,64,2),Blocks.BARREL.defaultBlockState());
        ((BarrelBlockEntity)level.getBlockEntity(new BlockPos(3,64,2))).setItem(0,new ItemStack(Items.GOLD_INGOT,32));
        PartHelper.setPart(level,new BlockPos(4,64,2),Direction.EAST,player,AEParts.STORAGE_BUS.asItem());level.setBlockAndUpdate(new BlockPos(5,64,2),AEBlocks.INTERFACE.block().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(5,64,3),AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());level.setBlockAndUpdate(new BlockPos(6,64,2),AEBlocks.DRIVE.block().defaultBlockState());
        var drive=(DriveBlockEntity)level.getBlockEntity(DRIVE);var source=new PlayerSource(player);var cell=AEItems.ITEM_CELL_1K.stack();var inventory=StorageCells.getCellInventory(cell,null);
        inventory.insert(AEItemKey.of(Items.IRON_INGOT),1234,Actionable.MODULATE,source);inventory.insert(AEItemKey.of(Items.DIAMOND),64,Actionable.MODULATE,source);inventory.persist();drive.getInternalInventory().setItemDirect(0,cell);
        drive.getInternalInventory().setItemDirect(1,AEItems.ITEM_CELL_4K.stack());drive.getInternalInventory().setItemDirect(2,AEItems.FLUID_CELL_4K.stack());
        ((ChestBlockEntity)level.getBlockEntity(CHEST)).setCell(AEItems.ITEM_CELL_16K.stack());part.temporaryCells().setItemDirect(0,AEItems.ITEM_CELL_64K.stack());
        // Fixture resets can drop old AE2 contents; discard them only inside this isolated test plot.
        for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-5,60,-5,12,75,12)))entity.discard();
        var wireless=new ItemStack(AWItems.WIRELESS_CELL_TERMINAL.get());AWItems.WIRELESS_CELL_TERMINAL.get().injectAEPower(wireless,10000,Actionable.MODULATE);GridLinkables.get(wireless.getItem()).link(wireless,GlobalPos.of(level.dimension(),WAP));player.getInventory().setItem(0,wireless);
        player.getInventory().setItem(1,AEItems.FUZZY_CARD.stack());player.getInventory().setItem(2,AEItems.INVERTER_CARD.stack());player.getInventory().setItem(3,AEItems.NETWORK_TOOL.stack());
        var tool=ToolboxAdapter.inventory(player,3);tool.setItemDirect(0,AEItems.FUZZY_CARD.stack());tool.setItemDirect(4,AEItems.INVERTER_CARD.stack());
    }
    private static void connectAndOpen(ServerPlayer player){
        var level=player.serverLevel();var drive=(DriveBlockEntity)level.getBlockEntity(DRIVE);var root=drive.getMainNode().getNode();
        var part=(PartCellTerminal)PartHelper.getPart(level,TERMINAL,Direction.SOUTH);
        var chest=(ChestBlockEntity)level.getBlockEntity(CHEST);var wap=(WirelessAccessPointBlockEntity)level.getBlockEntity(WAP);
        var bus=PartHelper.getPart(level,new BlockPos(2,64,2),Direction.EAST);var subnetBus=PartHelper.getPart(level,new BlockPos(4,64,2),Direction.EAST);
        for(var node:List.of(part.getGridNode(),chest.getMainNode().getNode(),wap.getMainNode().getNode(),bus.getGridNode(),subnetBus.getGridNode()))if(node.getGrid()!=root.getGrid())GridHelper.createConnection(root,node);
        // The next client phase opens after this grid's channels have settled.
    }
}
