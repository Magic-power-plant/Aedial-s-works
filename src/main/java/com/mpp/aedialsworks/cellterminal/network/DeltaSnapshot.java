package com.mpp.aedialsworks.cellterminal.network;
import java.util.*;
import net.minecraft.nbt.*;
/** Per-menu, per-channel full/delta state. Explicit commit only after successful encoding. */
public final class DeltaSnapshot {
    public record Update(CompoundTag payload, boolean full, CompoundTag snapshot) {}
    private final Map<String, CompoundTag> previous = new HashMap<>();
    public void reset() { previous.clear(); }
    public void commit(String channel, Update update) { previous.put(channel, update.snapshot().copy()); }
    public Update prepare(String channel, CompoundTag next, boolean delta) {
        var old = previous.get(channel);
        if (old == null || !delta) return new Update(next.copy(), true, next.copy());
        if (old.equals(next)) return null;
        var before = entries(old); var after = entries(next);
        var payload = next.copy(); payload.remove("entries");
        var added = new ListTag(); var updated = new ListTag(); var removed = new ListTag();
        after.forEach((id, entry) -> { if (!before.containsKey(id)) added.add(entry.copy());
            else if (!before.get(id).equals(entry)) updated.add(entry.copy()); });
        before.keySet().stream().filter(id -> !after.containsKey(id)).forEach(id -> removed.add(LongTag.valueOf(id)));
        payload.put("added", added); payload.put("updated", updated); payload.put("removed", removed);
        return new Update(payload, false, next.copy());
    }
    public static LinkedHashMap<Long, CompoundTag> entries(CompoundTag tag) {
        var map = new LinkedHashMap<Long, CompoundTag>();
        for (var value : tag.getList("entries", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            if (!entry.contains("id", Tag.TAG_LONG) || map.put(entry.getLong("id"), entry.copy()) != null)
                throw new IllegalArgumentException("Missing or duplicate snapshot id");
        }
        return map;
    }
    public static CompoundTag apply(CompoundTag old, CompoundTag payload, boolean full) {
        if (full) { entries(payload); return payload.copy(); }
        if (old == null) throw new IllegalStateException("Delta before full snapshot");
        var map = entries(old);
        for (var id : payload.getList("removed", Tag.TAG_LONG)) map.remove(((LongTag) id).getAsLong());
        for (String list : List.of("added", "updated")) for (var value : payload.getList(list, Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            map.put(entry.getLong("id"), entry.copy());
        }
        var result = payload.copy(); result.remove("added"); result.remove("updated"); result.remove("removed");
        var entries = new ListTag(); map.values().forEach(entries::add); result.put("entries", entries);
        return result;
    }
}
