package com.mpp.aedialsworks.powertools.integration.ae2;
import appeng.items.parts.PartItem;
import net.minecraft.world.item.Item;
public final class PowerPartItem extends PartItem<MonitorPart> {
    public final int size;
    public final boolean emitter;
    public PowerPartItem(boolean emitter,int size){super(new Item.Properties(),MonitorPart.class,MonitorPart::new);this.emitter=emitter;this.size=size;}
}
