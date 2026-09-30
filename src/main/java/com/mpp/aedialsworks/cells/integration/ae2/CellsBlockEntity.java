package com.mpp.aedialsworks.cells.integration.ae2;
import com.mpp.aedialsworks.cells.*;
import com.mpp.aedialsworks.cells.interfaceblock.InterfaceLogic;
import com.mpp.aedialsworks.cells.pattern.CompactingPatternLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
public final class CellsBlockEntity extends AbstractCellsBlockEntity {
    private final MachineKind kind;
    public CellsBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);kind=((CellsMachineBlock)state.getBlock()).kind;logic=kind.resourceInterface()?new InterfaceLogic(this):new CompactingPatternLogic(this);}
    @Override public MachineKind cellsKind(){return kind;}
    @Override public ItemLike cellsItem(){return getBlockState().getBlock();}
}
