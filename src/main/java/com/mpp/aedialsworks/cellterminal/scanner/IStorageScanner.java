package com.mpp.aedialsworks.cellterminal.scanner;
import appeng.api.networking.IGrid;
import java.util.List;
/** Registration point for later integrations without depending on any legacy API stubs. */
public interface IStorageScanner extends AutoCloseable {
    List<AbstractTerminalTarget> scan(IGrid grid);
    List<AbstractTerminalTarget> scanAll(java.util.Collection<IGrid> grids);
    @Override void close();
}
