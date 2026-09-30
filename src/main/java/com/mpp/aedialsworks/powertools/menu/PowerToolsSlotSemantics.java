package com.mpp.aedialsworks.powertools.menu;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;

/** Independently positioned native slots for the original PowerTools page layouts. */
public final class PowerToolsSlotSemantics {
    public static final SlotSemantic[] CONFIG = register("config", 24);
    public static final SlotSemantic[] PATTERN = register("pattern", 12);
    private PowerToolsSlotSemantics() {}
    public static void initialize() {}
    private static SlotSemantic[] register(String kind, int count) {
        var result = new SlotSemantic[count];
        for (int i = 0; i < count; i++) result[i] = SlotSemantics.register("aedialsworks_" + kind + "_" + i, false);
        return result;
    }
}
