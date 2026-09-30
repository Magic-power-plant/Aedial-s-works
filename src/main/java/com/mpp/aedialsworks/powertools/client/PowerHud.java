package com.mpp.aedialsworks.powertools.client;
import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.cellterminal.client.BlockHighlightRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=Aedialsworks.MODID,value=Dist.CLIENT)
public final class PowerHud {
    private static CompoundTag alarm,remote;
    private static long alarmExpires,remoteExpires;
    public static void receive(CompoundTag n){long now=System.currentTimeMillis();switch(n.getString("kind")){
        case "locate"->BlockHighlightRenderer.receive(n);
        case "alarm"->{alarm=n.copy();alarmExpires=now+7000;BlockHighlightRenderer.receive(n);}
        case "remote"->{remote=n.copy();remoteExpires=now+2500;}
    }}
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){var mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui||mc.screen!=null)return;long now=System.currentTimeMillis();int y=8;
        for(var n:new CompoundTag[]{now<alarmExpires?alarm:null,now<remoteExpires?remote:null})if(n!=null){var g=event.getGuiGraphics();int x=mc.getWindow().getGuiScaledWidth()-204;var rows=n.getList("rows",Tag.TAG_COMPOUND);g.fill(x-4,y-3,x+200,y+17+rows.size()*12,0xB0222222);g.drawString(mc.font,Component.translatable("gui.aedialsworks.powertools."+n.getString("kind")),x,y,n==alarm?0xFF6666:0x99FFCC);y+=14;
            for(var raw:rows){var row=(CompoundTag)raw;g.drawString(mc.font,mc.font.plainSubstrByWidth(row.getString("name")+": "+row.getLong("quantity"),195),x,y,row.getBoolean("met")?0xFF8888:0xFFFFFF);y+=12;}y+=10;
        }
    }
}
