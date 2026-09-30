package com.mpp.aedialsworks.cellterminal.integration.ae2;
import java.util.List;
import appeng.api.parts.*;
import appeng.api.networking.IGrid;
import appeng.api.inventories.InternalInventory;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractDisplayPart;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.mpp.aedialsworks.common.registry.AWMenus;
import com.mpp.aedialsworks.common.util.AWIds;
import com.mpp.aedialsworks.cellterminal.menu.TerminalHost;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
/** AE2 15.4.11 adapter: AEBasePart owns managed-node creation/destruction and power state. */
public abstract class TerminalDisplayPartAdapter extends AbstractDisplayPart implements TerminalHost {
    public static final ResourceLocation OFF=AWIds.id("part/cellterminal/cell_terminal_off"),
        ON=AWIds.id("part/cellterminal/cell_terminal_on"), DIM=AWIds.id("part/cellterminal/cell_terminal_on_dim");
    private static final IPartModel OFF_MODEL=new PartModel(MODEL_BASE,OFF,MODEL_STATUS_OFF),
        DIM_MODEL=new PartModel(MODEL_BASE,DIM,MODEL_STATUS_ON), ON_MODEL=new PartModel(MODEL_BASE,ON,MODEL_STATUS_HAS_CHANNEL);
    private final TempCellInventory temp=new TempCellInventory(this::saveTerminal);
    protected TerminalDisplayPartAdapter(IPartItem<?> item) { super(item,true); }
    @Override public IPartModel getStaticModels() { return selectModel(OFF_MODEL,DIM_MODEL,ON_MODEL); }
    @Override public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if(super.onPartActivate(player,hand,pos)) return true;
        if(!isClientSide() && canUseTerminal(player)) MenuOpener.open(AWMenus.CELL_TERMINAL.get(),player,MenuLocators.forPart(this));
        return true;
    }
    @Override public IGrid terminalGrid() { var node=getGridNode(); return node==null?null:node.getGrid(); }
    @Override public InternalInventory temporaryCells() { return temp; }
    @Override public void saveTerminal() { if(getHost()!=null) getHost().markForSave(); }
    @Override public boolean canUseTerminal(Player player) {
        return isClientSide() || (getHost()!=null && getHost().isInWorld() && getHost().getPart(getSide())==this
            && isActive() && player.level()==getLevel() && player.distanceToSqr(Vec3.atCenterOf(getBlockEntity().getBlockPos()))<=64);
    }
    @Override public void readFromNBT(CompoundTag tag) { super.readFromNBT(tag); temp.read(tag); }
    @Override public void writeToNBT(CompoundTag tag) { super.writeToNBT(tag); temp.write(tag); }
    @Override public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) { for(var stack:temp) if(!stack.isEmpty()) drops.add(stack.copy()); }
    @Override public void clearContent() { temp.clear(); }
}
