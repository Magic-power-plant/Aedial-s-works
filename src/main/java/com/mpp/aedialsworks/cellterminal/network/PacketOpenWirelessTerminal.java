package com.mpp.aedialsworks.cellterminal.network;
import com.mpp.aedialsworks.common.network.IAWPacket;
import com.mpp.aedialsworks.cellterminal.items.ItemWirelessCellTerminal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
public record PacketOpenWirelessTerminal() implements IAWPacket {
    public static PacketOpenWirelessTerminal read(FriendlyByteBuf buffer){return new PacketOpenWirelessTerminal();}
    @Override public void write(FriendlyByteBuf buffer){}
    @Override public void handle(NetworkEvent.Context context){
        var player=context.getSender();if(player==null || player.isSpectator() || player.containerMenu!=player.inventoryMenu)return;
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).getItem() instanceof ItemWirelessCellTerminal item){item.openFromInventory(player,i);return;}
    }
}
