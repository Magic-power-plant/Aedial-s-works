package com.mpp.aedialsworks.powertools.monitor;
public enum Comparison {
    LESS, LESS_EQUAL, EQUAL, GREATER_EQUAL, GREATER, NOT_EQUAL;
    public boolean test(long amount, long threshold) {
        return switch (this) {
            case LESS -> amount < threshold; case LESS_EQUAL -> amount <= threshold;
            case EQUAL -> amount == threshold; case GREATER_EQUAL -> amount >= threshold;
            case GREATER -> amount > threshold; case NOT_EQUAL -> amount != threshold;
        };
    }
    public static Comparison byId(int id) { return values()[Math.floorMod(id, values().length)]; }
}
