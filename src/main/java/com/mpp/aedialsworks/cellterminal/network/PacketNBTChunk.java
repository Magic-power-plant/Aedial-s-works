package com.mpp.aedialsworks.cellterminal.network;
import java.util.UUID;
import com.mpp.aedialsworks.common.network.IAWPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
public record PacketNBTChunk(int containerId, UUID menuSession, String channel, long revision,
        boolean full, int index, int total, byte[] bytes) implements IAWPacket {
    public PacketNBTChunk {
        if (containerId < 0 || menuSession == null || !TerminalChannels.ALL.contains(channel) || revision < 1
            || total < 1 || total > ChunkCodec.MAX_CHUNKS || index < 0 || index >= total
            || bytes.length == 0 || bytes.length > ChunkCodec.MAX_FRAME) throw new IllegalArgumentException("Invalid chunk header");
        bytes = bytes.clone();
    }
    public static PacketNBTChunk read(FriendlyByteBuf b) {
        return new PacketNBTChunk(b.readVarInt(), b.readUUID(), b.readUtf(32), b.readVarLong(),
            b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readByteArray(ChunkCodec.MAX_FRAME));
    }
    @Override public void write(FriendlyByteBuf b) {
        b.writeVarInt(containerId); b.writeUUID(menuSession); b.writeUtf(channel,32); b.writeVarLong(revision);
        b.writeBoolean(full); b.writeVarInt(index); b.writeVarInt(total); b.writeByteArray(bytes);
    }
    @Override public void handle(NetworkEvent.Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.mpp.aedialsworks.cellterminal.client.TerminalClientPackets.receive(this));
    }
}
