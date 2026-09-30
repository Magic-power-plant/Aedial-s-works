package com.mpp.aedialsworks.common.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;

/** Owns children, focus, pointer capture and disposal. Add directly with Screen.addRenderableWidget.
 * All coordinates are absolute; setPosition moves the children. Last added draws/receives clicks on top.
 */
public final class WidgetContainer extends AbstractContainerEventHandler implements IWidget {
    private final List<Child> entries = new ArrayList<>();
    private int x, y, width, height;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean closed;
    private boolean clip;
    private WidgetContainer owner;
    private GuiEventListener captured;
    private int capturedButton = -1;

    public WidgetContainer(int x, int y, int width, int height) {
        if (width < 0 || height < 0) throw new IllegalArgumentException("Negative container size");
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public <T extends AbstractWidget> T add(T child) {
        addEntry(new Child(child, child, child, child));
        return child;
    }

    public <T extends IWidget> T addCustom(T child) {
        if (child == this || child.isClosed()) throw new IllegalArgumentException("Invalid child lifecycle");
        addEntry(new Child(child, child, child, child));
        return child;
    }

    private void addEntry(Child child) {
        if (closed) throw new IllegalStateException("Container is closed");
        java.util.Objects.requireNonNull(child.events);
        if (child.events instanceof IWidget widget && widget.isClosed()) throw new IllegalArgumentException("Child is closed");
        if (entries.stream().anyMatch(entry -> entry.events == child.events)) throw new IllegalArgumentException("Duplicate child");
        if (child.events instanceof WidgetContainer container) {
            if (container.owner != null) throw new IllegalArgumentException("Container already has an owner");
            for (WidgetContainer ancestor = this; ancestor != null; ancestor = ancestor.owner) {
                if (ancestor == container) throw new IllegalArgumentException("Container ownership cycle");
            }
            container.owner = this;
        }
        entries.add(child);
    }

    public boolean remove(GuiEventListener child) {
        boolean removed = entries.removeIf(entry -> entry.events == child);
        if (!removed) return false;
        if (getFocused() == child) setFocused(null);
        if (captured == child) releaseCapture();
        if (child instanceof IWidget widget) widget.close();
        return true;
    }

    public void clear() {
        setFocused(null);
        releaseCapture();
        for (Child child : List.copyOf(entries)) if (child.events instanceof IWidget widget) widget.close();
        entries.clear();
    }

    public void setVisible(boolean value) { visible = value; if (!isVisible()) clearInput(); }
    public void setEnabled(boolean value) { enabled = value; if (!value) clearInput(); }
    public void setClip(boolean value) { clip = value; }
    private void clearInput() { setFocused(null); releaseCapture(); }
    private void releaseCapture() { captured = null; capturedButton = -1; setDragging(false); }

    @Override public boolean isVisible() { return visible && !closed; }
    @Override public boolean isClosed() { return closed; }
    @Override public int getX() { return x; }
    @Override public int getY() { return y; }
    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }
    @Override public void setX(int value) { int delta = value - x; x = value; entries.forEach(c -> c.layout.setX(c.layout.getX() + delta)); }
    @Override public void setY(int value) { int delta = value - y; y = value; entries.forEach(c -> c.layout.setY(c.layout.getY() + delta)); }
    public void setSize(int width, int height) {
        if (width < 0 || height < 0) throw new IllegalArgumentException("Negative container size");
        this.width = width; this.height = height;
    }
    @Override public void visitWidgets(Consumer<AbstractWidget> visitor) { entries.forEach(c -> c.layout.visitWidgets(visitor)); }

    private boolean acceptsInput() { return isVisible() && enabled; }
    private boolean inside(double mx, double my) { return mx >= x && my >= y && mx < (long) x + width && my < (long) y + height; }
    private boolean mayHit(double mx, double my) { return acceptsInput() && (!clip || inside(mx, my)); }

    @Override public List<? extends GuiEventListener> children() {
        if (!acceptsInput()) return List.of();
        return entries.stream().filter(Child::interactive).map(Child::events).toList();
    }

    @Override public boolean isMouseOver(double mx, double my) {
        return mayHit(mx, my) && (inside(mx, my) || getChildAt(mx, my).isPresent());
    }

    @Override public Optional<GuiEventListener> getChildAt(double mx, double my) {
        if (!mayHit(mx, my)) return Optional.empty();
        for (int i = entries.size() - 1; i >= 0; i--) {
            Child child = entries.get(i);
            if (child.interactive() && child.events.isMouseOver(mx, my)) return Optional.of(child.events);
        }
        return Optional.empty();
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (!mayHit(mx, my)) return false;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Child child = entries.get(i);
            if (child.interactive() && child.events.isMouseOver(mx, my) && child.events.mouseClicked(mx, my, button)) {
                // A button callback may have closed this GUI or removed/rebuilt its children.
                if (acceptsInput() && entries.contains(child) && child.interactive()) {
                    setFocused(child.events);
                    captured = child.events;
                    capturedButton = button;
                    setDragging(button == 0);
                }
                return true;
            }
        }
        clearInput();
        return false;
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (captured == null || button != capturedButton) return false;
        GuiEventListener target = captured;
        releaseCapture();
        target.mouseReleased(mx, my, button);
        return true;
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!acceptsInput() || captured == null || entries.stream().noneMatch(c -> c.events == captured && c.interactive())) {
            releaseCapture();
            return false;
        }
        return button == capturedButton && captured.mouseDragged(mx, my, button, dx, dy);
    }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        return getChildAt(mx, my).map(child -> child.mouseScrolled(mx, my, delta)).orElse(false);
    }

    private GuiEventListener inputFocus() {
        GuiEventListener focus = getFocused();
        if (!acceptsInput() || entries.stream().noneMatch(c -> c.events == focus && c.interactive())) {
            setFocused(null);
            return null;
        }
        return focus;
    }
    @Override public boolean keyPressed(int key, int scan, int mods) { var f = inputFocus(); return f != null && f.keyPressed(key, scan, mods); }
    @Override public boolean keyReleased(int key, int scan, int mods) { var f = inputFocus(); return f != null && f.keyReleased(key, scan, mods); }
    @Override public boolean charTyped(char value, int mods) { var f = inputFocus(); return f != null && f.charTyped(value, mods); }

    @Override public void render(GuiGraphics graphics, int mx, int my, float partialTick) {
        if (!isVisible()) return;
        if (clip) graphics.enableScissor(x, y, x + width, y + height);
        try {
            int hoverX = clip && !inside(mx, my) ? Integer.MIN_VALUE : mx;
            int hoverY = clip && !inside(mx, my) ? Integer.MIN_VALUE : my;
            for (Child child : List.copyOf(entries)) if (child.visible()) child.renderer.render(graphics, hoverX, hoverY, partialTick);
        } finally { if (clip) graphics.disableScissor(); }
    }

    @Override public void tick() {
        if (closed || !isVisible()) return;
        inputFocus();
        for (Child child : List.copyOf(entries)) {
            if (child.events instanceof IWidget widget) widget.tick();
            else if (child.events instanceof EditBox box) box.tick();
        }
    }

    @Override public void close() { if (!closed) { closed = true; clear(); } }
    @Override public NarrationPriority narrationPriority() {
        if (!acceptsInput()) return NarrationPriority.NONE;
        return entries.stream().filter(Child::interactive).map(c -> c.narration.narrationPriority())
                .max(java.util.Comparator.naturalOrder()).orElse(NarrationPriority.NONE);
    }
    @Override public void updateNarration(NarrationElementOutput output) {
        if (!acceptsInput()) return;
        entries.stream().filter(Child::interactive)
                .max(java.util.Comparator.comparing(c -> c.narration.narrationPriority()))
                .ifPresent(c -> c.narration.updateNarration(output.nest()));
    }

    private record Child(GuiEventListener events, Renderable renderer, NarratableEntry narration, LayoutElement layout) {
        boolean visible() {
            if (events instanceof IWidget widget) return widget.isVisible();
            return !(events instanceof AbstractWidget widget) || widget.visible;
        }
        boolean interactive() {
            return visible() && (!(events instanceof AbstractWidget widget) || widget.active)
                    && (!(events instanceof WidgetContainer container) || container.enabled);
        }
    }
}
