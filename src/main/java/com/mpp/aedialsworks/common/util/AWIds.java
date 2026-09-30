package com.mpp.aedialsworks.common.util;

import com.mpp.aedialsworks.Aedialsworks;
import net.minecraft.resources.ResourceLocation;

/** Identifiers shared by registries, packets and generated assets. */
public final class AWIds {
    private AWIds() {}

    public static ResourceLocation id(String path) {
        return new ResourceLocation(Aedialsworks.MODID, path);
    }

    public static String translation(String category, String path) {
        return category + "." + Aedialsworks.MODID + "." + path;
    }
}
