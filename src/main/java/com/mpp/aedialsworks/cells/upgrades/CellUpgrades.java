package com.mpp.aedialsworks.cells.upgrades;
import java.util.*;
import appeng.api.upgrades.IUpgradeInventory;
import com.mpp.aedialsworks.common.registry.AWCells;
import net.minecraft.world.item.Item;
public final class CellUpgrades {
    private CellUpgrades(){}
    public static int value(IUpgradeInventory inv,String prefix,int fallback){
        int result=fallback;
        for(var entry:AWCells.UPGRADES.entrySet())if(entry.getKey().startsWith(prefix)&&inv.isInstalled(entry.getValue().get())){
            String tail=entry.getKey().substring(prefix.length());
            int value=tail.equals("infinite")?Integer.MAX_VALUE:Integer.parseInt(tail.replace("x",""));
            result=Math.max(result,value);
        }
        return result;
    }
    public static boolean has(IUpgradeInventory inv,String name){var item=AWCells.UPGRADES.get(name);return item!=null&&inv.isInstalled(item.get());}
}
