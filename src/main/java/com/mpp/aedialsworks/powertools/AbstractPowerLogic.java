package com.mpp.aedialsworks.powertools;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Owns domain lifecycle independently of the block/part presentation. */
public abstract class AbstractPowerLogic implements PowerConfigurable, IGridTickable {
    protected final PowerHost host;
    protected final IActionSource source;
    private boolean disposed;
    protected AbstractPowerLogic(PowerHost host) {
        this.host = host;
        this.source = IActionSource.ofMachine(host);
        host.powerNode().addService(IGridTickable.class, this);
    }
    public final PowerHost host() { return host; }
    protected final IGrid grid() { return host.powerNode().getGrid(); }
    protected final void changed() { host.powerChanged(); }
    @Override public TickingRequest getTickingRequest(IGridNode node) { return new TickingRequest(1, 20, false, true, 1); }
    @Override public TickRateModulation tickingRequest(IGridNode node, int elapsed) {
        if (disposed || !host.powerActive()) return TickRateModulation.SLOWER;
        tick(elapsed);
        return TickRateModulation.URGENT;
    }
    protected abstract void tick(int elapsed);
    public void unload() { disposed = true; }
    public void resume() { disposed = false; }
    public void removed() { unload(); }
    public abstract void load(CompoundTag tag);
    public abstract void save(CompoundTag tag);
    @Override public CompoundTag snapshot() { var tag = new CompoundTag(); save(tag); return tag; }
    @Override public boolean configure(ServerPlayer player, String action, CompoundTag value) { return false; }
}
