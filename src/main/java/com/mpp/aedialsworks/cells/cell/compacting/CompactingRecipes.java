package com.mpp.aedialsworks.cells.cell.compacting;

import java.util.*;
import appeng.api.stacks.AEItemKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Uses the world's actual recipe manager; only exact reversible recipes without remainders qualify. */
public final class CompactingRecipes {
    private CompactingRecipes(){}
    public record Step(AEItemKey key,int ratio){}
    private static TransientCraftingContainer frame(AEItemKey key,int side){
        var frame=new TransientCraftingContainer(new AbstractContainerMenu(null,-1){public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}public boolean stillValid(Player p){return false;}},side,side);
        for(int i=0;i<side*side;i++)frame.setItem(i,key.toStack());return frame;
    }
    private static List<ItemStack> results(Level level,AEItemKey key,int side){
        var frame=frame(key,side);var result=new ArrayList<ItemStack>();
        for(var recipe:level.getRecipeManager().getRecipesFor(RecipeType.CRAFTING,frame,level)){
            if(recipe.getRemainingItems(frame).stream().anyMatch(s->!s.isEmpty()))continue;
            var output=recipe.assemble(frame,level.registryAccess());if(!output.isEmpty())result.add(output);
        }
        return result;
    }
    public static Step higher(Level level,AEItemKey key){
        for(int side:new int[]{3,2})for(var output:results(level,key,side)){
            if(output.getCount()!=1)continue;var candidate=AEItemKey.of(output);if(candidate.equals(key))continue;
            for(var reverse:results(level,candidate,1))if(reverse.getCount()==side*side&&key.matches(reverse))return new Step(candidate,side*side);
        }return null;
    }
    public static Step lower(Level level,AEItemKey key){
        for(var output:results(level,key,1)){
            int ratio=output.getCount();if(ratio!=4&&ratio!=9)continue;var candidate=AEItemKey.of(output);if(candidate.equals(key))continue;
            for(var reverse:results(level,candidate,ratio==9?3:2))if(reverse.getCount()==1&&key.matches(reverse))return new Step(candidate,ratio);
        }return null;
    }
    public static CompressionChain discover(Level level,AEItemKey main,int up,int down){
        var keys=new ArrayList<AEItemKey>();var adjacent=new ArrayList<Integer>();keys.add(main);var seen=new HashSet<AEItemKey>();seen.add(main);
        AEItemKey current=main;int mainIndex=0;
        for(int i=0;i<Math.min(up,15);i++){var next=higher(level,current);if(next==null||!seen.add(next.key))break;keys.add(0,next.key);adjacent.add(0,next.ratio);current=next.key;mainIndex++;}
        current=main;
        for(int i=0;i<Math.min(down,15);i++){var next=lower(level,current);if(next==null||!seen.add(next.key))break;keys.add(next.key);adjacent.add(next.ratio);current=next.key;}
        // Drop only extensions that cannot be represented by the public long resource API.
        var rates=new ArrayList<Long>(Collections.nCopies(keys.size(),1L));
        for(int i=keys.size()-2;i>=0;i--){long lower=rates.get(i+1);int ratio=adjacent.get(i);if(lower>Long.MAX_VALUE/ratio)return discover(level,main,Math.max(0,up-1),Math.max(0,down-1));rates.set(i,lower*ratio);}
        return new CompressionChain(keys,rates,mainIndex);
    }
}
