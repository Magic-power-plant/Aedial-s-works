package com.mpp.aedialsworks.powertools.integration.ae2;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.api.networking.*;
import com.mpp.aedialsworks.powertools.*;
import com.mpp.aedialsworks.powertools.monitor.MonitorLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
/** AE2 owns node create/destroy/NBT; this adapter owns the domain lifecycle. */
public abstract class AbstractPowerBlockEntity extends AENetworkBlockEntity implements PowerHost,PowerMenuHost {
    protected AbstractPowerLogic logic;
    protected AbstractPowerBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(2);}
    @Override public IManagedGridNode powerNode(){return getMainNode();}
    @Override public Level powerLevel(){return level;}
    @Override public BlockPos powerPos(){return worldPosition;}
    @Override public PowerConfigurable configuration(){return logic;}
    public AbstractPowerLogic logic(){return logic;}
    @Override public boolean canUsePower(Player player){return level!=null && player.level()==level && !isRemoved() && level.getBlockEntity(worldPosition)==this && player.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64;}
    @Override public void powerChanged(){saveChanges();if(level!=null && !level.isClientSide())markForUpdate();}
    @Override public void conditionChanged(boolean active){if(level!=null){level.updateNeighborsAt(worldPosition,getBlockState().getBlock());powerChanged();}}
    @Override public void onReady(){logic.resume();super.onReady();}
    @Override public void onChunkUnloaded(){logic.unload();super.onChunkUnloaded();}
    @Override public void setRemoved(){if(logic!=null)logic.unload();super.setRemoved();}
    @Override public void onMainNodeStateChanged(IGridNodeListener.State reason){if(logic instanceof MonitorLogic monitor)monitor.refresh();markForUpdate();}
    @Override public void loadTag(CompoundTag tag){super.loadTag(tag);if(logic!=null)logic.load(tag.getCompound("power"));}
    @Override public void saveAdditional(CompoundTag tag){super.saveAdditional(tag);var n=new CompoundTag();if(logic!=null)logic.save(n);tag.put("power",n);}
    @Override protected void writeToStream(FriendlyByteBuf buf){super.writeToStream(buf);buf.writeNbt(logic instanceof MonitorLogic?logic.snapshot():new CompoundTag());}
    @Override protected boolean readFromStream(FriendlyByteBuf buf){super.readFromStream(buf);var n=buf.readNbt();if(logic instanceof MonitorLogic && n!=null)logic.load(n);return true;}
}
