package com.mpp.aedialsworks.cells.client;
import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.cells.cell.*;
import com.mpp.aedialsworks.cells.integration.ae2.CellsBootstrap;
import com.mpp.aedialsworks.common.registry.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.mojang.blaze3d.platform.InputConstants;
@Mod.EventBusSubscriber(modid=Aedialsworks.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class CellsClient {
    public static final KeyMapping QUICK_ADD=new KeyMapping("key.aedialsworks.cells.quick_add",KeyConflictContext.GUI,InputConstants.Type.KEYSYM,-1,"key.categories.aedialsworks");
    public static final KeyMapping MEMORY_FILTERS=new KeyMapping("key.aedialsworks.cells.memory_filters",KeyConflictContext.UNIVERSAL,InputConstants.Type.KEYSYM,org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL,"key.categories.aedialsworks");
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){CellsBootstrap.models();e.enqueueWork(()->{MenuScreens.<com.mpp.aedialsworks.cells.menu.CellsMenu,CellsScreen>register(AWMenus.CELLS.get(),(m,i,t)->{try{return new CellsScreen(m,i,t);}catch(Throwable ex){Aedialsworks.LOGGER.error("CELLS screen construction failed",ex);throw ex;}});MenuScreens.<com.mpp.aedialsworks.cells.menu.CellConfigurationMenu,CellConfigurationScreen>register(AWMenus.CELL_CONFIGURATION.get(),(m,i,t)->{try{return new CellConfigurationScreen(m,i,t);}catch(Throwable ex){Aedialsworks.LOGGER.error("Cell screen construction failed",ex);throw ex;}});});}
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(QUICK_ADD);e.register(MEMORY_FILTERS);}
    private static CellTextureColors.CellType type(CellFamily f){return switch(f){case COMPACTING->CellTextureColors.CellType.COMPACTING;case HD_COMPACTING->CellTextureColors.CellType.HYPER_DENSITY_COMPACTING;case HD_FLUID->CellTextureColors.CellType.HYPER_DENSITY_FLUID;default->CellTextureColors.CellType.HYPER_DENSITY_ITEM;};}
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item e){
        for(var entry:AWCells.CELLS.values())e.register((s,t)->{var c=(TieredCellItem)s.getItem();return c.family.creative()||c.family==CellFamily.CONFIGURABLE&&c.component(s).isEmpty()?-1:CellTextureColors.getColorForLayer((c.storageFamily(s)==CellFamily.CONFIGURABLE?(c.keyType(s)==appeng.api.stacks.AEKeyType.fluids()?CellTextureColors.CellType.HYPER_DENSITY_FLUID:CellTextureColors.CellType.COMPACTING):type(c.storageFamily(s))),c.storageTier(s).ordinal(),t);},entry.get());
        for(var entry:AWCells.COMPONENTS.values())e.register((s,t)->t==0?CellTextureColors.getComponentWaveColor(type(((CellComponentItem)s.getItem()).family)):-1,entry.get());
    }
}
