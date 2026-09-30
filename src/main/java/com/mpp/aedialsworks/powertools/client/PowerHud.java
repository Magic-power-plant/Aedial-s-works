package com.mpp.aedialsworks.powertools.client;
import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.cellterminal.client.BlockHighlightRenderer;
import com.mpp.aedialsworks.common.config.AWConfigs;
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
    private static int color(String hex,int fallback){try{int value=(int)Long.parseLong(hex.trim(),16);return value<=0xFFFFFF?value|0xFF000000:value;}catch(NumberFormatException ignored){return fallback;}}
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){var mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui||mc.screen!=null)return;long now=System.currentTimeMillis();
        var cfg=AWConfigs.CLIENT.powertools.remoteMonitor;int gain=color(cfg.gainColor.get(),0xFF66FF66),loss=color(cfg.lossColor.get(),0xFFFF6666),pad=cfg.paddingInternal.get();
        for(var n:new CompoundTag[]{now<alarmExpires?alarm:null,now<remoteExpires?remote:null})if(n!=null){var g=event.getGuiGraphics();var rows=n.getList("rows",Tag.TAG_COMPOUND);
            boolean remoteHud=n==remote;int x=remoteHud?cfg.x.get():mc.getWindow().getGuiScaledWidth()-204,y=remoteHud?cfg.y.get():8;
            g.fill(x-pad,y-pad,x+200+pad,y+17+rows.size()*12+pad,0xB0222222);g.drawString(mc.font,Component.translatable("gui.aedialsworks.powertools."+n.getString("kind")),x,y,n==alarm?0xFF6666:0x99FFCC);y+=14;
            for(var raw:rows){var row=(CompoundTag)raw;g.drawString(mc.font,mc.font.plainSubstrByWidth(row.getString("name")+": "+row.getLong("quantity"),195),x,y,row.getBoolean("met")?loss:gain);y+=12;}y+=10;
        }
    }
}
