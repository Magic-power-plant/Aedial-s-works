package com.mpp.aedialsworks.cells.network;
import com.mpp.aedialsworks.common.network.IAWPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
public record PacketCellsFeedback(Component message) implements IAWPacket {
    public static PacketCellsFeedback read(FriendlyByteBuf b){return new PacketCellsFeedback(b.readComponent());}
    public void write(FriendlyByteBuf b){b.writeComponent(message);}
    public void handle(NetworkEvent.Context c){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.mpp.aedialsworks.cells.client.CellsFeedback.show(message));}
}
