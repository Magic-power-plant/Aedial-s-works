package com.mpp.aedialsworks.powertools.client;
import com.mojang.blaze3d.vertex.PoseStack;
import appeng.api.client.AEKeyRendering;
import appeng.api.orientation.BlockOrientation;
import appeng.client.render.BlockEntityRenderHelper;
import com.mpp.aedialsworks.powertools.MachineKind;
import com.mpp.aedialsworks.powertools.integration.ae2.*;
import com.mpp.aedialsworks.powertools.monitor.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.level.Level;
public final class PowerDisplayRenderer implements BlockEntityRenderer<PowerBlockEntity> {
    public PowerDisplayRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(PowerBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(be.kind!=MachineKind.DISPLAY||!(be.logic() instanceof MonitorLogic monitor))return;
        pose.pushPose();pose.translate(.5,.5,.5);BlockEntityRenderHelper.rotateToFace(pose,be.getOrientation());pose.translate(0,0,.501);draw(monitor.settings,pose,buffers,be.getLevel(),light,.9f);pose.popPose();
    }
    public static void renderPart(MonitorPart part,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.5,.5);BlockEntityRenderHelper.rotateToFace(pose,BlockOrientation.get(part.getSide(),0));pose.translate(0,0,.501);draw(part.logic.settings,pose,buffers,part.getLevel(),light,.72f-part.size*.12f);pose.popPose();
    }
    private static void draw(MonitorSettings settings,PoseStack pose,MultiBufferSource buffers,Level level,int light,float size){
        var list=java.util.Arrays.stream(settings.entries).filter(e->e.key!=null).toList();if(list.isEmpty())return;
        int cols=list.size()==1?1:list.size()<=4?2:list.size()<=9?3:6;int rows=(list.size()+cols-1)/cols;float cell=size/Math.max(cols,rows);
        for(int i=0;i<list.size();i++){var e=list.get(i);pose.pushPose();pose.translate((i%cols-(cols-1)/2f)*cell,((rows-1)/2f-i/cols)*cell,0);
            AEKeyRendering.drawOnBlockFace(pose,buffers,e.key,cell*.7f,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,level);
            pose.translate(0,-cell*.36f,.002);String text=Long.toString(e.quantity);var font=Minecraft.getInstance().font;float textScale=cell/Math.max(60,font.width(text)/.92f);pose.scale(textScale,-textScale,textScale);
            font.drawInBatch(text,-font.width(text)/2f,0,e.met?0xFF6666:0x66FF99,false,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0,15728880);pose.popPose();
        }
    }
}
