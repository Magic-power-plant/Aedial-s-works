package com.mpp.aedialsworks.smoke;

import com.mpp.aedialsworks.common.config.AWConfigs;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Runtime server-config overrides used by the dedicated-server smoke tests.
 *
 * <p>Forge 1.20.1 watches every config file and hot-reloads it whenever the file changes on disk
 * ({@code ConfigFileTypeHandler$ConfigWatcher} backed by NightConfig's {@code FileWatcher}). A bare
 * {@link ForgeConfigSpec.ConfigValue#set(Object)} only mutates the in-memory child config - it never
 * writes the file - so any reload that lands after the override silently restores whatever is still
 * stored on disk, i.e. the previous value.
 *
 * <p>That reload is delivered by a watcher thread, so on a slow CI runner it can arrive seconds after
 * the server config is first written at world creation - exactly inside a gametest callback window.
 * The permission tests then observed the gate still open and failed on one machine while passing on
 * another.
 *
 * <p>Persisting every override with {@link ForgeConfigSpec#save()} makes the file agree with the
 * override, so a reload is harmless and the value under test stays stable for the whole test.
 */
final class ConfigPins {
    private ConfigPins() {}

    /** Applies {@code expected} and keeps it effective even if Forge reloads the config file. */
    static <T> void pin(String name, ForgeConfigSpec.ConfigValue<T> value, T expected) {
        for (int attempt = 0; attempt < 4; attempt++) {
            value.set(expected);
            AWConfigs.SERVER_SPEC.save();
            if (expected.equals(value.get())) return;
        }
        throw new IllegalStateException("Server config value " + name + " could not be pinned to " + expected
                + " (still " + value.get() + "): a config reload keeps overwriting it");
    }

    /** Restores a value captured by the caller; safe to call from a {@code finally} block. */
    static <T> void restore(ForgeConfigSpec.ConfigValue<T> value, T previous) {
        value.set(previous);
        AWConfigs.SERVER_SPEC.save();
    }
}
