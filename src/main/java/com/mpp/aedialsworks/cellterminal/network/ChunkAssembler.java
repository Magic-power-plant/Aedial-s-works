package com.mpp.aedialsworks.cellterminal.network;
import java.io.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
/** Owned by one menu; disposal releases all inflight state. Duplicate/old frames cannot roll data back. */
public final class ChunkAssembler implements AutoCloseable {
    public record Payload(String channel, boolean full, CompoundTag tag) {}
    private final Map<String, Pending> pending = new HashMap<>();
    private final Map<String, Long> completed = new HashMap<>();
    public Optional<Payload> accept(PacketNBTChunk p, long now) throws IOException {
        pending.values().removeIf(value -> now - value.started > 30000);
        if (p.revision() <= completed.getOrDefault(p.channel(), 0L)) return Optional.empty();
        Pending assembly = pending.get(p.channel());
        if (assembly != null && p.revision() < assembly.revision) return Optional.empty();
        if (assembly == null || p.revision() > assembly.revision) {
            assembly = new Pending(p, now); pending.put(p.channel(), assembly);
        }
        if (p.total() != assembly.parts.length || p.full() != assembly.full) {
            pending.remove(p.channel()); throw new IOException("Inconsistent chunk metadata");
        }
        if (assembly.parts[p.index()] != null) return Optional.empty();
        if (p.bytes().length > ChunkCodec.MAX_COMPRESSED - assembly.bytes) {
            pending.remove(p.channel()); throw new IOException("Assembly exceeds limit");
        }
        assembly.parts[p.index()] = p.bytes().clone(); assembly.bytes += p.bytes().length; assembly.received++;
        if (assembly.received != assembly.parts.length) return Optional.empty();
        pending.remove(p.channel());
        var bytes = new ByteArrayOutputStream(assembly.bytes);
        for (var part : assembly.parts) bytes.write(part);
        var decoded = ChunkCodec.decode(bytes.toByteArray());
        completed.put(p.channel(), p.revision());
        return Optional.of(new Payload(p.channel(), p.full(), decoded));
    }
    @Override public void close() { pending.clear(); completed.clear(); }
    private static final class Pending {
        final long revision, started; final boolean full; final byte[][] parts; int bytes, received;
        Pending(PacketNBTChunk p, long now) { revision=p.revision(); started=now; full=p.full(); parts=new byte[p.total()][]; }
    }
}
