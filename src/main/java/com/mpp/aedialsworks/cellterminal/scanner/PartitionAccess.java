package com.mpp.aedialsworks.cellterminal.scanner;
import appeng.api.stacks.AEKey;
import appeng.api.config.FuzzyMode;
public interface PartitionAccess {
    int partitionSize();
    AEKey partitionKey(int slot);
    boolean isPartitionAllowed(AEKey key);
    boolean setPartition(int slot, AEKey key);
    void clearPartition();
    void cycleFuzzyMode();
}
