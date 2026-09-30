package com.mpp.aedialsworks.powertools.items;
import net.minecraft.world.item.Item;
public final class CrafterSpeedUpgrade extends Item {
    private final int tier;
    public CrafterSpeedUpgrade(int tier){super(new Properties());this.tier=tier;}
    public int tier(){return tier;}
    public int multiplier(){return 1 << (3*tier);}
}
