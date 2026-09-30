package com.mpp.aedialsworks.common.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PacketNbtTest {
    private CompoundTag payload() {
        var tag = new CompoundTag();
        tag.putLong("amount", Long.MAX_VALUE);
        tag.putString("name", "存储元件 / Ячейка");
        return tag;
    }

    @Test void menuMessagesRoundTripLongAmountsAndUnicode() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var data = payload();
            var key = new ResourceLocation("aedialsworks", "cellterminal/meta");
            var action = new PacketMenuAction(17, key, data);
            action.write(buffer);
            var read = PacketMenuAction.read(buffer);
            assertEquals(17, read.containerId());
            assertEquals(key, read.action());
            assertEquals(data, read.payload());
            assertFalse(buffer.isReadable());
            buffer.clear();
            var update = new PacketMenuData(17, key, data);
            update.write(buffer);
            assertEquals(update, PacketMenuData.read(buffer));
        } finally { buffer.release(); }
    }

    @Test void packetOwnsACopyOfTheMutableCallerTag() {
        var data = payload();
        var action = new PacketMenuAction(1, new ResourceLocation("aedialsworks", "test"), data);
        data.putLong("amount", 0);
        assertEquals(Long.MAX_VALUE, action.payload().getLong("amount"));
    }

    @Test void oversizedFramesAreRejectedBeforeAllocation() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(Integer.MAX_VALUE);
            assertThrows(DecoderException.class, () -> PacketNbt.read(buffer));
            buffer.clear();
            var data = new CompoundTag();
            data.putByteArray("tooBig", new byte[PacketNbt.MAX_BYTES]);
            assertThrows(EncoderException.class, () -> PacketNbt.write(buffer, data));
        } finally { buffer.release(); }
    }

    @Test void trailingBytesAndNonCompoundRootsAreRejected() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeByteArray(new byte[] {0});
            assertThrows(DecoderException.class, () -> PacketNbt.read(buffer));
            buffer.clear();
            buffer.writeByteArray(new byte[] {10, 0, 0, 0, 42});
            assertThrows(DecoderException.class, () -> PacketNbt.read(buffer));
        } finally { buffer.release(); }
    }
}
