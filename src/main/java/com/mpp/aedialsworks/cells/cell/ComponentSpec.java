package com.mpp.aedialsworks.cells.cell;
import net.minecraft.world.item.ItemStack;
/** Native AE2 components are channel-neutral in 1.20; a configurable casing selects item or fluid while empty. */
public record ComponentSpec(CellFamily family,CellTier tier) {
    public static ComponentSpec of(ItemStack stack){if(stack.getItem() instanceof CellComponentItem c)return new ComponentSpec(c.family,c.tier);if(stack.getItem() instanceof appeng.items.materials.StorageComponentItem c)for(var tier:CellTier.values())if(c.getBytes(stack)==tier.bytes)return new ComponentSpec(CellFamily.CONFIGURABLE,tier);return null;}
}
