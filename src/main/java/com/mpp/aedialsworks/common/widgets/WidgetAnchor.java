package com.mpp.aedialsworks.common.widgets;

import net.minecraft.client.gui.layouts.LayoutElement;

/** Converts legacy GUI-relative placement into native screen coordinates. */
public enum WidgetAnchor {
    GUI, SCREEN_CENTER;

    public void place(LayoutElement widget, WidgetContext context, int guiLeft, int guiTop, int x, int y) {
        int left = this == GUI ? guiLeft : (context.screenWidth() - widget.getWidth()) / 2;
        int top = this == GUI ? guiTop : (context.screenHeight() - widget.getHeight()) / 2;
        widget.setPosition(left + x, top + y);
    }
}
