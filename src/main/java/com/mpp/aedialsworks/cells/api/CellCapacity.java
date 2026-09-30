package com.mpp.aedialsworks.cells.api;

/** True long accounting, distinct from IBasicCellItem's legacy int presentation API. */
public interface CellCapacity {
    long totalBytes();
    long usedBytes();
    long storedUnits();
    int storedTypes();
    int maximumTypes();
}
