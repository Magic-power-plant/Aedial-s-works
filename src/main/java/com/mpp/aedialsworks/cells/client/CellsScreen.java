package com.mpp.aedialsworks.cells.client;
import java.util.*;
import appeng.api.stacks.*;
import appeng.client.gui.*;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.*;
import com.mpp.aedialsworks.cells.menu.*;
import com.mpp.aedialsworks.common.config.AWConfigs;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;

/** Original CELLS slot grids and AE2 controls; all mutations are validated by the server menu. */
public final class CellsScreen extends AEBaseScreen<CellsMenu> {
    private final int settingsPage;private final CellsScreen parent;private int selected;
    private final List<ConfirmableTextField> fields=new ArrayList<>();private boolean loaded;
    public CellsScreen(CellsMenu menu,Inventory inv,Component title){this(menu,inv,title,-1,null,0);}
    private CellsScreen(CellsMenu menu,Inventory inv,Component title,int settingsPage,CellsScreen parent,int selected){
        super(menu,inv,title,StyleManager.loadStyleDoc("/screens/aedialsworks/cells/"+(settingsPage<0?"machine":"settings")+".json"));this.settingsPage=settingsPage;this.parent=parent;this.selected=selected;
        setTextContent("dialog_title",fit(title,155));
        if(settingsPage<0){
            icon("previous",Icon.ARROW_LEFT,()->view(menu.port,menu.page-1));icon("next",Icon.ARROW_RIGHT,()->view(menu.port,menu.page+1));
            icon("settings",Icon.SCHEDULING_DEFAULT,()->open(0));icon("port",Icon.VIEW_MODE_ALL,()->view((menu.port+1)%Math.max(1,menu.data.getInt("portCount")),0));
            icon("inverse",Icon.BLOCKING_MODE_YES,()->toggle("inverse"));icon("items",Icon.VIEW_MODE_STORED,()->toggle("items"));icon("fluids",Icon.POWER_UNIT_AE,()->toggle("fluids"));icon("fuzzy",Icon.SEARCH_DEFAULT,()->menu.action("fuzzy",new CompoundTag()));
        }else {
            icon("back",Icon.ARROW_LEFT,this::back);icon("save",Icon.ENTER,()->{if(save())back();});icon("next",Icon.ARROW_RIGHT,()->{if(save())open((settingsPage+1)%3);});
            String[] names=settingsPage==0?new String[]{"polling","priority","tag"}:settingsPage==1?new String[]{"maxSlot","transferQuantity","keepQuantity"}:new String[]{"transferInterval","limit","slot"};
            for(int i=0;i<3;i++){setTextContent("label"+i,tr(names[i]));var f=new ConfirmableTextField(style,font,0,0,0,0);f.setBordered(false);f.setMaxLength(settingsPage==0&&i==2?128:20);f.setOnConfirm(()->{if(save())back();});widgets.add("field"+i,f);fields.add(f);}
        }
    }
    private Component tr(String s){return Component.translatable("gui.aedialsworks.cells."+s);}
    private Component fit(Component c,int width){return Component.literal(net.minecraft.client.Minecraft.getInstance().font.plainSubstrByWidth(c.getString(),width));}
    private void icon(String id,Icon image,Runnable action){var b=new IconButton(ignored->action.run()){@Override protected Icon getIcon(){return image;}};b.setMessage(tr(id));widgets.add(id,b);}
    private void open(int page){switchToScreen(new CellsScreen(menu,menu.getPlayerInventory(),title,page,settingsPage<0?this:parent,selected));}
    private void back(){if(parent==null)super.onClose();else switchToScreen(parent);}
    @Override public void onClose(){back();}
    private CompoundTag settings(){var n=new CompoundTag();for(String key:List.of("polling","priority","items","fluids","inverse","tag","maxSlot","transferQuantity","keepQuantity","transferInterval"))if(menu.data.contains(key))n.put(key,menu.data.get(key).copy());return n;}
    private void view(int port,int page){int pages=Math.max(1,menu.data.getInt("pages"));var n=new CompoundTag();n.putInt("port",port);n.putInt("page",Math.floorMod(page,pages));menu.action("view",n);}
    private void toggle(String key){var n=settings();n.putBoolean(key,!n.getBoolean(key));menu.action("settings",n);}
    private boolean save(){try{
        var n=settings();n.putInt("port",menu.port);
        if(settingsPage==0){int polling=Integer.parseInt(fields.get(0).getValue());if(polling<0)throw new NumberFormatException();n.putInt("polling",polling);n.putInt("priority",Integer.parseInt(fields.get(1).getValue()));String tag=fields.get(2).getValue();if(!tag.isEmpty()&&net.minecraft.resources.ResourceLocation.tryParse(tag)==null)throw new NumberFormatException();n.putString("tag",tag);menu.action("settings",n);}
        else if(settingsPage==1){n.putLong("maxSlot",positive(fields.get(0)));n.putLong("transferQuantity",positive(fields.get(1)));n.putLong("keepQuantity",nonnegative(fields.get(2)));menu.action("port_settings",n);}
        else {long interval=positive(fields.get(0));if(interval>Integer.MAX_VALUE)throw new NumberFormatException();n.putInt("transferInterval",(int)interval);long limit=nonnegative(fields.get(1));int slot=Integer.parseInt(fields.get(2).getValue())-1;if(slot<0||slot>=36)throw new NumberFormatException();var row=menu.data.getList("rows",Tag.TAG_COMPOUND).getCompound(slot);slot+=menu.page*36;menu.action("port_settings",n);n=new CompoundTag();n.putInt("port",menu.port);n.putInt("slot",slot);if(row.contains("filter"))n.put("key",row.getCompound("filter"));n.putLong("limit",limit);menu.action("port_filter",n);}
        return true;
    }catch(IllegalArgumentException ex){setTextContent("message",tr("invalid"));return false;}}
    private long positive(ConfirmableTextField f){long v=Long.parseLong(f.getValue());if(v<=0)throw new NumberFormatException();return v;}
    private long nonnegative(ConfirmableTextField f){long v=Long.parseLong(f.getValue());if(v<0)throw new NumberFormatException();return v;}
    @Override protected void updateBeforeRender(){super.updateBeforeRender();if(settingsPage>=0){if(!loaded&&!menu.data.isEmpty()){String[] values=settingsPage==0?new String[]{""+menu.data.getInt("polling"),""+menu.data.getInt("priority"),menu.data.getString("tag")}:settingsPage==1?new String[]{""+menu.data.getLong("maxSlot"),""+menu.data.getLong("transferQuantity"),""+menu.data.getLong("keepQuantity")}:new String[]{""+menu.data.getInt("transferInterval"),""+menu.data.getList("rows",Tag.TAG_COMPOUND).getCompound(Math.floorMod(selected,36)).getLong("limit"),""+(Math.floorMod(selected,36)+1)};for(int i=0;i<3;i++)fields.get(i).setValue(values[i]);loaded=true;}return;}
        String status=menu.data.getString("guard");if(status.isEmpty())status=menu.data.getBoolean("active")?"READY":"OFFLINE";
        setTextContent("status",fit(tr(status).copy().append(" · "+(menu.page+1)+"/"+Math.max(1,menu.data.getInt("pages"))),160));
        setTextContent("channel",menu.data.contains("portCount")?tr(menu.data.getBoolean("fluid")?"fluid":"item").copy().append(" · ").append(tr(menu.data.getBoolean("output")?"export":"import")):tr("filters"));
        for(int i=0;i<36;i++){var row=menu.data.getList("rows",Tag.TAG_COMPOUND).getCompound(i);setTextContent("amount"+i,Component.literal(row.contains("key")?compact(row.getLong("amount")):""));}
        setTextContent("message",AWConfigs.CLIENT.cells.hidden.showControlsHelp.get()||hasShiftDown()?fit(tr("hint"),210).copy().withStyle(net.minecraft.ChatFormatting.WHITE):Component.empty());
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){
        if(settingsPage==1||settingsPage==2){
            int index=settingsPage==1?0:1;var field=fields.get(index);
            if(delta!=0&&field.isMouseOver(x,y))try{
                var config=AWConfigs.SERVER.cells.interfaces;boolean fixed=config.interfaceMaxSlotSizeUseFixedValues.get();
                var values=fixed?config.interfaceMaxSlotSizeFixedValues.get():config.interfaceMaxSlotSizeOffsets.get();
                int magnitude=hasAltDown()?3:hasControlDown()?2:hasShiftDown()?1:0;
                field.setValue(Long.toString(AmountStepper.step(Long.parseLong(field.getValue()),delta>0,fixed,values,magnitude,settingsPage==1?1:0)));
                return true;
            }catch(NumberFormatException ignored){}
        }
        return super.mouseScrolled(x,y,delta);
    }
    @Override public void drawBG(net.minecraft.client.gui.GuiGraphics graphics,int left,int top,int mouseX,int mouseY,float partial){
        super.drawBG(graphics,left,top,mouseX,mouseY,partial);
        if(settingsPage<0)for(var semantic:CellsSlots.UPGRADE)for(var slot:menu.getSlots(semantic))
            Icon.SLOT_BACKGROUND.getBlitter().dest(left+slot.x-1,top+slot.y-1).blit(graphics);
    }
    static String compact(long value){if(value<1000)return ""+value;String[] suffix={"k","M","G","T","P","E"};double n=value;int i=-1;do{n/=1000;i++;}while(n>=1000&&i<5);return String.format(Locale.ROOT,"%.1f%s",n,suffix[i]);}
    static AEKey key(ItemStack stack,boolean fluid){if(stack.isEmpty())return null;if(GenericStack.isWrapped(stack))return GenericStack.fromItemStack(stack).what();if(fluid||hasControlDown()){var contained=FluidUtil.getFluidContained(stack);if(contained.isPresent())return AEFluidKey.of(contained.get());}return AEItemKey.of(stack);}
    private void filter(int index,AEKey key){var n=new CompoundTag();n.putInt("slot",menu.page*36+index);n.putInt("port",menu.port);if(key!=null)n.put("key",key.toTagGeneric());if(menu.data.contains("portCount")){n.putLong("limit",0);menu.action("port_filter",n);}else menu.action("filter",n);}
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top,int button){
        if(settingsPage<0)for(var semantic:com.mpp.aedialsworks.cells.menu.CellsSlots.UPGRADE)for(var slot:menu.getSlots(semantic))
            if(isHovering(slot,x,y))return false;
        return super.hasClickedOutside(x,y,left,top,button);
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(settingsPage<0)for(Slot slot:menu.slots)if(isHovering(slot,x,y)){int filter=menu.filterIndex(slot),buffer=menu.bufferIndex(slot);if(filter>=0){selected=menu.page*36+filter;if(button==1&&menu.data.contains("portCount"))open(2);else filter(filter,key(menu.getCarried(),menu.data.getBoolean("fluid")));return true;}if(buffer>=0){var n=new CompoundTag();n.putInt("port",menu.port);n.putInt("slot",menu.page*36+buffer);n.putBoolean("single",button==1);menu.action("buffer",n);return true;}}return super.mouseClicked(x,y,button);}
    @Override public boolean keyPressed(int code,int scan,int modifiers){if(settingsPage<0&&CellsClient.QUICK_ADD.matches(code,scan)){var hovered=getSlotUnderMouse();if(hovered!=null){var key=key(hovered.getItem(),menu.data.getBoolean("fluid"));if(key!=null)for(int i=0;i<36;i++){var slot=menu.getSlots(CellsSlots.FILTER[i]).get(0);if(slot.getItem().isEmpty()){filter(i,key);break;}}}return true;}return super.keyPressed(code,scan,modifiers);}
}
