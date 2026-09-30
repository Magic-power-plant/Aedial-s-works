package com.mpp.aedialsworks.cells.client;
import appeng.client.gui.*;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.*;
import com.mpp.aedialsworks.cells.menu.CellConfigurationMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class CellConfigurationScreen extends AEBaseScreen<CellConfigurationMenu> {
    private final ConfirmableTextField tag;private boolean loaded;
    public CellConfigurationScreen(CellConfigurationMenu menu,Inventory inv,Component title){super(menu,inv,title,StyleManager.loadStyleDoc("/screens/aedialsworks/cells/cell.json"));setTextContent("dialog_title",Component.literal(net.minecraft.client.Minecraft.getInstance().font.plainSubstrByWidth(title.getString(),160)));
        tag=new ConfirmableTextField(style,font,0,0,0,0);tag.setMaxLength(128);tag.setBordered(false);tag.setOnConfirm(()->{var n=new CompoundTag();n.putString("tag",tag.getValue());menu.action("tag",n);});widgets.add("tag",tag);
        icon("channel",Icon.POWER_UNIT_AE,()->menu.action("channel",new CompoundTag()));icon("previous",Icon.ARROW_LEFT,()->page(-1));icon("next",Icon.ARROW_RIGHT,()->page(1));icon("fuzzy",Icon.SEARCH_DEFAULT,()->menu.action("fuzzy",new CompoundTag()));icon("save",Icon.ENTER,()->{var n=new CompoundTag();n.putString("tag",tag.getValue());menu.action("tag",n);});
    }
    private void icon(String id,Icon icon,Runnable action){var b=new IconButton(ignored->action.run()){@Override protected Icon getIcon(){return icon;}};b.setMessage(Component.translatable("gui.aedialsworks.cells."+id));widgets.add(id,b);}
    private void page(int delta){var n=new CompoundTag();n.putInt("page",Math.floorMod(menu.page+delta,Math.max(1,menu.data.getInt("pages"))));menu.action("view",n);}
    private void filter(int index,appeng.api.stacks.AEKey key){var n=new CompoundTag();n.putInt("slot",menu.page*36+index);if(key!=null)n.put("key",key.toTagGeneric());menu.action("filter",n);}
    @Override public void drawBG(net.minecraft.client.gui.GuiGraphics g,int left,int top,int mouseX,int mouseY,float partial){
        super.drawBG(g,left,top,mouseX,mouseY,partial);
        for(var slot:menu.slots)if(slot.x>=0&&slot.x<240&&slot.y>=0&&slot.y<256){
            Icon.SLOT_BACKGROUND.getBlitter().dest(left+slot.x-1,top+slot.y-1).blit(g);
        }
    }
    @Override protected void updateBeforeRender(){super.updateBeforeRender();if(!loaded&&!menu.data.isEmpty()){tag.setValue(menu.data.getString("tag"));loaded=true;}setTextContent("status",Component.literal(CellsScreen.compact(menu.data.getLong("used"))+" / "+CellsScreen.compact(menu.data.getLong("capacity"))+" B · "+(menu.page+1)+"/"+Math.max(1,menu.data.getInt("pages"))));setTextContent("channel",Component.translatable("gui.aedialsworks.cells."+(menu.data.getBoolean("fluid")?"fluid":"item")));setTextContent("message",Component.translatable("gui.aedialsworks.cells.cell_hint"));}
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top,int button){
        if(true)for(var semantic:com.mpp.aedialsworks.cells.menu.CellsSlots.UPGRADE)for(var slot:menu.getSlots(semantic))
            if(isHovering(slot,x,y))return false;
        return super.hasClickedOutside(x,y,left,top,button);
    }
    @Override public boolean mouseClicked(double x,double y,int button){for(var slot:menu.slots){int i=menu.filterIndex(slot);if(i>=0&&isHovering(slot,x,y)){filter(i,button==1?null:CellsScreen.key(menu.getCarried(),menu.data.getBoolean("fluid")));return true;}}return super.mouseClicked(x,y,button);}
    @Override public boolean keyPressed(int code,int scan,int modifiers){if(CellsClient.QUICK_ADD.matches(code,scan)){var hovered=getSlotUnderMouse();if(hovered!=null){var key=CellsScreen.key(hovered.getItem(),menu.data.getBoolean("fluid"));if(key!=null)for(int i=0;i<36;i++)if(menu.getSlots(com.mpp.aedialsworks.cells.menu.CellsSlots.FILTER[i]).get(0).getItem().isEmpty()){filter(i,key);break;}}return true;}return super.keyPressed(code,scan,modifiers);}
}
