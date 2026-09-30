package com.mpp.aedialsworks.powertools.client;
import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.common.registry.*;
import com.mpp.aedialsworks.powertools.integration.ae2.PowerToolsBootstrap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
@Mod.EventBusSubscriber(modid=Aedialsworks.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class PowerToolsClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){PowerToolsBootstrap.models();event.enqueueWork(()->MenuScreens.register(AWMenus.POWER_TOOLS.get(),PowerToolsScreen::new));}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(AWBlockEntities.STORAGE_DISPLAY.get(),PowerDisplayRenderer::new);}
}
