package com.mpp.aedialsworks.powertools;
public enum MachineKind {
    MAINTAINER("better_level_maintainer"), CRAFTER("auto_crafter"), EMITTER("storage_level_emitter"),
    DISPLAY("storage_display"), ALARM("storage_level_alarm");
    public final String id;
    MachineKind(String id) { this.id = id; }
}
