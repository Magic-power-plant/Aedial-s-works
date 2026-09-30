package com.mpp.aedialsworks.powertools.integration.ae2;
import java.util.function.BiConsumer;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.ISubMenu;
import com.mpp.aedialsworks.powertools.*;
import com.mpp.aedialsworks.powertools.items.RemoteMonitorItem;
import com.mpp.aedialsworks.powertools.monitor.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
public final class WirelessPowerHost extends WirelessTerminalMenuHost implements PowerMenuHost,PowerConfigurable {
    public final MonitorSettings settings=new MonitorSettings();
    public WirelessPowerHost(Player player,Integer slot,ItemStack stack,BiConsumer<Player,ISubMenu> back){super(player,slot,stack,back);settings.load(stack.getOrCreateTag().getCompound("monitor"));}
    @Override public PowerConfigurable configuration(){return this;}
    @Override public String powerKind(){return "remote_storage_monitor";}
    @Override public boolean canUsePower(Player p){return p==getPlayer()&&getSlot()!=null&&p.getInventory().getItem(getSlot())==getItemStack()&&(isClientSide()||rangeCheck()&&((RemoteMonitorItem)getItemStack().getItem()).hasPower(p,0.5,getItemStack()));}
    public void refresh(){var node=getActionableNode();settings.evaluate(key->node.getGrid().getStorageService().getCachedInventory().get(key),node!=null&&node.isActive());}
    public boolean pollHud(){var node=getActionableNode();if(node==null)return false;var pos=com.mpp.aedialsworks.powertools.scanner.NetworkScanner.position(node);if(pos==null)return false;double distance=Math.sqrt(getPlayer().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)));return ((RemoteMonitorItem)getItemStack().getItem()).usePower(getPlayer(),20*appeng.core.AEConfig.instance().wireless_getDrainRate(distance),getItemStack());}
    private void save(){var n=new CompoundTag();settings.save(n);getItemStack().getOrCreateTag().put("monitor",n);getPlayer().getInventory().setChanged();}
    @Override public CompoundTag snapshot(){if(!isClientSide())refresh();var n=new CompoundTag();settings.save(n);n.putBoolean("hud",getItemStack().getOrCreateTag().getBoolean("remote_hud"));return n;}
    @Override public boolean configure(ServerPlayer p,String action,CompoundTag n){
        if(action.equals("entry")){int i=n.getInt("slot");if(i<0||i>=24)return false;settings.entries[i]=ResourceEntry.load(n);save();return true;}
        if(action.equals("settings")){getItemStack().getOrCreateTag().putBoolean("remote_hud",n.getBoolean("hud"));settings.any=n.getBoolean("any");settings.hysteresis=n.getBoolean("hysteresis");settings.strength=Math.max(1,Math.min(15,n.getInt("strength")));settings.refreshTicks=Math.max(20,Math.min(1200,n.getInt("refresh")));save();return true;}return false;
    }
}
