package com.mpp.aedialsworks.common.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Client menu capability without a dependency on any physical-client class. */
public interface ClientMenuReceiver {
    void receiveServerData(ResourceLocation channel, CompoundTag payload);
}
