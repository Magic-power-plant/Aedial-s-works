package com.mpp.aedialsworks.cells.api;
import appeng.api.networking.IGrid;
/** Internal capability consumed directly by the terminal and diagnostics. */
public interface SubnetView {
    IGrid originGrid();
    IGrid destinationGrid();
    String guardStatus();
}
