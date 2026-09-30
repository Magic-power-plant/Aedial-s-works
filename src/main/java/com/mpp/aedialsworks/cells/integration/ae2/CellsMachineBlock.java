package com.mpp.aedialsworks.cells.integration.ae2;
import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.mpp.aedialsworks.cells.MachineKind;
import com.mpp.aedialsworks.common.registry.AWMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
public final class CellsMachineBlock extends AEBaseEntityBlock<CellsBlockEntity> {
    public final MachineKind kind;
    public CellsMachineBlock(MachineKind kind){super(Properties.of().strength(3).requiresCorrectToolForDrops());this.kind=kind;}
    @Override public appeng.api.orientation.IOrientationStrategy getOrientationStrategy(){return appeng.api.orientation.OrientationStrategies.facing();}
    @Override public InteractionResult onActivated(Level level,BlockPos pos,Player p,InteractionHand hand,ItemStack held,BlockHitResult hit){var be=getBlockEntity(level,pos);if(be==null)return InteractionResult.PASS;if(!level.isClientSide())MenuOpener.open(AWMenus.CELLS.get(),p,MenuLocators.forBlockEntity(be));return InteractionResult.sidedSuccess(level.isClientSide());}
}
