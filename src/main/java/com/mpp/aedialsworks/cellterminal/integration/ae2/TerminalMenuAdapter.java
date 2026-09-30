package com.mpp.aedialsworks.cellterminal.integration.ae2;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.mpp.aedialsworks.cellterminal.menu.*;
import com.mpp.aedialsworks.common.registry.AWMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.NetworkHooks;
import java.util.UUID;
public abstract class TerminalMenuAdapter extends AEBaseMenu {
    protected TerminalMenuAdapter(int id, Inventory inventory, TerminalHost host) {
        super(AWMenus.CELL_TERMINAL.get(),id,inventory,host);
        for(int i=0;i<inventory.items.size();i++) {
            final int index=i;
            addSlot(new net.minecraft.world.inventory.Slot(inventory,i,8+18*(i%9),i<9?286:228+18*(i/9-1)) {
                @Override public boolean mayPickup(net.minecraft.world.entity.player.Player player) {return !isPlayerInventorySlotLocked(index);}
                @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {return !isPlayerInventorySlotLocked(index);}
            },i<9?appeng.menu.SlotSemantics.PLAYER_HOTBAR:appeng.menu.SlotSemantics.PLAYER_INVENTORY);
        }
    }
    public static CellTerminalMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        var locator=MenuLocators.readFromPacket(buf);
        var host=locator.locate(inv.player,TerminalHost.class);
        if(host==null) throw new IllegalStateException("Cell terminal host not found");
        var menu=new CellTerminalMenu(id,inv,host,buf.readUUID()); menu.setLocator(locator); return menu;
    }
    public static void registerOpener() {
        MenuOpener.addOpener(AWMenus.CELL_TERMINAL.get(),(player,locator,returning)->{
            if(!(player instanceof ServerPlayer server)) return false;
            var host=locator.locate(player,TerminalHost.class);
            if(host==null || !host.canUseTerminal(player)) return false;
            var nonce=UUID.randomUUID();
            NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inv,p)->{
                var menu=new CellTerminalMenu(id,inv,host,nonce); menu.setLocator(locator); return menu;
            },Component.translatable("gui.aedialsworks.cellterminal.title")),buf->{MenuLocators.writeToPacket(buf,locator);buf.writeUUID(nonce);});
            return true;
        });
    }
}
