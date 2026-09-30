package com.mpp.aedialsworks.cellterminal.client;
import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.common.registry.AWMenus;
import com.mpp.aedialsworks.cellterminal.integration.ae2.CellTerminalBootstrap;
import com.mpp.aedialsworks.cellterminal.screen.CellTerminalScreen;
import com.mpp.aedialsworks.cellterminal.network.PacketOpenWirelessTerminal;
import com.mpp.aedialsworks.common.network.AWNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
@Mod.EventBusSubscriber(modid=Aedialsworks.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class CellTerminalClient {
    public static final KeyMapping OPEN=new KeyMapping("key.aedialsworks.cellterminal.open",KeyConflictContext.IN_GAME,KeyModifier.SHIFT,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_P,"key.categories.aedialsworks");
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){
        CellTerminalBootstrap.registerModels();event.enqueueWork(()->MenuScreens.register(AWMenus.CELL_TERMINAL.get(),CellTerminalScreen::new));
    }
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event){event.register(OPEN);}
    @Mod.EventBusSubscriber(modid=Aedialsworks.MODID,value=Dist.CLIENT)
    public static final class Ticks {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
            if(event.phase!=TickEvent.Phase.END)return;
            while(OPEN.consumeClick())if(Minecraft.getInstance().screen==null)AWNetwork.sendToServer(new PacketOpenWirelessTerminal());
        }
    }
}
