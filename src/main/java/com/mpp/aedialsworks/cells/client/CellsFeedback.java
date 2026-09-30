package com.mpp.aedialsworks.cells.client;
import com.mpp.aedialsworks.Aedialsworks;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
/** Transient feedback is drawn after the container, so it stays visible while editing. */
@Mod.EventBusSubscriber(modid=Aedialsworks.MODID,value=Dist.CLIENT)
public final class CellsFeedback {
    private static Component message;private static long expires;
    public static void show(Component text){message=text;expires=System.currentTimeMillis()+2500;}
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event){if(message==null||System.currentTimeMillis()>expires)return;var mc=Minecraft.getInstance();var g=event.getGuiGraphics();int width=mc.font.width(message),x=(event.getScreen().width-width)/2,y=event.getScreen().height-20;g.pose().pushPose();g.pose().translate(0,0,600);g.fill(x-5,y-4,x+width+5,y+13,0xdd20262e);g.drawString(mc.font,message,x,y,0x99ffcc);g.pose().popPose();}
}
