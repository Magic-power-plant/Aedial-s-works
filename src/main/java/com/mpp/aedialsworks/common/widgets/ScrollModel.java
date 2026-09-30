package com.mpp.aedialsworks.common.widgets;

/** Pure state model shared by virtual terminal lists and scrollbars, independently testable. */
public final class ScrollModel {
    private int contentHeight;
    private int viewportHeight;
    private int offset;

    public void setExtent(int contentHeight, int viewportHeight) {
        if (contentHeight < 0 || viewportHeight < 0) throw new IllegalArgumentException("Negative scroll extent");
        this.contentHeight = contentHeight;
        this.viewportHeight = viewportHeight;
        setOffset(offset);
    }

    public int maxOffset() { return Math.max(0, contentHeight - viewportHeight); }
    public int offset() { return offset; }
    public int contentHeight() { return contentHeight; }
    public int viewportHeight() { return viewportHeight; }
    public double fraction() { return maxOffset() == 0 ? 0 : (double) offset / maxOffset(); }

    public boolean setOffset(long value) {
        int next = (int) Math.max(0, Math.min((long) maxOffset(), value));
        boolean changed = next != offset;
        offset = next;
        return changed;
    }

    public boolean scroll(double wheelDelta, int pixelsPerStep) {
        if (!Double.isFinite(wheelDelta) || pixelsPerStep < 1) return false;
        double next = offset - wheelDelta * pixelsPerStep;
        return setOffset(next <= 0 ? 0 : next >= maxOffset() ? maxOffset() : Math.round(next));
    }

    public boolean setFraction(double fraction) {
        if (!Double.isFinite(fraction)) return false;
        return setOffset(Math.round(Math.max(0, Math.min(1, fraction)) * maxOffset()));
    }
}
