package com.mpp.aedialsworks.powertools.menu;
import java.util.*;
import appeng.menu.*;
import appeng.menu.locator.MenuLocators;
import appeng.api.stacks.*;
import com.mpp.aedialsworks.powertools.*;
import com.mpp.aedialsworks.powertools.crafter.CrafterLogic;
import com.mpp.aedialsworks.common.registry.AWMenus;
import com.mpp.aedialsworks.common.network.*;
import com.mpp.aedialsworks.common.util.AWIds;
import com.mpp.aedialsworks.cellterminal.network.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkHooks;
/** All mutations are menu-addressed and revalidate session, host identity, distance and build permission. */
public final class PowerToolsMenu extends AEBaseMenu implements ServerMenuReceiver {
    public static final String CHANNEL="pt:state";
    public final PowerMenuHost host;
    public final UUID session;
    public CompoundTag data=new CompoundTag();
    private final ChunkAssembler assembler=new ChunkAssembler();
    private final appeng.util.inv.AppEngInternalInventory configSlots=new appeng.util.inv.AppEngInternalInventory(24);
    private final appeng.util.inv.AppEngInternalInventory recipePreview=new appeng.util.inv.AppEngInternalInventory(19);
    private int recipePage,previewPage=-1;
    private ItemStack previewPattern=ItemStack.EMPTY;
    private ListTag catalog;
    private boolean catalogTruncated;
    private CompoundTag last;
    private long revision,lastSent=-100,actionTick=-1;
    private int actions;
    public PowerToolsMenu(int id,Inventory inventory,PowerMenuHost host,UUID session){
        super(AWMenus.POWER_TOOLS.get(),id,inventory,host);this.host=host;this.session=session;PowerToolsSlotSemantics.initialize();
        if(host.configuration() instanceof CrafterLogic crafter){
            for(int i=0;i<12;i++)addSlot(new SlotItemHandler(crafter.patterns,i,8+(i%6)*18,30+(i/6)*18),PowerToolsSlotSemantics.PATTERN[i]);
            for(int i=0;i<4;i++)addSlot(new SlotItemHandler(crafter.upgrades,i,187,8+i*18),SlotSemantics.UPGRADE);
            for(int i=0;i<19;i++)addSlot(new appeng.menu.slot.FakeSlot(recipePreview,i),
                    i<9?SlotSemantics.CRAFTING_GRID:i==9?SlotSemantics.CRAFTING_RESULT:SlotSemantics.MISSING_INGREDIENT);
        }
        if(!(host.configuration() instanceof CrafterLogic)&&!isInspectionTool()){
            for(int i=0;i<24;i++){var slot=new appeng.menu.slot.FakeSlot(configSlots,i);slot.setHideAmount(true);addSlot(slot,PowerToolsSlotSemantics.CONFIG[i]);}
        }
        for(int i=0;i<36;i++){final int slot=i;addSlot(new Slot(inventory,i,8+18*(i%9),i<9?248:190+18*(i/9-1)){
            @Override public boolean mayPickup(Player p){return !isPlayerInventorySlotLocked(slot);}
            @Override public boolean mayPlace(ItemStack s){return !isPlayerInventorySlotLocked(slot);}
        },i<9?SlotSemantics.PLAYER_HOTBAR:SlotSemantics.PLAYER_INVENTORY);}
    }
    public boolean isInspectionTool(){return Set.of("network_health_scanner","network_component_locator","priority_tuner").contains(host.powerKind());}
    /** Display-only AE2 config slots; all edits still use the authorized PowerTools action channel. */
    public void refreshConfigSlots(){
        if(isInspectionTool()||host.configuration() instanceof CrafterLogic)return;
        var entries=data.getList("entries",Tag.TAG_COMPOUND);
        for(int i=0;i<24;i++){
            var entry=entries.getCompound(i);
            var key=entry.contains("key")?AEKey.fromTagGeneric(entry.getCompound("key")):null;
            configSlots.setItemDirect(i,key==null?ItemStack.EMPTY:GenericStack.wrapInItemStack(key,1));
        }
    }
    public List<Slot> getConfigurationSlots(){
        var result=new ArrayList<Slot>();for(var semantic:PowerToolsSlotSemantics.CONFIG)result.addAll(getSlots(semantic));return List.copyOf(result);
    }
    /** Recipe and remainder previews are ghost slots, never an alternative inventory. */
    public void refreshRecipePreview(int page){
        if(!(host.configuration() instanceof CrafterLogic crafter))return;
        recipePage=Math.max(0,Math.min(11,page));
        var encoded=crafter.patterns.getStackInSlot(recipePage);
        if(previewPage==recipePage&&ItemStack.matches(previewPattern,encoded))return;
        previewPage=recipePage;previewPattern=encoded.copy();
        for(int i=0;i<19;i++)recipePreview.setItemDirect(i,ItemStack.EMPTY);
        var details=appeng.api.crafting.PatternDetailsHelper.decodePattern(crafter.patterns.getStackInSlot(recipePage),getPlayer().level());
        if(!(details instanceof appeng.crafting.pattern.AECraftingPattern pattern))return;
        var inputs=pattern.getSparseInputs();
        var frame=new TransientCraftingContainer(new AbstractContainerMenu(null,-1){
            @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
            @Override public boolean stillValid(Player player){return false;}
            @Override public void slotsChanged(net.minecraft.world.Container inventory){}
        },3,3);
        for(int i=0;i<Math.min(9,inputs.length);i++)if(inputs[i]!=null){
            var stack=GenericStack.wrapInItemStack(inputs[i]);recipePreview.setItemDirect(i,stack);
            if(inputs[i].what() instanceof AEItemKey key)frame.setItem(i,key.toStack(1));
        }
        var outputs=pattern.getOutputs();if(outputs.length>0&&outputs[0]!=null)recipePreview.setItemDirect(9,GenericStack.wrapInItemStack(outputs[0]));
        var level=getPlayer().level();
        level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,frame,level).ifPresent(recipe->{
            var remaining=recipe.getRemainingItems(frame);for(int i=0;i<Math.min(9,remaining.size());i++)recipePreview.setItemDirect(10+i,remaining.get(i).copy());
        });
    }
    @Override public boolean stillValid(Player p){return super.stillValid(p)&&host.canUsePower(p);}
    private boolean isConfigSlot(int index){
        if(index<0||index>=slots.size())return false;
        var semantic=getSlotSemantic(slots.get(index));
        return Arrays.asList(PowerToolsSlotSemantics.CONFIG).contains(semantic)||semantic==SlotSemantics.CRAFTING_GRID
                ||semantic==SlotSemantics.CRAFTING_RESULT||semantic==SlotSemantics.MISSING_INGREDIENT;
    }
    @Override public void clicked(int slot,int button,ClickType type,Player p){
        if(!isConfigSlot(slot)&&p==getPlayer()&&stillValid(p)&&p.mayBuild())super.clicked(slot,button,type,p);
    }
    @Override public void setFilter(int slot,ItemStack item){
        // The AE2 ghost-drag protocol is not an alternative authorization path.
        if(!isConfigSlot(slot)&&stillValid(getPlayer())&&getPlayer().mayBuild())super.setFilter(slot,item);
    }
    @Override public void doAction(ServerPlayer player,appeng.helpers.InventoryAction action,int slot,long id){
        if(!isConfigSlot(slot)&&player==getPlayer()&&stillValid(player)&&player.mayBuild())super.doAction(player,action,slot,id);
    }
    @Override public void receiveClientAction(ServerPlayer sender,ResourceLocation action,CompoundTag payload){
        if(sender!=getPlayer()||!stillValid(sender)||!sender.mayBuild()||!action.getNamespace().equals("aedialsworks")||!payload.hasUUID("session")||!session.equals(payload.getUUID("session")))return;
        long tick=sender.level().getGameTime();if(actionTick!=tick){actionTick=tick;actions=0;}if(++actions>8)return;
        if(action.getPath().equals("catalog")&&!isInspectionTool()){
            var keys=new LinkedHashSet<AEKey>();var node=host.getActionableNode();
            if(node!=null){var grid=node.getGrid();
                for(var key:grid.getCraftingService().getCraftables(key->true)){keys.add(key);if(keys.size()>=512)break;}
                for(var key:grid.getStorageService().getCachedInventory().keySet()){if(keys.size()>=512)break;keys.add(key);}
            }
            catalogTruncated=keys.size()>=512;catalog=new ListTag();
            for(var key:keys){var row=new CompoundTag();row.put("key",key.toTagGeneric());catalog.add(row);}last=null;return;
        }
        if(action.getPath().equals("view_recipe")&&host.configuration() instanceof CrafterLogic){
            refreshRecipePreview(payload.getInt("slot"));last=null;return;
        }
        if(host.configuration().configure(sender,action.getPath(),payload))last=null;
    }
    public void action(String action,CompoundTag n){n.putUUID("session",session);AWNetwork.sendToServer(new PacketMenuAction(containerId,AWIds.id(action),n));}
    @Override public void broadcastChanges(){
        if(!(getPlayer() instanceof ServerPlayer player)||!stillValid(player)){super.broadcastChanges();return;}
        long now=player.level().getGameTime();if(last!=null&&now-lastSent<10){super.broadcastChanges();return;}
        var next=host.configuration().snapshot();next.putString("kind",host.powerKind());
        if(catalog!=null){next.put("catalog",catalog.copy());next.putBoolean("catalogTruncated",catalogTruncated);}
        data=next;refreshConfigSlots();refreshRecipePreview(recipePage);super.broadcastChanges();lastSent=now;
        if(next.equals(last))return;
        try{var chunks=ChunkCodec.encode(next,32*1024);long rev=++revision;for(int i=0;i<chunks.size();i++)AWNetwork.sendToPlayer(player,new PacketNBTChunk(containerId,session,CHANNEL,rev,true,i,chunks.size(),chunks.get(i)));last=next;lastSent=now;}
        catch(java.io.IOException ex){com.mpp.aedialsworks.Aedialsworks.LOGGER.error("Cannot sync PowerTools menu",ex);player.closeContainer();}
    }
    public void acceptChunk(PacketNBTChunk packet){
        if(packet.containerId()!=containerId||!session.equals(packet.menuSession())||!CHANNEL.equals(packet.channel()))return;
        try{assembler.accept(packet,System.currentTimeMillis()).ifPresent(p->{data=p.tag();refreshConfigSlots();});}
        catch(java.io.IOException ex){assembler.close();}
    }
    @Override public void removed(Player player){super.removed(player);assembler.close();}
    public static PowerToolsMenu fromNetwork(int id,Inventory inv,FriendlyByteBuf buf){
        var locator=MenuLocators.readFromPacket(buf);var host=locator.locate(inv.player,PowerMenuHost.class);
        if(host==null)throw new IllegalStateException("PowerTools host missing");var menu=new PowerToolsMenu(id,inv,host,buf.readUUID());menu.setLocator(locator);return menu;
    }
    public static void registerOpener(){MenuOpener.addOpener(AWMenus.POWER_TOOLS.get(),(player,locator,returning)->{
        if(!(player instanceof ServerPlayer server))return false;var host=locator.locate(player,PowerMenuHost.class);if(host==null||!host.canUsePower(player))return false;
        UUID nonce=UUID.randomUUID();NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inv,p)->{var menu=new PowerToolsMenu(id,inv,host,nonce);menu.setLocator(locator);return menu;},Component.translatable("item.aedialsworks."+host.powerKind())),buf->{MenuLocators.writeToPacket(buf,locator);buf.writeUUID(nonce);});return true;
    });}
}
