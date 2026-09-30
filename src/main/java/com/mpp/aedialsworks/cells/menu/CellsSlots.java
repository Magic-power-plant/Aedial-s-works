package com.mpp.aedialsworks.cells.menu;
import appeng.menu.*;
public final class CellsSlots {
    public static final SlotSemantic[] FILTER=register("filter",36),BUFFER=register("buffer",36),UPGRADE=register("upgrade",24);
    private static SlotSemantic[] register(String kind,int count){var out=new SlotSemantic[count];for(int i=0;i<count;i++)out[i]=SlotSemantics.register("cells_"+kind+"_"+i,false);return out;}
    private CellsSlots(){}
}
