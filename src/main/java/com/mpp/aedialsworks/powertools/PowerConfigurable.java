package com.mpp.aedialsworks.powertools;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** A menu capability; packet callers must validate the live host before dispatch. */
public interface PowerConfigurable {
    CompoundTag snapshot();
    boolean configure(ServerPlayer player, String action, CompoundTag value);
}
