package com.mpp.aedialsworks.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

/** Specs exist during construction; values must not be read during registry declaration. */
public final class AWConfigs {
    private static final Pair<AWCommonConfig, ForgeConfigSpec> COMMON_PAIR =
            new ForgeConfigSpec.Builder().configure(AWCommonConfig::new);
    private static final Pair<AWServerConfig, ForgeConfigSpec> SERVER_PAIR =
            new ForgeConfigSpec.Builder().configure(AWServerConfig::new);
    private static final Pair<AWClientConfig, ForgeConfigSpec> CLIENT_PAIR =
            new ForgeConfigSpec.Builder().configure(AWClientConfig::new);
    public static final AWCommonConfig COMMON = COMMON_PAIR.getLeft();
    public static final AWServerConfig SERVER = SERVER_PAIR.getLeft();
    public static final AWClientConfig CLIENT = CLIENT_PAIR.getLeft();
    public static final ForgeConfigSpec COMMON_SPEC = COMMON_PAIR.getRight();
    public static final ForgeConfigSpec SERVER_SPEC = SERVER_PAIR.getRight();
    public static final ForgeConfigSpec CLIENT_SPEC = CLIENT_PAIR.getRight();

    private AWConfigs() {}

    public static void register(ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        context.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }
}
