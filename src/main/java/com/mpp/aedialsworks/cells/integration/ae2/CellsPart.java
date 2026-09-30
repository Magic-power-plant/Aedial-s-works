package com.mpp.aedialsworks.cells.integration.ae2;

import java.util.*;
import appeng.api.parts.*;
import appeng.api.networking.*;
import appeng.parts.*;
import appeng.helpers.IPriorityHost;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.util.SettingsFrom;
import com.mpp.aedialsworks.cells.*;
import com.mpp.aedialsworks.cells.api.CellsHost;
import com.mpp.aedialsworks.cells.interfaceblock.InterfaceLogic;
import com.mpp.aedialsworks.cells.subnetproxy.ProxyLogic;
import com.mpp.aedialsworks.common.registry.AWMenus;
import com.mpp.aedialsworks.common.util.AWIds;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public final class CellsPart extends AEBasePart implements CellsHost,IPriorityHost {
    public final MachineKind kind;
    public final AbstractCellsLogic logic;
    public CellsPart(IPartItem<?> item){super(item);kind=((CellsPartItem)item).kind;getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(1);logic=kind.resourceInterface()?new InterfaceLogic(this):new ProxyLogic(this);}
    @Override public IManagedGridNode cellsNode(){return getMainNode();}
    @Override public IGridNode getActionableNode(){return getGridNode();}
    @Override public Level cellsLevel(){return getLevel();}
    @Override public BlockPos cellsPos(){return getBlockEntity().getBlockPos();}
    @Override public MachineKind cellsKind(){return kind;}
    @Override public ItemLike cellsItem(){return getPartItem();}
    @Override public AbstractCellsLogic cellsLogic(){return logic;}
    @Override public Set<Direction> targetSides(){return Set.of(getSide());}
    @Override public void cellsChanged(){if(getHost()!=null)getHost().markForSave();}
    @Override public boolean canUseCells(Player p){return getHost()!=null&&getHost().isInWorld()&&getHost().getPart(getSide())==this&&p.level()==getLevel()&&p.distanceToSqr(Vec3.atCenterOf(cellsPos()))<=64;}
    @Override public void returnToMainMenu(Player player, appeng.menu.ISubMenu menu){MenuOpener.open(AWMenus.CELLS.get(),player,MenuLocators.forPart(this));}
    @Override public net.minecraft.world.item.ItemStack getMainMenuIcon(){return new net.minecraft.world.item.ItemStack(cellsItem());}
    @Override public int getPriority(){return logic.priority;}
    @Override public void setPriority(int value){logic.priority=value;logic.settingsChanged();}
    @Override public void addToWorld(){super.addToWorld();logic.resume();}
    @Override public void removeFromWorld(){logic.unload();super.removeFromWorld();}
    @Override protected void onMainNodeStateChanged(IGridNodeListener.State reason){super.onMainNodeStateChanged(reason);if(logic!=null)logic.settingsChanged();}
    @Override public void readFromNBT(CompoundTag n){super.readFromNBT(n);logic.load(n.getCompound("cells"));}
    @Override public void writeToNBT(CompoundTag n){super.writeToNBT(n);n.put("cells",logic.save());}
    @Override public void exportSettings(SettingsFrom from,CompoundTag n){super.exportSettings(from,n);n.put("cells",logic.exportData(from));}
    @Override public void importSettings(SettingsFrom from,CompoundTag n,Player p){if(p!=null&&!p.mayBuild())return;super.importSettings(from,n,p);if(n.contains("cells"))logic.importData(from,n.getCompound("cells"));}
    @Override public void clearContent(){logic.load(new CompoundTag());}
    @Override public boolean onPartActivate(Player p,InteractionHand hand,Vec3 hit){if(super.onPartActivate(p,hand,hit))return true;if(!isClientSide())MenuOpener.open(AWMenus.CELLS.get(),p,MenuLocators.forPart(this));return true;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap){return logic instanceof InterfaceLogic i?i.capability(cap):LazyOptional.empty();}
    public CellsPart counterpart(){
        if(!kind.proxy()||getHost()==null||getLevel()==null)return null;var pos=cellsPos().relative(getSide());if(!getLevel().hasChunkAt(pos))return null;
        var part=PartHelper.getPart(getLevel(),pos,getSide().getOpposite());return part instanceof CellsPart other&&other.kind.proxy()&&other.kind!=kind?other:null;
    }
    public static String modelPath(MachineKind kind){if(kind.proxy())return "part/cells/"+kind.id;String operation=kind.input&&kind.output?"io_interface":kind.input?"import_interface":"export_interface";String channel=kind.items&&kind.fluids?"combined":kind.fluids?"fluid":"item";return "part/cells/"+operation+"/"+channel;}
    @Override public IPartModel getStaticModels(){
        String base=modelPath(kind);if(kind.proxy())return new PartModel(AWIds.id(base+"/base"),AWIds.id(base+"/status_"+(isActive()?"active":isPowered()?"on":"off")));
        boolean fixed=com.mpp.aedialsworks.common.config.AWConfigs.CLIENT.cells.interfaces.useFixedInterfaceTextures.get();String suffix=fixed?"_fixed":"";
        return new PartModel(AWIds.id(base+"/base"+suffix),AWIds.id(base+"/"+(isActive()?"has_channel"+suffix:isPowered()?"on":"off")));
    }
    @Override public void getBoxes(IPartCollisionHelper b){b.addBox(2,2,14,14,14,16);b.addBox(5,5,11,11,11,14);}
}
