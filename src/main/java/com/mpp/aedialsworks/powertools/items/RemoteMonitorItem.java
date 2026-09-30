package com.mpp.aedialsworks.powertools.items;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.core.AEConfig;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import com.mpp.aedialsworks.common.registry.AWMenus;
import com.mpp.aedialsworks.common.network.AWNetwork;
import com.mpp.aedialsworks.powertools.integration.ae2.WirelessPowerHost;
import com.mpp.aedialsworks.powertools.network.PacketPowerHud;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public final class RemoteMonitorItem extends WirelessTerminalItem {
    public RemoteMonitorItem(){super(AEConfig.instance().getWirelessTerminalBattery(),new Item.Properties().stacksTo(1));}
    @Override public MenuType<?> getMenuType(){return AWMenus.POWER_TOOLS.get();}
    @Override public ItemMenuHost getMenuHost(Player player,int slot,ItemStack stack,BlockPos pos){return new WirelessPowerHost(player,slot,stack,(p,m)->openFromInventory(p,slot,true));}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        super.inventoryTick(stack,level,entity,slot,selected);
        if(!(entity instanceof ServerPlayer player)||level.getGameTime()%20!=0||!stack.getOrCreateTag().getBoolean("remote_hud"))return;
        var host=new WirelessPowerHost(player,slot,stack,(p,m)->{});
        if(!host.canUsePower(player)||!host.pollHud())return;
        host.refresh();AWNetwork.sendToPlayer(player,PacketPowerHud.monitor("remote",host.settings));
    }
}
