package com.mpp.aedialsworks.cells.api;

import appeng.api.stacks.AEKey;

/** A resource capability; implementations may support exact, fuzzy, inverse or tag matching. */
@FunctionalInterface
public interface ResourceFilter {
    boolean accepts(AEKey key);
}
