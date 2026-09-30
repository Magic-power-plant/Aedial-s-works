package com.mpp.aedialsworks.cellterminal.integration.ae2;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.core.AEConfig;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import com.mpp.aedialsworks.common.registry.AWMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
/** Uses the actual 15.4.11 access-point linking, power capacity, charging and range implementation. */
public abstract class WirelessCellTerminalAdapter extends WirelessTerminalItem {
    protected WirelessCellTerminalAdapter() { super(AEConfig.instance().getWirelessTerminalBattery(),new Item.Properties().stacksTo(1)); }
    @Override public MenuType<?> getMenuType() { return AWMenus.CELL_TERMINAL.get(); }
    @Override public ItemMenuHost getMenuHost(Player player,int slot,ItemStack stack,BlockPos pos) {
        return new WirelessCellTerminalHost(player,slot,stack,(p,menu)->openFromInventory(p,slot,true));
    }
}
