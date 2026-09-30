package com.mpp.aedialsworks.common.network.client;

import com.mpp.aedialsworks.common.network.ClientMenuReceiver;
import com.mpp.aedialsworks.common.network.PacketMenuData;
import net.minecraft.client.Minecraft;

/** Physical-client entry point. No static registration from common code. */
public final class ClientPackets {
    private ClientPackets() {}

    public static void receive(PacketMenuData packet) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var menu = player.containerMenu;
        if (menu.containerId == packet.containerId() && menu instanceof ClientMenuReceiver receiver) {
            receiver.receiveServerData(packet.channel(), packet.payload().copy());
            if(menu instanceof com.mpp.aedialsworks.cellterminal.menu.CellTerminalMenu terminal
                    && packet.channel().equals(com.mpp.aedialsworks.common.util.AWIds.id("cellterminal/highlight"))
                    && packet.payload().hasUUID("session") && packet.payload().getUUID("session").equals(terminal.session())) {
                com.mpp.aedialsworks.cellterminal.client.BlockHighlightRenderer.receive(packet.payload());
            }
        }
    }
}
