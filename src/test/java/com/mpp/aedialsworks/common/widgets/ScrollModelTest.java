package com.mpp.aedialsworks.common.widgets;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScrollModelTest {
    @Test void emptyAndSmallListsHaveNoScrollRange() {
        var model = new ScrollModel();
        model.setExtent(30, 50);
        assertEquals(0, model.maxOffset());
        assertFalse(model.scroll(-1, 18));
        assertEquals(0, model.fraction());
    }

    @Test void resizeClampsOffsetAndGrowingPreservesPosition() {
        var model = new ScrollModel();
        model.setExtent(1000, 100);
        model.setOffset(800);
        model.setExtent(300, 100);
        assertEquals(200, model.offset());
        model.setExtent(1000, 100);
        assertEquals(200, model.offset());
        model.setExtent(1000, 1000);
        assertEquals(0, model.offset());
    }

    @Test void wheelsAndFractionsClampWithoutIntegerOverflow() {
        var model = new ScrollModel();
        model.setExtent(Integer.MAX_VALUE, 18);
        model.setOffset(Long.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE - 18, model.offset());
        model.scroll(Double.MAX_VALUE, 18);
        assertEquals(0, model.offset());
        model.scroll(-Double.MAX_VALUE, 18);
        assertEquals(model.maxOffset(), model.offset());
        model.setFraction(.5);
        assertEquals(Math.round(model.maxOffset() * .5), model.offset());
        assertFalse(model.setFraction(Double.NaN));
        assertFalse(model.scroll(Double.POSITIVE_INFINITY, 18));
        assertThrows(IllegalArgumentException.class, () -> model.setExtent(-1, 0));
    }
}
