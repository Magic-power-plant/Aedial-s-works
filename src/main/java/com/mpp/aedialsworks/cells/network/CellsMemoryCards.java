package com.mpp.aedialsworks.cells.network;
import appeng.api.implementations.items.*;
import appeng.api.parts.*;
import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.cells.api.CellsHost;
import com.mpp.aedialsworks.common.network.AWNetwork;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@Mod.EventBusSubscriber(modid=Aedialsworks.MODID)
public final class CellsMemoryCards {
    private CellsMemoryCards(){}
    @SubscribeEvent public static void rightClick(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getItemStack().getItem() instanceof IMemoryCard))return;
        var be=event.getLevel().getBlockEntity(event.getPos());CellsHost target=be instanceof CellsHost host?host:null;Direction side=null;
        if(be instanceof IPartHost host){var selected=host.selectPartWorld(event.getHitVec().getLocation());if(selected.part instanceof CellsHost cells){target=cells;side=selected.side;}}
        if(target==null)return;event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        if(event.getLevel().isClientSide){var packetSide=side;DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->AWNetwork.sendToServer(new PacketCellsMemoryCard(event.getPos(),packetSide,event.getHand(),com.mpp.aedialsworks.cells.client.CellsClient.MEMORY_FILTERS.isDown())));}
    }
    public static boolean interact(ServerPlayer player,BlockPos pos,Direction side,InteractionHand hand,boolean includeFilters){
        var level=player.serverLevel();if(!level.hasChunkAt(pos)||!player.mayBuild()||!level.mayInteract(player,pos))return false;
        var be=level.getBlockEntity(pos);CellsHost host=side==null?(be instanceof CellsHost cells?cells:null):(PartHelper.getPart(level,pos,side) instanceof CellsHost cells?cells:null);
        var stack=player.getItemInHand(hand);if(host==null||!host.canUseCells(player)||!(stack.getItem() instanceof IMemoryCard card))return false;
        var logic=host.cellsLogic();String kind="item.aedialsworks."+host.cellsKind().id;String message;
        if(player.isShiftKeyDown()){card.setMemoryCardContents(stack,kind,logic.memorySettings(includeFilters));message=includeFilters?"saved_filters":"saved";}
        else {if(!kind.equals(card.getSettingsName(stack))){feedback(player,"incompatible");return false;}logic.loadSettings(card.getData(stack));message="loaded";}
        player.getInventory().setChanged();feedback(player,message);return true;
    }
    private static void feedback(ServerPlayer p,String key){var text=Component.translatable("gui.aedialsworks.cells.memory_"+key);p.displayClientMessage(text,true);AWNetwork.sendToPlayer(p,new PacketCellsFeedback(text));}
}
