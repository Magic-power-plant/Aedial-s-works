package com.mpp.aedialsworks.cellterminal.network;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import net.minecraft.nbt.*;
/** Hard caps are independent of administrator config and protect both the wire and NBT heap. */
public final class ChunkCodec {
    public static final int MAX_FRAME = 128 * 1024, MAX_COMPRESSED = 8 * 1024 * 1024, MAX_DECODED = 32 * 1024 * 1024;
    public static final int MAX_CHUNKS = MAX_COMPRESSED / 4096;
    private ChunkCodec() {}
    public static List<byte[]> encode(CompoundTag tag, int configuredSize) throws IOException {
        var raw = new ByteArrayOutputStream();
        NbtIo.write(tag, new DataOutputStream(new LimitedOutput(raw, MAX_DECODED)));
        var packed = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(new LimitedOutput(packed, MAX_COMPRESSED))) { raw.writeTo(gzip); }
        byte[] bytes = packed.toByteArray();
        int size = Math.max(4096, Math.min(MAX_FRAME, configuredSize));
        var chunks = new ArrayList<byte[]>();
        for (int i = 0; i < bytes.length; i += size) chunks.add(Arrays.copyOfRange(bytes, i, Math.min(i + size, bytes.length)));
        return chunks;
    }
    public static CompoundTag decode(byte[] bytes) throws IOException {
        if (bytes.length > MAX_COMPRESSED) throw new IOException("Compressed snapshot exceeds limit");
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            var raw = gzip.readNBytes(MAX_DECODED + 1);
            if (raw.length > MAX_DECODED) throw new IOException("Expanded snapshot exceeds limit");
            return NbtIo.read(new DataInputStream(new ByteArrayInputStream(raw)), new NbtAccounter(MAX_DECODED));
        }
    }
    private static final class LimitedOutput extends FilterOutputStream {
        private final int limit; private int written;
        LimitedOutput(OutputStream stream, int limit) { super(stream); this.limit = limit; }
        private void reserve(int count) throws IOException { if (count > limit - written) throw new IOException("Snapshot exceeds limit"); written += count; }
        @Override public void write(int b) throws IOException { reserve(1); out.write(b); }
        @Override public void write(byte[] b, int off, int len) throws IOException { reserve(len); out.write(b, off, len); }
    }
}
