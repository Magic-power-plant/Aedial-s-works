package com.mpp.aedialsworks.cellterminal.client;
import com.mpp.aedialsworks.cellterminal.menu.CellTerminalMenu;
import com.mpp.aedialsworks.cellterminal.network.PacketNBTChunk;
import net.minecraft.client.Minecraft;
public final class TerminalClientPackets {
    private TerminalClientPackets(){}
    public static void receive(PacketNBTChunk packet){var player=Minecraft.getInstance().player;if(player==null)return;if(player.containerMenu instanceof com.mpp.aedialsworks.cells.menu.CellConfigurationMenu cell){cell.acceptChunk(packet);return;}if(player.containerMenu instanceof com.mpp.aedialsworks.cells.menu.CellsMenu cells){cells.acceptChunk(packet);return;}if(player.containerMenu instanceof com.mpp.aedialsworks.powertools.menu.PowerToolsMenu power){power.acceptChunk(packet);return;}if(player.containerMenu instanceof CellTerminalMenu menu)menu.acceptChunk(packet);}
}
