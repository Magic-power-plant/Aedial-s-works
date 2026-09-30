package com.mpp.aedialsworks.common.widgets;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;

/** Client-only capabilities shared by terminal and PowerTools widgets. Use screen coordinates. */
public interface IWidget extends Renderable, GuiEventListener, LayoutElement, NarratableEntry, AutoCloseable {
    @Override default net.minecraft.client.gui.navigation.ScreenRectangle getRectangle() {
        return LayoutElement.super.getRectangle();
    }

    boolean isVisible();
    boolean isClosed();
    void tick();
    @Override void close();
}
