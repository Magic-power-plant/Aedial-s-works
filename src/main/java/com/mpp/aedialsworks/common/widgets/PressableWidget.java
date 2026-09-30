package com.mpp.aedialsworks.common.widgets;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Shared button: mouse, keyboard, focus narration, and native tooltips. */
public class PressableWidget extends AbstractAWWidget {
    private final Runnable action;

    public PressableWidget(int x, int y, int width, int height, Component text, Runnable action) {
        super(x, y, width, height, text);
        this.action = Objects.requireNonNull(action);
    }

    @Override public void onClick(double mouseX, double mouseY) { press(); }

    protected void press() {
        if (!isClosed() && visible && active) action.run();
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!isClosed() && active && visible && isFocused()
                && (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            press();
            return true;
        }
        return false;
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int background = !active ? 0xFF303030 : isHoveredOrFocused() ? 0xFF606C80 : 0xFF454B55;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
        graphics.renderOutline(getX(), getY(), width, height, isFocused() ? 0xFFFFFFFF : 0xFF858B95);
        renderScrollingString(graphics, Minecraft.getInstance().font, 3, active ? 0xFFFFFFFF : 0xFFA0A0A0);
    }
}
