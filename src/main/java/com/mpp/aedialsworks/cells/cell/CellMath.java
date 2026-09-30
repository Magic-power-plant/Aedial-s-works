package com.mpp.aedialsworks.cells.cell;

import java.math.BigInteger;

/** Nonnegative storage arithmetic. Products are evaluated exactly before capping at AE2's long boundary. */
public final class CellMath {
    private static final BigInteger LIMIT = BigInteger.valueOf(Long.MAX_VALUE);
    private CellMath() {}
    public static long add(long a, long b) {
        require(a); require(b);
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }
    public static long multiply(long... factors) {
        BigInteger value = BigInteger.ONE;
        for (long factor : factors) { require(factor); value = value.multiply(BigInteger.valueOf(factor)); }
        return clamp(value);
    }
    public static long multiplyDivide(long a, long b, long divisor) {
        require(a); require(b);
        if (divisor <= 0) throw new IllegalArgumentException("Nonpositive divisor");
        return clamp(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)).divide(BigInteger.valueOf(divisor)));
    }
    public static long ceilDivide(long value, long divisor) {
        require(value);
        if (divisor <= 0) throw new IllegalArgumentException("Nonpositive divisor");
        return value / divisor + (value % divisor == 0 ? 0 : 1);
    }
    public static long capacity(long bytes, long overhead, int types, long multiplier, int units) {
        require(bytes); require(overhead);
        if (types < 0 || units <= 0 || multiplier <= 0) throw new IllegalArgumentException("Invalid accounting");
        var free = BigInteger.valueOf(bytes).subtract(BigInteger.valueOf(overhead).multiply(BigInteger.valueOf(types)));
        return clamp(free.max(BigInteger.ZERO).multiply(BigInteger.valueOf(multiplier)).multiply(BigInteger.valueOf(units)));
    }
    public static long clamp(BigInteger value) { return value.max(BigInteger.ZERO).min(LIMIT).longValueExact(); }
    private static void require(long value) { if (value < 0) throw new IllegalArgumentException("Negative amount"); }
}
