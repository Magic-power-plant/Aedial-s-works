package com.mpp.aedialsworks.powertools;

import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Capabilities supplied by either a block, cable part, or wireless host. */
public interface PowerHost extends IActionHost {
    IManagedGridNode powerNode();
    Level powerLevel();
    BlockPos powerPos();
    void powerChanged();
    default boolean powerActive() { return powerNode().isActive(); }
    default void conditionChanged(boolean active) { powerChanged(); }
}
