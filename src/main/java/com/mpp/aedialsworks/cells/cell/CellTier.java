package com.mpp.aedialsworks.cells.cell;

/** Metadata variants become independent, stable registry IDs in 1.20. */
public enum CellTier {
    K1("1k",1024), K4("4k",4096), K16("16k",16384), K64("64k",65536),
    K256("256k",262144), M1("1m",1048576), M4("4m",4194304), M16("16m",16777216),
    M64("64m",67108864), M256("256m",268435456), G1("1g",1073741824), G2("2g",2147483648L);
    public final String id;
    public final long bytes;
    CellTier(String id,long bytes) { this.id=id; this.bytes=bytes; }
}
