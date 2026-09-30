package com.mpp.aedialsworks.common.network;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** S2C data. Client classes are only resolved on the physical client. */
public record PacketMenuData(int containerId, ResourceLocation channel, CompoundTag payload) implements IAWPacket {
    public PacketMenuData {
        if (containerId < 0) throw new IllegalArgumentException("Negative menu id");
        Objects.requireNonNull(channel);
        payload = Objects.requireNonNull(payload).copy();
    }

    public static PacketMenuData read(FriendlyByteBuf buffer) {
        return new PacketMenuData(buffer.readVarInt(), buffer.readResourceLocation(), PacketNbt.read(buffer));
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeResourceLocation(channel);
        PacketNbt.write(buffer, payload);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.mpp.aedialsworks.common.network.client.ClientPackets.receive(this));
    }
}
