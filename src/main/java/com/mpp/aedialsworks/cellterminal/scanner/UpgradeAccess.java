package com.mpp.aedialsworks.cellterminal.scanner;
import net.minecraft.world.item.ItemStack;
public interface UpgradeAccess {
    int upgradeSlots();
    ItemStack upgrade(int slot);
    ItemStack insertUpgrade(ItemStack stack);
    ItemStack extractUpgrade(int slot);
}
