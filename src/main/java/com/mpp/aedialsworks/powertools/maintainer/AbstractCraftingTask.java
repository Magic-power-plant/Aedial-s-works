package com.mpp.aedialsworks.powertools.maintainer;
import java.util.concurrent.Future;
import appeng.api.networking.crafting.*;
/** Owns transient calculation and persistent CPU-link lifecycle. */
public abstract class AbstractCraftingTask {
    public enum State { IDLE, CALCULATING, WAITING_CPU, CRAFTING, MISSING_RESOURCES, NOT_CRAFTABLE, OFFLINE }
    public State state = State.IDLE;
    protected Future<ICraftingPlan> future;
    protected ICraftingPlan plan;
    protected ICraftingLink link;
    protected int retries;
    public boolean busy() { return future!=null || plan!=null || link!=null; }
    public void unload() {
        if(future!=null) future.cancel(true);
        future=null; plan=null; retries=0;
        state=link==null?State.IDLE:State.CRAFTING;
    }
    public void cancel() { unload(); if(link!=null) link.cancel(); link=null; state=State.IDLE; }
}
