package com.mpp.aedialsworks.cellterminal.network;
import java.util.Set;
public final class TerminalChannels {
    public static final String META="ct:meta", STORAGES="ct:storages", BUSES="ct:buses", TEMP_CELLS="ct:temp", SUBNETS="ct:subnets";
    public static final Set<String> ALL = Set.of(META, STORAGES, BUSES, TEMP_CELLS, SUBNETS, "pt:state", "cells:state","cells:item");
    private TerminalChannels() {}
}
