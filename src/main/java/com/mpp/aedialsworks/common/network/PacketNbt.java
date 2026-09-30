package com.mpp.aedialsworks.common.network;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;

/** Small menu messages only. Bulk terminal snapshots use a separate chunk protocol in P1. */
public final class PacketNbt {
    public static final int MAX_BYTES = 16 * 1024;
    private static final long MAX_HEAP_BYTES = 128 * 1024;

    private PacketNbt() {}

    public static void write(FriendlyByteBuf buffer, CompoundTag tag) {
        try {
            var bytes = new ByteArrayOutputStream();
            NbtIo.write(tag, new DataOutputStream(bytes));
            if (bytes.size() > MAX_BYTES) throw new EncoderException("Menu NBT exceeds " + MAX_BYTES);
            buffer.writeByteArray(bytes.toByteArray());
        } catch (IOException e) {
            throw new EncoderException("Cannot encode menu NBT", e);
        }
    }

    public static CompoundTag read(FriendlyByteBuf buffer) {
        byte[] bytes = buffer.readByteArray(MAX_BYTES);
        try (var input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            CompoundTag tag = NbtIo.read(input, new NbtAccounter(MAX_HEAP_BYTES));
            if (tag == null || input.available() != 0) throw new DecoderException("Invalid menu NBT framing");
            return tag;
        } catch (IOException e) {
            throw new DecoderException("Cannot decode menu NBT", e);
        }
    }
}
