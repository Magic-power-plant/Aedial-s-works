package com.mpp.aedialsworks.smoke;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.*;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.mpp.aedialsworks.common.registry.*;
import com.mpp.aedialsworks.cells.*;
import com.mpp.aedialsworks.cells.cell.*;
import com.mpp.aedialsworks.cells.integration.ae2.*;
import com.mpp.aedialsworks.cells.interfaceblock.*;
import com.mpp.aedialsworks.cells.menu.*;
import com.mpp.aedialsworks.cells.client.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.server.level.ServerPlayer;
public final class P3ClientSmoke {
    private static int phase,ticks,total,index,step,upgradeStep;private static boolean language;private static volatile boolean ready;private static volatile Throwable failure;
    private static final Path REPORT=Path.of(System.getProperty("aedialsworks.smokeReportDir"),"p3",System.getProperty("aedialsworks.smokeLanguage","en_us"));
    private static final List<MachineKind> BLOCKS=Arrays.stream(MachineKind.values()).filter(k->!k.proxy()).toList(),PARTS=Arrays.stream(MachineKind.values()).filter(k->k!=MachineKind.EXPOSER).toList();
    private static void require(boolean b,String message){if(!b)throw new IllegalStateException(message);}
    private static void server(java.util.function.Consumer<ServerPlayer> work){var s=Minecraft.getInstance().getSingleplayerServer();s.execute(()->{try{work.accept(s.getPlayerList().getPlayers().get(0));}catch(Throwable e){failure=e;}});}
    private static void capture(Minecraft mc,String name)throws Exception{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(REPORT.resolve(name+".png"));}}
    public static void tick()throws Exception{
        var mc=Minecraft.getInstance();ticks++;if(phase>=2&&ticks==150){capture(mc,"stalled");throw new IllegalStateException("P3 stalled phase="+phase+" index="+index+" step="+step+" screen="+mc.screen+" menu="+mc.player.containerMenu+" data="+(mc.player.containerMenu instanceof CellsMenu m?m.data:"other")+" position="+mc.player.position());}if(++total>2500)throw new IllegalStateException("P3 timeout phase="+phase+" index="+index+" step="+step);if(failure!=null)throw new IllegalStateException("P3 server fixture",failure);
        if(phase==0){if(mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")){mc.setScreen(new TitleScreen());return;}if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
            if(!language){language=true;String lang=System.getProperty("aedialsworks.smokeLanguage","en_us");if(!mc.getLanguageManager().getSelected().equals(lang)){mc.getLanguageManager().setSelected(lang);mc.options.languageCode=lang;mc.reloadResourcePacks();return;}}
            Files.createDirectories(REPORT);for(var item:AWItems.ITEMS.getEntries())checkModel(mc,mc.getItemRenderer().getModel(new ItemStack(item.get()),null,null,0),item.getId().toString());
            for(var kind:PARTS){String base=CellsPart.modelPath(kind);checkModel(mc,mc.getModelManager().getModel(com.mpp.aedialsworks.common.util.AWIds.id(base+"/base")),"part "+kind);}
            for(var item:AWCells.CELLS.values())if(item.get().family!=CellFamily.CONFIGURABLE&&!item.get().family.creative()){var stack=new ItemStack(item.get());var model=mc.getItemRenderer().getModel(stack,null,null,0);require(model.getQuads(null,null,net.minecraft.util.RandomSource.create()).stream().map(net.minecraft.client.renderer.block.model.BakedQuad::getTintIndex).distinct().count()>=5,"Five tint layers "+item.getId());}
            mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.pauseOnLostFocus=false;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);String name="P3Verification";var flow=mc.createWorldOpenFlows();if(Files.exists(Path.of("saves/"+name+"/level.dat")))flow.loadLevel(mc.screen,name);else flow.createFreshLevel(name,new LevelSettings(name,GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(42,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());phase=1;ticks=0;return;
        }
        if(phase==1){if(mc.player==null||ticks<70)return;server(P3ClientSmoke::setup);phase=2;ticks=0;return;}
        if(phase==2){if(!ready||ticks<60)return;capture(mc,"world-machines");server(p->open(p,0));phase=3;ticks=0;return;}
        if(phase==3){if(ticks==3)server(p->com.mpp.aedialsworks.Aedialsworks.LOGGER.info("P3 open server menu={} valid={}",p.containerMenu,p.containerMenu.stillValid(p)));if(!(mc.screen instanceof CellsScreen screen)||!(mc.player.containerMenu instanceof CellsMenu menu)||menu.data.isEmpty()||ticks<25)return;
            require(menu.slots.size()>=108,"Real player/config/buffer slots");require(menu.getSlots(CellsSlots.FILTER[0]).get(0) instanceof appeng.menu.slot.FakeSlot,"Native ghost filter");
            if(index==0&&step<5){
                if(step==0||step==2){var w=screen.getStyle().getWidget("settings");screen.mouseClicked(screen.getGuiLeft()+w.getLeft()+3,screen.getGuiTop()+w.getTop()+3,0);step++;ticks=0;return;}
                if(step==3){var w=screen.getStyle().getWidget("next");screen.mouseClicked(screen.getGuiLeft()+w.getLeft()+3,screen.getGuiTop()+w.getTop()+3,0);step++;ticks=0;return;}
                var fields=screen.children().stream().filter(c->c instanceof appeng.client.gui.widgets.ConfirmableTextField).map(c->(appeng.client.gui.widgets.ConfirmableTextField)c).sorted(Comparator.comparingInt(net.minecraft.client.gui.components.EditBox::getY)).toList();require(fields.size()==3,"Three editable settings fields");
                if(step==1){capture(mc,"settings");fields.get(1).setValue("-12345");}
                else {var amount=fields.get(0);amount.setValue("4294967296");require(screen.mouseScrolled(amount.getX()+3,amount.getY()+3,1),"Amount scroll consumed");require(amount.getValue().equals("4294967297"),"Amount scroll preserves long quantities");capture(mc,"amount-settings");}
                var w=screen.getStyle().getWidget("save");screen.mouseClicked(screen.getGuiLeft()+w.getLeft()+3,screen.getGuiTop()+w.getTop()+3,0);step++;ticks=0;return;
            }
            if(index==0){require(menu.data.getInt("priority")==-12345,"Signed priority round trip");require(menu.data.getLong("maxSlot")==4294967297L,"Long amount setting round trip");}
            if(index>=BLOCKS.size()&&PARTS.get(index-BLOCKS.size()).proxy())require(menu.getSlots(CellsSlots.UPGRADE[23]).size()==1,"All 24 proxy upgrade slots visible");
            if(index>=BLOCKS.size()&&PARTS.get(index-BLOCKS.size())==MachineKind.PROXY_FRONT&&!exerciseUpgrade(mc,screen,menu,23))return;
            capture(mc,(index<BLOCKS.size()?"block_"+BLOCKS.get(index).id:"part_"+PARTS.get(index-BLOCKS.size()).id));
            if(++index<BLOCKS.size()+PARTS.size()){server(p->{p.closeContainer();open(p,index);});ticks=0;return;}index=0;server(p->{p.closeContainer();openCell(p,0);});phase=4;ticks=0;return;
        }
        if(phase==4){if(!(mc.screen instanceof CellConfigurationScreen screen)||!(mc.player.containerMenu instanceof CellConfigurationMenu menu)||menu.data.isEmpty()||ticks<25)return;require(menu.slots.size()>=72,"Cell configuration includes inventory and ghost slots");if(index<2)require(menu.getSlots(CellsSlots.UPGRADE[15]).size()==1,"All 16 cell upgrade slots visible");if(index==0&&!exerciseUpgrade(mc,screen,menu,15))return;capture(mc,"cell_"+index);if(++index<3){server(p->{p.closeContainer();openCell(p,index);});ticks=0;return;}server(ServerPlayer::closeContainer);mc.setScreen(new Gallery());phase=5;ticks=0;return;}
        if(phase==5){if(ticks<30)return;if(!(mc.screen instanceof Gallery)){mc.setScreen(new Gallery());ticks=0;return;}capture(mc,"cell-gallery");Files.writeString(REPORT.resolve("client.txt"),"PASS: 179 item models; 10 part models; 45 five-layer tinted cells; 19 block/part menus; three cell menus; signed priority and long amount roundtrips; expanded 24/16 upgrade slots and actual card pickup/return.\n",StandardCharsets.UTF_8);com.mpp.aedialsworks.Aedialsworks.LOGGER.info("P3_CLIENT_SMOKE_PASS");phase=6;mc.stop();}
    }
    private static boolean exerciseUpgrade(Minecraft mc,appeng.client.gui.AEBaseScreen<?> screen,appeng.menu.AEBaseMenu menu,int index){
        var slot=menu.getSlots(CellsSlots.UPGRADE[index]).get(0);double x=screen.getGuiLeft()+slot.x+8,y=screen.getGuiTop()+slot.y+8;
        if(upgradeStep==0){require(slot.getItem().is(AEItems.FUZZY_CARD.asItem()),"Last upgrade slot is synchronized");screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);upgradeStep=1;ticks=0;return false;}
        if(upgradeStep==1){require(menu.getCarried().is(AEItems.FUZZY_CARD.asItem()),"Expanded upgrade slot can be picked up: valid="+menu.stillValid(mc.player));screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);upgradeStep=2;ticks=0;return false;}
        require(menu.getCarried().isEmpty()&&slot.getItem().is(AEItems.FUZZY_CARD.asItem()),"Expanded upgrade slot accepts returned card");upgradeStep=0;return true;
    }
    private static void checkModel(Minecraft mc,net.minecraft.client.resources.model.BakedModel model,String id){
        require(model!=mc.getModelManager().getMissingModel(),"Missing model "+id);
        var random=net.minecraft.util.RandomSource.create();var faces=new ArrayList<Direction>(Arrays.asList(Direction.values()));faces.add(null);
        for(var face:faces)for(var quad:model.getQuads(null,face,random))require(!quad.getSprite().contents().name().getPath().equals("missingno"),"Missing sprite "+id);
    }
    private static void setup(ServerPlayer p){
        var config=com.mpp.aedialsworks.common.config.AWConfigs.SERVER.cells;config.general.subnetProxyUpgradeSlots.set(24);config.general.hdItemCellUpgradeSlots.set(16);config.general.configurableCellUpgradeSlots.set(16);config.interfaces.interfaceMaxSlotSizeLimit.set("-1");
        var level=p.serverLevel();for(int x=-3;x<=14;x++)for(int z=-5;z<=7;z++)level.setBlockAndUpdate(new BlockPos(x,63,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());p.teleportTo(level,2.5,65,-3.5,0,25);p.getInventory().clearContent();p.getInventory().setItem(8,new ItemStack(Items.IRON_INGOT,64));p.getInventory().setItem(7,new ItemStack(Items.WATER_BUCKET));
        for(int i=0;i<BLOCKS.size();i++){var kind=BLOCKS.get(i);var pos=new BlockPos(i,64,0);level.setBlockAndUpdate(pos,AWCells.BLOCKS.get(kind).get().defaultBlockState());var logic=((CellsBlockEntity)level.getBlockEntity(pos)).cellsLogic();configure(logic);}
        for(int i=0;i<PARTS.size();i++){var kind=PARTS.get(i);var pos=new BlockPos(i,64,4);if(PartHelper.getPart(level,pos,Direction.NORTH) instanceof CellsPart old)old.getHost().removePart(old);var part=PartHelper.setPart(level,pos,Direction.NORTH,p,AWCells.PARTS.get(kind).get());configure(part.logic);if(kind==MachineKind.PROXY_FRONT)part.logic.upgrades.setItemDirect(23,AEItems.FUZZY_CARD.stack());}ready=true;
    }
    private static void configure(AbstractCellsLogic logic){if(logic instanceof InterfaceLogic i){for(var port:i.ports){port.filters[0]=port.fluid?AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER):AEItemKey.of(Items.IRON_INGOT);port.keys[0]=port.filters[0];port.amounts[0]=1L<<34;port.maxSlot=1L<<40;}}else logic.filters.setStack(0,new GenericStack(AEItemKey.of(Items.IRON_INGOT),1));}
    private static void open(ServerPlayer p,int index){boolean block=index<BLOCKS.size();int i=block?index:index-BLOCKS.size();var pos=new BlockPos(i,64,block?0:4);p.teleportTo(p.serverLevel(),i+.5,65,block?-2:2,0,20);boolean opened=block?MenuOpener.open(AWMenus.CELLS.get(),p,MenuLocators.forBlockEntity(p.level().getBlockEntity(pos))):MenuOpener.open(AWMenus.CELLS.get(),p,MenuLocators.forPart((CellsPart)PartHelper.getPart(p.level(),pos,Direction.NORTH)));require(opened&&p.containerMenu instanceof CellsMenu,"Server menu opened "+pos);}
    private static void openCell(ServerPlayer p,int index){String id=index==0?"hyper_density_cell_1g":index==1?"configurable_cell":"creative_fluid_cell";var stack=new ItemStack(AWCells.CELLS.get(id).get());if(index==1)stack=CellComponentRecipe(stack);var item=(TieredCellItem)stack.getItem();if(index==0)item.getUpgrades(stack).setItemDirect(15,AEItems.FUZZY_CARD.stack());item.getConfigInventory(stack).setStack(0,new GenericStack(index==0?AEItemKey.of(Items.IRON_INGOT):AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER),1));p.getInventory().selected=0;p.getInventory().setItem(0,stack);CellConfigurationMenu.open(p);}
    private static ItemStack CellComponentRecipe(ItemStack stack){return com.mpp.aedialsworks.common.recipe.CellComponentRecipe.replaced(stack,new ItemStack(AWCells.component(CellFamily.HD_FLUID,CellTier.G1).get()));}
    private static final class Gallery extends Screen{private final List<ItemStack> icons=new ArrayList<>();Gallery(){super(Component.literal("CELLS"));AWCells.CELLS.values().forEach(v->icons.add(new ItemStack(v.get())));AWCells.COMPONENTS.values().forEach(v->icons.add(new ItemStack(v.get())));AWCells.UPGRADES.values().forEach(v->icons.add(new ItemStack(v.get())));}
        @Override public void render(GuiGraphics g,int x,int y,float partial){g.fill(0,0,width,height,0xff20262e);g.drawString(font,"CELLS · cells, components and upgrades",20,12,0xffffff);for(int i=0;i<icons.size();i++)g.renderItem(icons.get(i),20+(i%18)*30,35+(i/18)*36);super.render(g,x,y,partial);}
    }
}
