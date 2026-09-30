package com.mpp.aedialsworks.powertools.network;
import com.mpp.aedialsworks.common.network.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
public record PacketPowerHud(CompoundTag data) implements IAWPacket {
    public static PacketPowerHud monitor(String kind,com.mpp.aedialsworks.powertools.monitor.MonitorSettings settings){
        var n=new CompoundTag();n.putString("kind",kind);var list=new net.minecraft.nbt.ListTag();
        for(var e:settings.entries)if(e.key!=null&&list.size()<4){var row=new CompoundTag();String name=e.key.getDisplayName().getString();row.putString("name",name.substring(0,Math.min(256,name.length())));row.putLong("quantity",e.quantity);row.putBoolean("met",e.met);list.add(row);}n.put("rows",list);return new PacketPowerHud(n);
    }
    public PacketPowerHud {data=data.copy();}
    public static PacketPowerHud read(FriendlyByteBuf buf){return new PacketPowerHud(PacketNbt.read(buf));}
    @Override public void write(FriendlyByteBuf buf){PacketNbt.write(buf,data);}
    @Override public void handle(NetworkEvent.Context context){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.mpp.aedialsworks.powertools.client.PowerHud.receive(data));}
}
