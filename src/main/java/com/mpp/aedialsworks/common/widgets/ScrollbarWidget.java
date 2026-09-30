package com.mpp.aedialsworks.common.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Pixel-based scrollbar; the owner renders only visible rows using model.offset(). */
public final class ScrollbarWidget extends AbstractAWWidget {
    private final ScrollModel model;
    private final int step;
    private double grabOffset;

    public ScrollbarWidget(int x, int y, int height, ScrollModel model, int step) {
        super(x, y, 8, height, Component.translatable("gui.aedialsworks.widgets.scroll"));
        if (step < 1) throw new IllegalArgumentException("Scroll step must be positive");
        this.model = java.util.Objects.requireNonNull(model);
        this.step = step;
    }

    private int thumbHeight() {
        if (model.contentHeight() == 0) return height;
        return Math.min(height, Math.max(8, (int) ((long) height * model.viewportHeight() / model.contentHeight())));
    }

    private int thumbY() { return getY() + (int) Math.round((height - thumbHeight()) * model.fraction()); }

    @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
        g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF20242A);
        g.fill(getX() + 1, thumbY(), getX() + width - 1, thumbY() + thumbHeight(),
                isHoveredOrFocused() ? 0xFFBCCADB : 0xFF8794A5);
    }

    @Override public void onClick(double x, double y) {
        int thumb = thumbHeight();
        grabOffset = y >= thumbY() && y < thumbY() + thumb ? y - thumbY() : thumb / 2.0;
        moveThumb(y);
    }

    @Override protected void onDrag(double x, double y, double dx, double dy) { moveThumb(y); }

    private void moveThumb(double y) {
        int track = height - thumbHeight();
        if (active && visible && !isClosed() && track > 0) model.setFraction((y - getY() - grabOffset) / track);
    }

    @Override public boolean mouseScrolled(double x, double y, double delta) {
        return active && isVisible() && isMouseOver(x, y) && model.scroll(delta, step);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!active || !isVisible() || !isFocused()) return false;
        return switch (key) {
            case GLFW.GLFW_KEY_UP -> model.setOffset((long) model.offset() - step);
            case GLFW.GLFW_KEY_DOWN -> model.setOffset((long) model.offset() + step);
            case GLFW.GLFW_KEY_PAGE_UP -> model.setOffset((long) model.offset() - model.viewportHeight());
            case GLFW.GLFW_KEY_PAGE_DOWN -> model.setOffset((long) model.offset() + model.viewportHeight());
            case GLFW.GLFW_KEY_HOME -> model.setOffset(0);
            case GLFW.GLFW_KEY_END -> model.setOffset(model.maxOffset());
            default -> false;
        };
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
        output.add(NarratedElementType.POSITION, Component.literal(Math.round(model.fraction() * 100) + "%"));
    }
}
