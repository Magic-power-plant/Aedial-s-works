package com.mpp.aedialsworks.powertools.items;
import appeng.api.implementations.menuobjects.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPartHost;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.blockentity.crafting.MolecularAssemblerBlockEntity;
import appeng.core.definitions.AEItems;
import com.mpp.aedialsworks.common.registry.AWMenus;
import com.mpp.aedialsworks.powertools.integration.ae2.*;
import com.mpp.aedialsworks.powertools.monitor.MonitorLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
public final class NetworkToolItem extends Item implements IMenuItem {
    public enum Kind{SCANNER,LOCATOR,TUNER,DISTRIBUTOR,ALARM_LOCATOR}
    public final Kind kind;
    public NetworkToolItem(Kind kind){super(new Properties().stacksTo(1));this.kind=kind;}
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        var level=context.getLevel();Object target=level.getBlockEntity(context.getClickedPos());
        int face=-1;if(target instanceof IPartHost parts){var selected=parts.selectPartWorld(context.getClickLocation());if(selected.part!=null){target=selected.part;face=selected.side==null?-1:selected.side.ordinal();}else target=parts.getPart(null);}
        if(!(target instanceof IActionHost host)||host.getActionableNode()==null)return InteractionResult.PASS;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        if(kind==Kind.ALARM_LOCATOR){
            if(target instanceof PowerBlockEntity be&&be.logic() instanceof MonitorLogic monitor&&be.kind==com.mpp.aedialsworks.powertools.MachineKind.ALARM&&player.mayBuild()){
                boolean on=monitor.toggleBinding(player.getUUID());player.displayClientMessage(Component.translatable("gui.aedialsworks.powertools."+(on?"bound":"unbound")),true);
                var n=context.getItemInHand().getOrCreateTag();n.putLong("target",context.getClickedPos().asLong());n.putString("dimension",level.dimension().location().toString());return InteractionResult.CONSUME;
            }return InteractionResult.PASS;
        }
        if(kind==Kind.DISTRIBUTOR){if(!player.mayBuild()||!host.getActionableNode().isActive())return InteractionResult.FAIL;int moved=distribute(player,host);player.displayClientMessage(Component.translatable("gui.aedialsworks.powertools.distributed",moved),true);return InteractionResult.CONSUME;}
        var tag=context.getItemInHand().getOrCreateTag();tag.putLong("target",context.getClickedPos().asLong());tag.putInt("face",face);tag.putString("dimension",level.dimension().location().toString());
        player.inventoryMenu.broadcastChanges();
        MenuOpener.open(AWMenus.POWER_TOOLS.get(),player,MenuLocators.forItemUseContext(context));return InteractionResult.CONSUME;
    }
    public static int distribute(Player player,IActionHost host){
        int moved=0;for(var node:host.getActionableNode().getGrid().getNodes())if(node.getOwner() instanceof MolecularAssemblerBlockEntity assembler){
            var upgrades=assembler.getUpgrades();
            for(int s=0;s<player.getInventory().items.size();s++){var held=player.getInventory().getItem(s);if(!AEItems.SPEED_CARD.isSameAs(held))continue;
                var left=upgrades.addItems(held.copy());int n=held.getCount()-left.getCount();if(n>0){held.shrink(n);moved+=n;assembler.saveChanges();}
            }
        }player.getInventory().setChanged();return moved;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(!level.isClientSide()&&player.getItemInHand(hand).getOrCreateTag().contains("target")){
            if(kind==Kind.ALARM_LOCATOR){var n=player.getItemInHand(hand).getOrCreateTag();var out=new net.minecraft.nbt.CompoundTag();out.putString("kind","locate");out.putLong("pos",n.getLong("target"));out.putString("dimension",n.getString("dimension"));com.mpp.aedialsworks.common.network.AWNetwork.sendToPlayer((net.minecraft.server.level.ServerPlayer)player,new com.mpp.aedialsworks.powertools.network.PacketPowerHud(out));}
            else if(kind!=Kind.DISTRIBUTOR)MenuOpener.open(AWMenus.POWER_TOOLS.get(),player,MenuLocators.forHand(player,hand));
        }return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide());
    }
    @Override public ItemMenuHost getMenuHost(Player player,int slot,ItemStack stack,BlockPos pos){return new NetworkToolHost(player,slot,stack,kind);}
}
