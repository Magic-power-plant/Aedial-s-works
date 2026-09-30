package com.mpp.aedialsworks.smoke;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import appeng.api.parts.PartHelper;
import appeng.api.networking.GridHelper;
import appeng.api.features.GridLinkables;
import appeng.api.config.Actionable;
import appeng.api.stacks.*;
import appeng.core.definitions.*;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.mpp.aedialsworks.common.registry.*;
import com.mpp.aedialsworks.powertools.integration.ae2.*;
import com.mpp.aedialsworks.powertools.monitor.*;
import com.mpp.aedialsworks.powertools.menu.PowerToolsMenu;
import com.mpp.aedialsworks.powertools.client.PowerToolsScreen;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.server.level.ServerPlayer;
/** Isolated, opt-in client acceptance fixture; never included in the production jar. */
public final class P2ClientSmoke {
    private static int phase,ticks,total,index,nativeInputStage,layoutStage;
    private static volatile Throwable failure;
    private static volatile boolean ready,restartDone,nativeRoundTripChecked;
    private static boolean language,interactionChecked;
    public static volatile Runnable jeiShow;
    private static final Path REPORT=Path.of(System.getProperty("aedialsworks.smokeReportDir"),"p2",System.getProperty("aedialsworks.smokeLanguage","en_us"));
    private static final List<String> NAMES=List.of("maintainer","crafter","emitter","display","alarm","emitter_part","display_part","small_display","tiny_display","scanner","locator","priority","remote");
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static void server(java.util.function.Consumer<ServerPlayer> work){var server=Minecraft.getInstance().getSingleplayerServer();server.execute(()->{try{work.accept(server.getPlayerList().getPlayers().get(0));}catch(Throwable e){failure=e;}});}
    public static void tick() throws Exception {
        var mc=Minecraft.getInstance();ticks++;if(++total>4800)throw new IllegalStateException("P2 smoke timed out, phase="+phase+" index="+index);if(failure!=null)throw new IllegalStateException("P2 server fixture",failure);
        if((phase==4||phase==5)&&mc.player!=null){mc.mouseHandler.releaseMouse();mc.player.setYRot(0);mc.player.yRotO=0;mc.player.setXRot(20);mc.player.xRotO=20;}
        if(phase==0){
            if(mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")){mc.setScreen(new TitleScreen());return;}
            if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
            if(!language){language=true;String lang=System.getProperty("aedialsworks.smokeLanguage","en_us");if(!mc.getLanguageManager().getSelected().equals(lang)){mc.getLanguageManager().setSelected(lang);mc.options.languageCode=lang;mc.reloadResourcePacks();return;}}
            Files.createDirectories(REPORT);
            for(String texture:List.of("powertools/blocks/maintainer_top","powertools/blocks/monitor_front","powertools/items/crafter_speed_upgrade_iv","powertools/parts/part_side"))require(!mc.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(com.mpp.aedialsworks.common.util.AWIds.id(texture)).contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()),"Missing atlas texture "+texture);
            for(var item:AWItems.ITEMS.getEntries())require(mc.getItemRenderer().getModel(new ItemStack(item.get()),null,null,0)!=mc.getModelManager().getMissingModel(),"Missing item model "+item.getId());
            for(var id:List.of(MonitorPart.model(true,0),MonitorPart.model(false,0),MonitorPart.model(false,1),MonitorPart.model(false,2)))require(mc.getModelManager().getModel(com.mpp.aedialsworks.common.util.AWIds.id(id))!=mc.getModelManager().getMissingModel(),"Missing part model "+id);
            mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.pauseOnLostFocus=false;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            String worldName=System.getProperty("aedialsworks.restartSmoke","").isEmpty()?"P2Verification":"P2RestartVerification";var flow=mc.createWorldOpenFlows();if(Files.exists(Path.of("saves/"+worldName+"/level.dat")))flow.loadLevel(mc.screen,worldName);else flow.createFreshLevel(worldName,new LevelSettings(worldName,GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(42,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());phase=1;ticks=0;return;
        }
        if(phase==1){if(mc.player==null||mc.getSingleplayerServer()==null||ticks<80)return;
            String mode=System.getProperty("aedialsworks.restartSmoke","");if(!mode.isEmpty()){server(p->P2RestartFixture.begin(p,mode));phase=8;ticks=0;return;}
            server(P2ClientSmoke::setup);phase=2;ticks=0;return;}
        if(phase==8){if(restartDone){com.mpp.aedialsworks.Aedialsworks.LOGGER.info("P2_RESTART_{}_PASS",System.getProperty("aedialsworks.restartSmoke"));mc.stop();phase=9;return;}server(p->restartDone=P2RestartFixture.step(p));return;}
        if(phase==2){if(!ready||ticks<100)return;server(p->{connect(p);open(p,0);});phase=3;ticks=0;return;}
        if(phase==3){
            if(!(mc.screen instanceof PowerToolsScreen)||!(mc.player.containerMenu instanceof PowerToolsMenu menu)||menu.data.isEmpty()||ticks<25)return;
            if(index==3)require(menu.data.getList("entries",Tag.TAG_COMPOUND).getCompound(1).getLong("threshold")==((1L<<40)+(nativeInputStage>=2?17:0)),"Long fluid threshold survives menu sync");
            require(mc.screen instanceof appeng.client.gui.AEBaseScreen<?>, "AE2 native screen: "+NAMES.get(index));
            require(menu.slots.size()==(index==1?71:index<9||index==12?60:36),"Slot topology: "+NAMES.get(index));
            if(index<9&&index!=1||index==12){
                var config=menu.getConfigurationSlots();
                require(config.size()==24&&config.stream().allMatch(slot->slot instanceof appeng.menu.slot.FakeSlot),"24 native AE2 configuration slots");
                for(int i=0;i<24;i++){
                    var entry=menu.data.getList("entries",Tag.TAG_COMPOUND).getCompound(i);
                    if(entry.contains("key"))require(!config.get(i).getItem().isEmpty(),"Native config icon "+i);
                }
            }
            if(index==3&&nativeInputStage<2){
                var screen=(PowerToolsScreen)mc.screen;
                var field=screen.children().stream().filter(c->c instanceof appeng.client.gui.widgets.ConfirmableTextField)
                    .map(c->(appeng.client.gui.widgets.ConfirmableTextField)c).min(java.util.Comparator.comparingInt(net.minecraft.client.gui.components.EditBox::getY)).orElseThrow();
                if(nativeInputStage==0){
                    screen.mouseClicked(screen.getGuiLeft()+94,screen.getGuiTop()+27,0);
                    nativeInputStage=1;return;
                }
                require(field.getValue().equals(Long.toString(1L<<40)),"Native field preserves long fluid threshold");
                screen.mouseClicked(field.getX()+2,field.getY()+2,0);
                screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_E,0,0);
                require(mc.screen==screen,"Native number field consumes inventory hotkey");
                field.setValue(Long.toString((1L<<40)+17));
                screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0);
                require(mc.screen==screen,"Native number field confirms without closing");
                nativeInputStage=2;ticks=0;return;
            }
            if(index==3&&nativeInputStage>=2&&nativeInputStage<6){
                if(nativeInputStage==2){server(p->{var logic=(MonitorLogic)((PowerBlockEntity)p.level().getBlockEntity(new BlockPos(3,64,0))).logic();require(logic.settings.entries[1].threshold==(1L<<40)+17,"Native Enter saves long quantity on server");nativeRoundTripChecked=true;});nativeInputStage=3;return;}
                if(!nativeRoundTripChecked)return;
                int inventorySlot=menu.getSlots(appeng.menu.SlotSemantics.PLAYER_HOTBAR).get(0).index;
                if(nativeInputStage==3){click((PowerToolsScreen)mc.screen,"inventoryButton");require(((PowerToolsScreen)mc.screen).currentView()==PowerToolsScreen.View.INVENTORY,"Native inventory child page");mc.gameMode.handleInventoryMouseClick(menu.containerId,inventorySlot,0,net.minecraft.world.inventory.ClickType.PICKUP,mc.player);nativeInputStage=4;ticks=0;return;}
                if(nativeInputStage==4){require(menu.getCarried().is(Items.IRON_INGOT)&&menu.getCarried().getCount()==64,"Real player inventory remains usable");mc.gameMode.handleInventoryMouseClick(menu.containerId,inventorySlot,0,net.minecraft.world.inventory.ClickType.PICKUP,mc.player);nativeInputStage=5;ticks=0;return;}
                require(menu.getCarried().isEmpty()&&menu.slots.get(inventorySlot).getItem().getCount()==64,"Inventory round trip conserves items and native config icons");nativeInputStage=6;mc.screen.onClose();ticks=0;return;
            }
            if(index<9||index==12)require(menu.data.getList("entries",Tag.TAG_COMPOUND).size()==(index==1?12:24),"Full state received: "+NAMES.get(index));
            if(index==9&&!interactionChecked){
                var box=(net.minecraft.client.gui.components.EditBox)mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox).findFirst().orElseThrow();
                mc.screen.mouseClicked(box.getX()+3,box.getY()+3,0);mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_E,0,0);mc.screen.charTyped('e',0);
                require(mc.screen instanceof PowerToolsScreen&&box.getValue().equals("e"),"Search handles inventory key without closing");box.setValue("");interactionChecked=true;
            }
            if(!originalLayout(mc,menu))return;
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),5,5);
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(REPORT.resolve(NAMES.get(index)+".png"));}
            com.mpp.aedialsworks.Aedialsworks.LOGGER.info("P2_SCREEN_PASS: {}",NAMES.get(index));
            layoutStage=0;if(++index<NAMES.size()){server(p->{p.closeContainer();open(p,index);});ticks=0;return;}
            server(p->{p.closeContainer();p.teleportTo(p.serverLevel(),2.5,65,-5.5,0,8);var alarm=(MonitorLogic)((PowerBlockEntity)p.level().getBlockEntity(new BlockPos(4,64,0))).logic();alarm.toggleBinding(p.getUUID());alarm.refresh();});phase=4;ticks=0;return;
        }
        if(phase==4){
            if(ticks<120)return;
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(REPORT.resolve("world.png"));}
            server(p->p.teleportTo(p.serverLevel(),9.5,65,-5.5,0,20));phase=5;ticks=0;return;
        }
        if(phase==5){
            if(ticks<35)return;
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(REPORT.resolve("parts-world.png"));}
            if(Boolean.getBoolean("aedialsworks.jeiSmoke")){if(jeiShow==null)return;jeiShow.run();phase=6;ticks=0;return;}
            finish(mc);return;
        }
        if(phase==6){if(ticks<25)return;require(mc.screen!=null&&mc.screen.getClass().getName().startsWith("mezz.jei"),"JEI recipe screen opened");try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(REPORT.resolve("jei-reusable.png"));}finish(mc);}

    }
    private static void click(PowerToolsScreen screen,String widget){var p=screen.getStyle().getWidget(widget);screen.mouseClicked(screen.getGuiLeft()+p.getLeft()+3,screen.getGuiTop()+p.getTop()+3,0);}
    private static void capture(Minecraft mc,String name)throws Exception{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(REPORT.resolve(name+".png"));}}
    private static boolean originalLayout(Minecraft mc,PowerToolsMenu menu)throws Exception{
        var screen=(PowerToolsScreen)mc.screen;var view=screen.currentView();
        if(index==0){
            if(layoutStage==0){require(screen.getStyle().getBackground().getSrcWidth()==238,"Original maintainer width");require(menu.getConfigurationSlots().stream().filter(slot->slot.x>=0).count()==18,"Original three by six viewport");screen.mouseScrolled(screen.getGuiLeft()+100,screen.getGuiTop()+80,-1);screen.mouseScrolled(screen.getGuiLeft()+100,screen.getGuiTop()+80,-1);layoutStage++;ticks=0;return false;}
            if(layoutStage==1){var slot=menu.getConfigurationSlots().get(23);require(slot.x==148&&slot.y==136,"Scroll reaches final maintainer entry");screen.mouseClicked(screen.getGuiLeft()+slot.x+3,screen.getGuiTop()+slot.y+3,0);layoutStage++;ticks=0;return false;}
            if(layoutStage==2){require(view==PowerToolsScreen.View.ENTRY,"Original maintainer editor");capture(mc,"maintainer-editor");click(screen,"choose");layoutStage++;ticks=0;return false;}
            if(layoutStage==3){require(view==PowerToolsScreen.View.SELECTOR,"Resource selector");capture(mc,"resource-selector");var slot=menu.getSlots(appeng.menu.SlotSemantics.PLAYER_HOTBAR).get(0);screen.mouseClicked(screen.getGuiLeft()+slot.x+3,screen.getGuiTop()+slot.y+3,0);layoutStage++;ticks=0;return false;}
            if(layoutStage==4){require(view==PowerToolsScreen.View.ENTRY&&menu.getCarried().isEmpty(),"Selecting inventory key preserves inventory");screen.onClose();layoutStage++;ticks=0;return false;}
            require(view==PowerToolsScreen.View.MAINTAINER,"Return preserves maintainer session");
        }
        if(index==1){
            if(layoutStage==0){require(screen.getStyle().getBackground().getSrcWidth()==211,"Original crafter width");require(menu.getSlots(appeng.menu.SlotSemantics.CRAFTING_RESULT).get(0).getItem().is(appeng.core.definitions.AEItems.WRAPPED_GENERIC_STACK.asItem()),"Native result preview");click(screen,"overview");layoutStage++;ticks=0;return false;}
            if(layoutStage==1){require(view==PowerToolsScreen.View.CRAFTER_OVERVIEW,"Twelve recipe overview");require(screen.getStyle().getBackground().getSrcHeight()==248,"Original overview height");require(menu.getSlots(com.mpp.aedialsworks.powertools.menu.PowerToolsSlotSemantics.PATTERN[11]).get(0).y==224,"Original final overview slot");capture(mc,"crafter-overview");screen.mouseClicked(screen.getGuiLeft()+50,screen.getGuiTop()+232,0);layoutStage++;ticks=0;return false;}
            if(layoutStage==2){require(view==PowerToolsScreen.View.CRAFTER,"Return to recipe detail");var slot=menu.getSlots(com.mpp.aedialsworks.powertools.menu.PowerToolsSlotSemantics.PATTERN[11]).get(0);require(slot.x==17&&slot.y==43,"Last recipe uses original pattern slot");click(screen,"machine");layoutStage++;ticks=0;return false;}
            if(layoutStage==3){require(view==PowerToolsScreen.View.SETTINGS,"Speed and batch editor");capture(mc,"crafter-settings");screen.onClose();layoutStage++;ticks=0;return false;}
        }
        if(index>=2&&index<=8){require(screen.getStyle().getBackground().getSrcWidth()==221,"Original monitor width");var last=menu.getConfigurationSlots().get(23);require(last.x==165&&last.y==137,"Original four by six monitor grid");}
        if(index==10){
            if(layoutStage==0){click(screen,"component0");layoutStage++;ticks=0;return false;}
            if(layoutStage==1){require(view==PowerToolsScreen.View.LOCATIONS,"Locator component detail");capture(mc,"locator-detail");screen.onClose();layoutStage++;ticks=0;return false;}
        }
        if(index==11){
            if(layoutStage==0){click(screen,"component0");layoutStage++;ticks=0;return false;}
            if(layoutStage==1){require(view==PowerToolsScreen.View.LOCATIONS,"Priority target details");click(screen,"result0");layoutStage++;ticks=0;return false;}
            if(layoutStage==2){require(view==PowerToolsScreen.View.PRIORITY,"Original priority editor");capture(mc,"priority-editor");screen.onClose();layoutStage++;ticks=0;return false;}
            if(layoutStage==3){screen.onClose();layoutStage++;ticks=0;return false;}
        }
        if(index==12)require(screen.getStyle().getBackground().getSrcWidth()==178,"Original remote monitor width");
        return true;
    }
    private static void finish(Minecraft mc) throws Exception {
        Files.writeString(REPORT.resolve("client.txt"),"PASS: all 21 item models and legacy atlas textures, four part models, five block menus, four part menus, scanner/locator/tuner and native search input, remote menu, AE2 native screen/widgets and 24 fake slots, long fluid field selection and Enter confirmation, long item/fluid snapshots and world display/HUD rendering. JEI: "+Boolean.getBoolean("aedialsworks.jeiSmoke")+"\n",StandardCharsets.UTF_8);
        com.mpp.aedialsworks.Aedialsworks.LOGGER.info("P2_CLIENT_SMOKE_PASS");mc.stop();phase=7;
    }
    private static void setup(ServerPlayer p){
        var level=p.serverLevel();for(int x=-3;x<=13;x++)for(int z=-8;z<=5;z++)for(int y=63;y<=67;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),y==63?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(level,2.5,65,-1.5,0,15);p.getInventory().clearContent();
        var blocks=List.of(AWBlocks.BETTER_LEVEL_MAINTAINER.get(),AWBlocks.AUTO_CRAFTER.get(),AWBlocks.STORAGE_LEVEL_EMITTER.get(),AWBlocks.STORAGE_DISPLAY.get(),AWBlocks.STORAGE_LEVEL_ALARM.get());
        for(int i=0;i<blocks.size();i++){var pos=new BlockPos(i,64,0);level.setBlockAndUpdate(pos,blocks.get(i).defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.NORTH));var be=(PowerBlockEntity)level.getBlockEntity(pos);if(be.logic() instanceof MonitorLogic m){m.settings.entries[0].key=AEItemKey.of(Items.IRON_INGOT);m.settings.entries[0].threshold=100;m.settings.entries[1].key=AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER);m.settings.entries[1].threshold=1L<<40;if(i==3)m.settings.entries[1].comparison=Comparison.GREATER;be.powerChanged();}}
        var parts=List.of(AWItems.STORAGE_LEVEL_EMITTER_PART.get(),AWItems.STORAGE_DISPLAY_PART.get(),AWItems.STORAGE_DISPLAY_PART_SMALLER.get(),AWItems.STORAGE_DISPLAY_PART_SMALLERER.get());
        for(int i=0;i<parts.size();i++){var part=PartHelper.setPart(level,new BlockPos(8+i,64,0),Direction.NORTH,p,parts.get(i));part.logic.settings.entries[0].key=AEItemKey.of(Items.DIAMOND);part.logic.settings.entries[0].threshold=64;part.powerChanged();}
        var maintainer=(com.mpp.aedialsworks.powertools.maintainer.MaintainerLogic)((PowerBlockEntity)level.getBlockEntity(new BlockPos(0,64,0))).logic();
        for(int i=0;i<24;i++){maintainer.entries[i].key=AEItemKey.of(i%2==0?Items.IRON_INGOT:Items.DIAMOND);maintainer.entries[i].threshold=64L*(i+1);maintainer.entries[i].enabled=false;}
        var crafter=(com.mpp.aedialsworks.powertools.crafter.CrafterLogic)((PowerBlockEntity)level.getBlockEntity(new BlockPos(1,64,0))).logic();
        var recipe=level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("minecraft:oak_planks")).orElseThrow();
        ItemStack[] inputs=new ItemStack[9];Arrays.fill(inputs,ItemStack.EMPTY);inputs[0]=new ItemStack(Items.OAK_LOG);
        var pattern=appeng.api.crafting.PatternDetailsHelper.encodeCraftingPattern((net.minecraft.world.item.crafting.CraftingRecipe)recipe,inputs,new ItemStack(Items.OAK_PLANKS,4),false,false);
        crafter.patterns.setStackInSlot(0,pattern);crafter.patterns.setStackInSlot(11,pattern.copy());crafter.upgrades.setStackInSlot(0,new ItemStack(AWItems.CRAFTER_SPEED_UPGRADE_IV.get()));
        level.setBlockAndUpdate(new BlockPos(7,64,0),AEBlocks.DRIVE.block().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(5,65,0),AEBlocks.CONTROLLER.block().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(5,64,0),AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());level.setBlockAndUpdate(new BlockPos(6,64,0),AEBlocks.WIRELESS_ACCESS_POINT.block().defaultBlockState());
        p.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,64));p.getInventory().setItem(1,new ItemStack(AWItems.CRAFTER_SPEED_UPGRADE_IV.get()));ready=true;
    }
    private static void connect(ServerPlayer p){var level=p.serverLevel();var controller=(appeng.blockentity.networking.ControllerBlockEntity)level.getBlockEntity(new BlockPos(5,65,0));var root=controller.getMainNode().getNode();
        for(int i=0;i<8;i++){var be=level.getBlockEntity(new BlockPos(i,64,0));if(be instanceof appeng.me.helpers.IGridConnectedBlockEntity host&&host.getMainNode().getNode()!=root&&host.getMainNode().getGrid()!=root.getGrid())GridHelper.createConnection(root,host.getMainNode().getNode());}
        for(int i=0;i<4;i++){var part=(MonitorPart)PartHelper.getPart(level,new BlockPos(8+i,64,0),Direction.NORTH);if(part.getGridNode().getGrid()!=root.getGrid())GridHelper.createConnection(root,part.getGridNode());}
    }
    private static void open(ServerPlayer p,int i){
        p.teleportTo(p.serverLevel(),2.5,65,0.5,0,15);
        if(i<5){require(MenuOpener.open(AWMenus.POWER_TOOLS.get(),p,MenuLocators.forBlockEntity(p.level().getBlockEntity(new BlockPos(i,64,0)))),"Block menu opener");return;}
        if(i<9){p.teleportTo(p.serverLevel(),9.5,65,-.5,0,20);var part=(MonitorPart)PartHelper.getPart(p.level(),new BlockPos(i+3,64,0),Direction.NORTH);require(MenuOpener.open(AWMenus.POWER_TOOLS.get(),p,MenuLocators.forPart(part)),"Part menu opener");return;}
        var item=i==9?AWItems.NETWORK_HEALTH_SCANNER.get():i==10?AWItems.NETWORK_COMPONENT_LOCATOR.get():i==11?AWItems.PRIORITY_TUNER.get():AWItems.REMOTE_STORAGE_MONITOR.get();var stack=new ItemStack(item);
        if(i==12){AWItems.REMOTE_STORAGE_MONITOR.get().injectAEPower(stack,10000,Actionable.MODULATE);GridLinkables.get(item).link(stack,GlobalPos.of(p.level().dimension(),new BlockPos(6,64,0)));}
        else {stack.getOrCreateTag().putLong("target",new BlockPos(0,64,0).asLong());stack.getOrCreateTag().putInt("face",-1);stack.getOrCreateTag().putString("dimension",p.level().dimension().location().toString());}
        p.getInventory().setItem(8,stack);p.inventoryMenu.broadcastChanges();require(MenuOpener.open(AWMenus.POWER_TOOLS.get(),p,MenuLocators.forInventorySlot(8)),"Tool menu opener "+i);
    }
}
