package com.mpp.aedialsworks.common.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Menu capability; implementations must validate each action, permissions and all payload values. */
public interface ServerMenuReceiver {
    void receiveClientAction(ServerPlayer sender, ResourceLocation action, CompoundTag payload);
}
