package com.mpp.aedialsworks.cellterminal.integration.ae2;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.*;
import appeng.client.gui.layout.SlotGridLayout;
import appeng.menu.SlotSemantics;
import com.google.gson.JsonObject;
import com.mpp.aedialsworks.cellterminal.menu.CellTerminalMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
/** Each screen owns a mutable AE2 style; resizing never changes AE2's shared style cache. */
public abstract class TerminalScreenAdapter extends AEBaseScreen<CellTerminalMenu> {
    protected TerminalScreenAdapter(CellTerminalMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title,ownStyle());imageWidth=208;imageHeight=276;
    }
    private static ScreenStyle ownStyle(){
        var base=StyleManager.loadStyleDoc("/screens/common/common.json");
        var json=new JsonObject();var palette=new JsonObject();
        for(var color:PaletteColor.values())palette.addProperty(color.name(),base.getColor(color).toString());
        json.add("palette",palette);
        var toolbar=new JsonObject();toolbar.addProperty("left",-2);toolbar.addProperty("top",6);
        var widgets=new JsonObject();widgets.add("verticalToolbar",toolbar);json.add("widgets",widgets);
        return ScreenStyle.GSON.fromJson(json,ScreenStyle.class);
    }
    public final int panelHeight(){return imageHeight;}
    protected void prepareLayout(int visibleRows){
        int footer = 34 + visibleRows * 18;
        imageHeight = footer + 98;
        var inventory=new SlotPosition();inventory.setLeft(23);inventory.setTop(footer+17);inventory.setGrid(SlotGridLayout.BREAK_AFTER_9COLS);
        var hotbar=new SlotPosition();hotbar.setLeft(23);hotbar.setTop(footer+75);hotbar.setGrid(SlotGridLayout.HORIZONTAL);
        style.getSlots().put(SlotSemantics.PLAYER_INVENTORY.id(),inventory);style.getSlots().put(SlotSemantics.PLAYER_HOTBAR.id(),hotbar);
    }
}
