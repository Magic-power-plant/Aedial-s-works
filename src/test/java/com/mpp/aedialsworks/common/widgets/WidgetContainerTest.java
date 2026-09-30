package com.mpp.aedialsworks.common.widgets;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WidgetContainerTest {
    @Test void topmostChildCapturesDragAndReleaseOutsideBounds() {
        var parent = new WidgetContainer(0, 0, 40, 40);
        var underneath = parent.addCustom(new Probe(0, 0));
        var top = parent.addCustom(new Probe(0, 0));
        assertTrue(parent.mouseClicked(5, 5, 0));
        assertEquals(0, underneath.clicks);
        assertEquals(1, top.clicks);
        assertSame(top, parent.getFocused());
        assertTrue(parent.mouseDragged(400, 400, 0, 395, 395));
        assertTrue(parent.mouseReleased(400, 400, 0));
        assertEquals(1, top.releases);
        assertFalse(parent.isDragging());
        assertFalse(parent.mouseDragged(400, 400, 0, 1, 1));
    }

    @Test void hiddenDisabledAndClippedChildrenDoNotReceiveInput() {
        var parent = new WidgetContainer(0, 0, 10, 10);
        var child = parent.addCustom(new Probe(15, 15));
        parent.setClip(true);
        assertFalse(parent.mouseClicked(16, 16, 0));
        parent.setClip(false);
        assertTrue(parent.mouseClicked(16, 16, 0));
        child.visible = false;
        assertFalse(parent.keyPressed(42, 0, 0));
        assertNull(parent.getFocused());
        child.visible = true;
        parent.setEnabled(false);
        assertFalse(parent.mouseClicked(16, 16, 0));
        parent.setEnabled(true);
        parent.setVisible(false);
        assertFalse(parent.mouseClicked(16, 16, 0));
    }

    @Test void movingAndDisposingAContainerMovesAndClosesItsChildrenExactlyOnce() {
        var parent = new WidgetContainer(0, 0, 40, 40);
        var child = parent.addCustom(new Probe(5, 6));
        parent.setPosition(30, 40);
        assertEquals(35, child.getX());
        assertEquals(46, child.getY());
        parent.tick();
        assertEquals(1, child.ticks);
        parent.close();
        parent.close();
        parent.tick();
        assertEquals(1, child.closes);
        assertEquals(1, child.ticks);
        assertTrue(parent.children().isEmpty());
        assertThrows(IllegalStateException.class, () -> parent.addCustom(new Probe(0, 0)));
    }

    @Test void removalClearsFocusAndCaptureAndRejectsDuplicateOwnership() {
        var parent = new WidgetContainer(0, 0, 40, 40);
        var child = parent.addCustom(new Probe(0, 0));
        assertThrows(IllegalArgumentException.class, () -> parent.addCustom(child));
        parent.mouseClicked(5, 5, 0);
        assertTrue(parent.remove(child));
        assertNull(parent.getFocused());
        assertFalse(parent.mouseReleased(5, 5, 0));
        assertEquals(1, child.closes);
        assertFalse(parent.remove(child));
    }

    @Test void nestedContainersRejectCyclesAndMultipleOwners() {
        var root = new WidgetContainer(0, 0, 40, 40);
        var nested = root.addCustom(new WidgetContainer(0, 0, 20, 20));
        assertThrows(IllegalArgumentException.class, () -> nested.addCustom(root));
        var other = new WidgetContainer(0, 0, 40, 40);
        assertThrows(IllegalArgumentException.class, () -> other.addCustom(nested));
        root.close();
        assertTrue(nested.isClosed());
    }

    @Test void callbacksMayCloseTheParentWithoutLeavingCaptureOrFocus() {
        var parent = new WidgetContainer(0, 0, 40, 40);
        var child = parent.addCustom(new Probe(0, 0));
        child.onClick = parent::close;
        assertTrue(parent.mouseClicked(5, 5, 0));
        assertNull(parent.getFocused());
        assertFalse(parent.isDragging());
        assertFalse(parent.mouseDragged(6, 6, 0, 1, 1));
        assertFalse(parent.mouseReleased(6, 6, 0));
    }

    private static final class Probe implements IWidget {
        int x, y, clicks, releases, ticks, closes;
        boolean visible = true, focused;
        Runnable onClick = () -> {};
        Probe(int x, int y) { this.x = x; this.y = y; }
        @Override public int getX() { return x; }
        @Override public int getY() { return y; }
        @Override public int getWidth() { return 20; }
        @Override public int getHeight() { return 20; }
        @Override public void setX(int x) { this.x = x; }
        @Override public void setY(int y) { this.y = y; }
        @Override public boolean isVisible() { return visible && !isClosed(); }
        @Override public boolean isClosed() { return closes > 0; }
        @Override public boolean isMouseOver(double mx, double my) { return mx >= x && my >= y && mx < x + 20 && my < y + 20; }
        @Override public boolean mouseClicked(double mx, double my, int button) { clicks++; onClick.run(); return true; }
        @Override public boolean mouseReleased(double mx, double my, int button) { releases++; return true; }
        @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) { return true; }
        @Override public boolean keyPressed(int key, int scan, int mods) { return true; }
        @Override public boolean isFocused() { return focused; }
        @Override public void setFocused(boolean value) { focused = value; }
        @Override public void tick() { ticks++; }
        @Override public void close() { closes++; }
        @Override public void render(GuiGraphics graphics, int mx, int my, float partial) {}
        @Override public void visitWidgets(Consumer<AbstractWidget> visitor) {}
        @Override public NarrationPriority narrationPriority() { return focused ? NarrationPriority.FOCUSED : NarrationPriority.NONE; }
        @Override public void updateNarration(NarrationElementOutput output) {}
    }
}
