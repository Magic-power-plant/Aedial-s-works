package com.mpp.aedialsworks.cells.integration.ae2;

import java.util.*;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.api.networking.*;
import appeng.helpers.IPriorityHost;
import appeng.util.SettingsFrom;
import com.mpp.aedialsworks.cells.*;
import com.mpp.aedialsworks.cells.api.CellsHost;
import com.mpp.aedialsworks.cells.interfaceblock.InterfaceLogic;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public abstract class AbstractCellsBlockEntity extends AENetworkBlockEntity implements CellsHost,IPriorityHost {
    protected AbstractCellsLogic logic;
    protected AbstractCellsBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(2);}
    @Override public IManagedGridNode cellsNode(){return getMainNode();}
    @Override public Level cellsLevel(){return level;}
    @Override public BlockPos cellsPos(){return worldPosition;}
    @Override public AbstractCellsLogic cellsLogic(){return logic;}
    @Override public Set<Direction> targetSides(){return EnumSet.allOf(Direction.class);}
    @Override public boolean canUseCells(Player p){return !isRemoved()&&level!=null&&p.level()==level&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64;}
    @Override public void cellsChanged(){saveChanges();}
    @Override public void returnToMainMenu(Player player, appeng.menu.ISubMenu menu){appeng.menu.MenuOpener.open(com.mpp.aedialsworks.common.registry.AWMenus.CELLS.get(),player,appeng.menu.locator.MenuLocators.forBlockEntity(this));}
    @Override public net.minecraft.world.item.ItemStack getMainMenuIcon(){return new net.minecraft.world.item.ItemStack(cellsItem());}
    @Override public int getPriority(){return logic.priority;}
    @Override public void setPriority(int priority){logic.priority=priority;logic.settingsChanged();}
    @Override public void onReady(){logic.resume();super.onReady();}
    @Override public void onMainNodeStateChanged(IGridNodeListener.State reason){if(logic!=null)logic.settingsChanged();}
    @Override public void onChunkUnloaded(){logic.unload();super.onChunkUnloaded();}
    @Override public void setRemoved(){if(logic!=null)logic.unload();super.setRemoved();}
    @Override public void loadTag(CompoundTag n){super.loadTag(n);if(logic!=null)logic.load(n.getCompound("cells"));}
    @Override public void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("cells",logic.save());}
    @Override public void exportSettings(SettingsFrom from,CompoundTag n,Player p){super.exportSettings(from,n,p);n.put("cells",logic.exportData(from));}
    @Override public void importSettings(SettingsFrom from,CompoundTag n,Player p){if(p!=null&&!p.mayBuild())return;super.importSettings(from,n,p);if(n.contains("cells"))logic.importData(from,n.getCompound("cells"));}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){if(!isRemoved()&&logic instanceof InterfaceLogic i){var value=i.capability(cap);if(value.isPresent())return value;}return super.getCapability(cap,side);}
    @Override public void clearContent(){if(logic!=null)logic.load(new CompoundTag());}
}
