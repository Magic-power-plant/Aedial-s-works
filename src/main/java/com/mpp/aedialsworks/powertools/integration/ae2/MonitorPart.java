package com.mpp.aedialsworks.powertools.integration.ae2;
import appeng.api.parts.*;
import appeng.api.networking.*;
import appeng.parts.PartModel;
import appeng.parts.AEBasePart;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.mpp.aedialsworks.common.registry.AWMenus;
import com.mpp.aedialsworks.common.util.AWIds;
import com.mpp.aedialsworks.powertools.*;
import com.mpp.aedialsworks.powertools.monitor.MonitorLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
public final class MonitorPart extends AEBasePart implements PowerHost,PowerMenuHost {
    public final MonitorLogic logic;
    public final int size;
    public final boolean emitter;
    public MonitorPart(IPartItem<?> item){super(item);getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);var spec=(PowerPartItem)item;size=spec.size;emitter=spec.emitter;logic=new MonitorLogic(this,false);getMainNode().setIdlePowerUsage(1);}
    public static String model(boolean emitter,int size){return "part/powertools/"+(emitter?"emitter":"display_"+size);}
    @Override public IPartModel getStaticModels(){return new PartModel(AWIds.id(model(emitter,size)));}
    @Override public void getBoxes(IPartCollisionHelper boxes){int inset=emitter?5:2+size;boxes.addBox(inset,inset,14,16-inset,16-inset,16);boxes.addBox(6,6,11,10,10,14);}
    @Override public boolean onPartActivate(Player player,InteractionHand hand,Vec3 pos){if(super.onPartActivate(player,hand,pos))return true;if(!isClientSide())MenuOpener.open(AWMenus.POWER_TOOLS.get(),player,MenuLocators.forPart(this));return true;}
    @Override public PowerConfigurable configuration(){return logic;}
    @Override public String powerKind(){return emitter?"storage_level_emitter":"storage_display";}
    @Override public IManagedGridNode powerNode(){return getMainNode();}
    @Override public IGridNode getActionableNode(){return getGridNode();}
    @Override public Level powerLevel(){return getLevel();}
    @Override public BlockPos powerPos(){return getBlockEntity().getBlockPos();}
    @Override public boolean canUsePower(Player p){return getHost()!=null&&getHost().isInWorld()&&getHost().getPart(getSide())==this&&p.level()==getLevel()&&p.distanceToSqr(Vec3.atCenterOf(powerPos()))<=64;}
    @Override public void powerChanged(){if(getHost()!=null){getHost().markForSave();getHost().markForUpdate();}}
    @Override public void conditionChanged(boolean active){powerChanged();if(getHost()!=null && getLevel()!=null){var block=getBlockEntity().getBlockState().getBlock();getLevel().updateNeighborsAt(powerPos(),block);getLevel().updateNeighborsAt(powerPos().relative(getSide()),block);}}
    @Override protected void onMainNodeStateChanged(IGridNodeListener.State reason){super.onMainNodeStateChanged(reason);if(logic!=null)logic.refresh();}
    @Override public void addToWorld(){logic.resume();super.addToWorld();}
    @Override public void removeFromWorld(){logic.unload();super.removeFromWorld();}
    @Override public int isProvidingStrongPower(){return isProvidingWeakPower();}
    @Override public int isProvidingWeakPower(){return emitter&&isActive()&&logic.settings.condition?logic.settings.strength:0;}
    @Override public void readFromNBT(CompoundTag n){super.readFromNBT(n);logic.load(n.getCompound("power"));}
    @Override public void writeToNBT(CompoundTag n){super.writeToNBT(n);var p=new CompoundTag();logic.save(p);n.put("power",p);}
    @Override public void writeToStream(FriendlyByteBuf buf){super.writeToStream(buf);buf.writeNbt(logic.snapshot());}
    @Override public boolean readFromStream(FriendlyByteBuf buf){super.readFromStream(buf);var n=buf.readNbt();if(n!=null)logic.load(n);return true;}
    @Override public boolean requireDynamicRender(){return !emitter;}
    @Override public void renderDynamic(float partial,com.mojang.blaze3d.vertex.PoseStack poses,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){
        com.mpp.aedialsworks.powertools.client.PowerDisplayRenderer.renderPart(this,poses,buffers,light,overlay);
    }
}
