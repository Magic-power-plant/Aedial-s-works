package com.mpp.aedialsworks.cellterminal.menu;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import net.minecraft.world.entity.player.Player;
/** Domain capabilities shared by a cable-mounted part and an inventory-hosted terminal. */
public interface TerminalHost {
    IGrid terminalGrid();
    InternalInventory temporaryCells();
    boolean canUseTerminal(Player player);
    void saveTerminal();
}
