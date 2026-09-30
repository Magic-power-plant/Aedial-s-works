package com.mpp.aedialsworks.cellterminal.integration.ae2;
import appeng.items.tools.NetworkToolItem;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.api.inventories.InternalInventory;
import net.minecraft.world.entity.player.Player;
public final class ToolboxAdapter {
    private ToolboxAdapter() {}
    public static int find(Player player) {for(int i=0;i<player.getInventory().items.size();i++)if(player.getInventory().getItem(i).getItem() instanceof NetworkToolItem)return i;return -1;}
    public static InternalInventory inventory(Player player,int slot) {
        if(slot<0 || !(player.getInventory().getItem(slot).getItem() instanceof NetworkToolItem))return InternalInventory.empty();
        return new NetworkToolMenuHost(player,slot,player.getInventory().getItem(slot),null).getInternalInventory();
    }
}
