package com.mpp.aedialsworks.common.config;

/** Decimal strings deliberately preserve all 64 bits across legacy config migration. */
final class ConfigValidators {
    private ConfigValidators() {}

    static boolean signedLong(Object value) {
        if (!(value instanceof String text)) return false;
        try { Long.parseLong(text); return true; }
        catch (NumberFormatException e) { return false; }
    }

    static boolean positiveLong(Object value) {
        return signedLong(value) && Long.parseLong((String) value) > 0;
    }

    static boolean positiveLongOrUnlimited(Object value) {
        return positiveLong(value) || "-1".equals(value);
    }
}
