package com.mpp.aedialsworks.cellterminal.integration.ae2;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.api.networking.IGrid;
import appeng.api.inventories.InternalInventory;
import com.mpp.aedialsworks.cellterminal.menu.TerminalHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.function.BiConsumer;
public final class WirelessCellTerminalHost extends WirelessTerminalMenuHost implements TerminalHost {
    private final TempCellInventory temp=new TempCellInventory(this::saveTerminal);
    public WirelessCellTerminalHost(Player player,Integer slot,ItemStack stack,BiConsumer<Player,ISubMenu> back) {
        super(player,slot,stack,back); if(stack.hasTag()) temp.read(stack.getTag());
    }
    @Override public IGrid terminalGrid() { var node=getActionableNode(); return node==null?null:node.getGrid(); }
    @Override public InternalInventory temporaryCells() { return temp; }
    @Override public void saveTerminal() { temp.write(getItemStack().getOrCreateTag()); getPlayer().getInventory().setChanged(); }
    @Override public boolean canUseTerminal(Player player) {
        if(player!=getPlayer()) return false;
        if(isClientSide()) return true;
        // Exact identity prevents a moved/replaced terminal from resurrecting an old stack.
        return getSlot()!=null && player.getInventory().getItem(getSlot())==getItemStack() && rangeCheck()
            && ((WirelessCellTerminalAdapter)getItemStack().getItem()).hasPower(player,0.5,getItemStack());
    }
    @Override public boolean onBroadcastChanges(AbstractContainerMenu menu) {
        return canUseTerminal(getPlayer()) && super.onBroadcastChanges(menu);
    }
}
