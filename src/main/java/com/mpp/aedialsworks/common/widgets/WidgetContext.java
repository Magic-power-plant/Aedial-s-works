package com.mpp.aedialsworks.common.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

/** Rendering capability; resolve dimensions lazily so GUI-scale changes do not leave stale bounds. */
public interface WidgetContext {
    Font font();
    int screenWidth();
    int screenHeight();

    static WidgetContext of(Screen screen) {
        return new WidgetContext() {
            @Override public Font font() { return Minecraft.getInstance().font; }
            @Override public int screenWidth() { return screen.width; }
            @Override public int screenHeight() { return screen.height; }
        };
    }
}
