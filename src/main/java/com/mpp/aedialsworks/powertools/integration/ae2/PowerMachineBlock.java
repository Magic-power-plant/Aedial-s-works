package com.mpp.aedialsworks.powertools.integration.ae2;
import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.mpp.aedialsworks.powertools.MachineKind;
import com.mpp.aedialsworks.common.registry.AWMenus;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class PowerMachineBlock extends AEBaseEntityBlock<PowerBlockEntity> {
    public final MachineKind kind;
    public PowerMachineBlock(MachineKind kind){super(Properties.of().strength(3).requiresCorrectToolForDrops());this.kind=kind;}
    @Override public appeng.api.orientation.IOrientationStrategy getOrientationStrategy(){return appeng.api.orientation.OrientationStrategies.facing();}
    @Override public InteractionResult onActivated(Level level,BlockPos pos,Player player,InteractionHand hand,net.minecraft.world.item.ItemStack held,BlockHitResult hit){
        var be=getBlockEntity(level,pos);if(be==null)return InteractionResult.PASS;
        if(!level.isClientSide())MenuOpener.open(AWMenus.POWER_TOOLS.get(),player,MenuLocators.forBlockEntity(be));return InteractionResult.sidedSuccess(level.isClientSide());
    }
    @Override public boolean isSignalSource(BlockState state){return kind==MachineKind.EMITTER;}
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){var be=getBlockEntity(level,pos);return be==null?0:be.signal();}
    @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return getSignal(state,level,pos,side);}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof PowerBlockEntity be)be.logic().removed();super.onRemove(state,level,pos,next,moving);}
}
