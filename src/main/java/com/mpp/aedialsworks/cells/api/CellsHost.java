package com.mpp.aedialsworks.cells.api;
import java.util.Set;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import com.mpp.aedialsworks.cells.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
public interface CellsHost extends IActionHost {
    IManagedGridNode cellsNode();
    Level cellsLevel();
    BlockPos cellsPos();
    MachineKind cellsKind();
    ItemLike cellsItem();
    AbstractCellsLogic cellsLogic();
    void cellsChanged();
    boolean canUseCells(Player player);
    Set<Direction> targetSides();
}
