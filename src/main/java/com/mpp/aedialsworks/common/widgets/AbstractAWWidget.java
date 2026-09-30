package com.mpp.aedialsworks.common.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Owns widget lifecycle/state; native AbstractWidget supplies focus, tooltip and narration integration. */
public abstract class AbstractAWWidget extends AbstractWidget implements IWidget {
    private boolean closed;

    protected AbstractAWWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        if (width < 0 || height < 0) throw new IllegalArgumentException("Negative widget size");
    }

    @Override public final boolean isClosed() { return closed; }
    @Override public final boolean isVisible() { return visible && !closed; }

    public final void setVisible(boolean value) {
        visible = value && !closed;
        if (!visible) setFocused(false);
    }

    public final void setEnabled(boolean value) {
        active = value && !closed;
        if (!active) setFocused(false);
    }

    @Override public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (isVisible()) super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public final void tick() {
        if (!closed) onTick();
    }

    protected void onTick() {}
    protected void onClose() {}

    @Override public final void close() {
        if (closed) return;
        closed = true;
        visible = false;
        active = false;
        setFocused(false);
        onClose();
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
