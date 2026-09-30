package com.mpp.aedialsworks.common.network;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

/** C2S action addressed to the currently open menu, never to an arbitrary world position. */
public record PacketMenuAction(int containerId, ResourceLocation action, CompoundTag payload) implements IAWPacket {
    public PacketMenuAction {
        if (containerId < 0) throw new IllegalArgumentException("Negative menu id");
        Objects.requireNonNull(action);
        payload = Objects.requireNonNull(payload).copy();
    }

    public static PacketMenuAction read(FriendlyByteBuf buffer) {
        return new PacketMenuAction(buffer.readVarInt(), buffer.readResourceLocation(), PacketNbt.read(buffer));
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeResourceLocation(action);
        PacketNbt.write(buffer, payload);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        var sender = context.getSender();
        if (sender == null) return;
        var menu = sender.containerMenu;
        if (menu.containerId == containerId && menu.stillValid(sender) && menu instanceof ServerMenuReceiver receiver) {
            receiver.receiveClientAction(sender, action, payload.copy());
        }
    }
}
