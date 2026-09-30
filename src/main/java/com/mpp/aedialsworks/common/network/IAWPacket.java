package com.mpp.aedialsworks.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** A message capability. AWNetwork dispatches handlers on the receiving game thread. */
public interface IAWPacket {
    void write(FriendlyByteBuf buffer);
    void handle(NetworkEvent.Context context);
}
