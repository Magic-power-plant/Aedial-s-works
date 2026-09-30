package com.mpp.aedialsworks.cellterminal.scanner;
import appeng.api.networking.IGrid;
import net.minecraft.core.BlockPos;
import java.util.List;
public interface ISubnetScanner {
    record Link(IGrid from,IGrid to,BlockPos position,String dimension,boolean outbound) {}
    List<Link> connections(IGrid grid);
}
