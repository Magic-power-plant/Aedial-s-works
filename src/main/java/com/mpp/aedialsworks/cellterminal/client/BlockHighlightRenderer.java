package com.mpp.aedialsworks.cellterminal.client;
import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.common.config.AWConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=Aedialsworks.MODID,value=Dist.CLIENT)
public final class BlockHighlightRenderer {
    private static BlockPos target;private static String dimension="";private static long expires;
    public static void receive(CompoundTag data){target=BlockPos.of(data.getLong("pos"));dimension=data.getString("dimension");expires=System.currentTimeMillis()+1000L*AWConfigs.CLIENT.cellterminal.settings.highlightDuration.get();}
    private static boolean visible(){var mc=Minecraft.getInstance();if(target==null || mc.level==null || mc.player==null || System.currentTimeMillis()>expires || !dimension.equals(mc.level.dimension().location().toString()))return false;
        int limit=AWConfigs.CLIENT.cellterminal.settings.maxHighlightDistance.get();return limit<0 || mc.player.distanceToSqr(Vec3.atCenterOf(target))<=(double)limit*limit;}
    @SubscribeEvent public static void world(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !visible())return;
        var mc=Minecraft.getInstance();var camera=event.getCamera().getPosition();var pose=event.getPoseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        var buffers=mc.renderBuffers().bufferSource();LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),new AABB(target).inflate(0.01),0.15f,1f,0.7f,1f);buffers.endBatch(RenderType.lines());pose.popPose();
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
        if(!visible())return;var mc=Minecraft.getInstance();var player=mc.player;var delta=Vec3.atCenterOf(target).subtract(player.position());
        double angle=Math.toRadians(player.getYRot())-Math.atan2(-delta.x,delta.z);String arrow=Math.sin(angle)>0.25?"▶":Math.sin(angle)<-0.25?"◀":Math.cos(angle)<0?"▼":"▲";
        var cfg=AWConfigs.CLIENT.cellterminal.settings;var g=event.getGuiGraphics();int cx=mc.getWindow().getGuiScaledWidth()/2;
        g.pose().pushPose();float scale=cfg.arrowScalePercent.get()/100f;g.pose().translate(cx,36,0);g.pose().scale(scale,scale,1);g.drawCenteredString(mc.font,arrow,0,0,0x55FFBB);g.pose().popPose();
        float textScale=cfg.textScalePercent.get()/100f;
        if(cfg.adaptiveTextScale.get())textScale*=Math.max(cfg.adaptiveTextScaleMinPercent.get()/100f,Math.min(cfg.adaptiveTextScaleMaxPercent.get()/100f,(float)(1+delta.length()/128)));
        g.pose().pushPose();g.pose().translate(cx,52,0);g.pose().scale(textScale,textScale,1);g.drawCenteredString(mc.font,target.toShortString()+" · "+(int)delta.length()+"m",0,0,0x55FFBB);g.pose().popPose();
    }
}
