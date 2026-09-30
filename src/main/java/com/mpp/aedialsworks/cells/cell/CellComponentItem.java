package com.mpp.aedialsworks.cells.cell;
import net.minecraft.world.item.Item;
public final class CellComponentItem extends Item {
    public final CellFamily family;
    public final CellTier tier;
    public CellComponentItem(CellFamily family,CellTier tier){super(new Properties());this.family=family;this.tier=tier;}
}
