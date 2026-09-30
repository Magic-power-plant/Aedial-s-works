package com.mpp.aedialsworks.cells.network;
import com.mpp.aedialsworks.common.network.IAWPacket;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
/** Only position and user intent cross the wire; all settings and held items come from the server. */
public record PacketCellsMemoryCard(BlockPos pos,Direction side,InteractionHand hand,boolean includeFilters) implements IAWPacket {
    public static PacketCellsMemoryCard read(FriendlyByteBuf b){var pos=b.readBlockPos();Direction side=b.readBoolean()?b.readEnum(Direction.class):null;return new PacketCellsMemoryCard(pos,side,b.readEnum(InteractionHand.class),b.readBoolean());}
    public void write(FriendlyByteBuf b){b.writeBlockPos(pos);b.writeBoolean(side!=null);if(side!=null)b.writeEnum(side);b.writeEnum(hand);b.writeBoolean(includeFilters);}
    public void handle(NetworkEvent.Context context){var player=context.getSender();if(player!=null)CellsMemoryCards.interact(player,pos,side,hand,includeFilters);}
}
