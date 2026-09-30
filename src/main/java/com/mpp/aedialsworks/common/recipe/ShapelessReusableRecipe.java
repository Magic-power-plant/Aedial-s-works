package com.mpp.aedialsworks.common.recipe;
import java.util.*;
import com.google.gson.*;
import com.mpp.aedialsworks.common.registry.AWRecipeSerializers;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
/** Normal crafting recipe (including JEI's vanilla category), with exact reusable ingredient assignments. */
public final class ShapelessReusableRecipe extends ShapelessRecipe {
    private final Set<Integer> reusable;
    public ShapelessReusableRecipe(ResourceLocation id,String group,CraftingBookCategory category,ItemStack result,NonNullList<Ingredient> ingredients,Set<Integer> reusable){super(id,group,category,result,ingredients);this.reusable=Set.copyOf(reusable);}
    @Override public RecipeSerializer<?> getSerializer(){return AWRecipeSerializers.SHAPELESS_REUSABLE.get();}
    private boolean assign(CraftingContainer grid,int ingredient,boolean[] used,int[] slots){
        if(ingredient==getIngredients().size())return true;
        for(int s=0;s<grid.getContainerSize();s++)if(!used[s] && getIngredients().get(ingredient).test(grid.getItem(s))){
            used[s]=true;slots[ingredient]=s;if(assign(grid,ingredient+1,used,slots))return true;used[s]=false;
        }return false;
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid){
        var result=NonNullList.withSize(grid.getContainerSize(),ItemStack.EMPTY);
        for(int i=0;i<grid.getContainerSize();i++){var s=grid.getItem(i);if(s.hasCraftingRemainingItem())result.set(i,s.getCraftingRemainingItem());}
        int[] slots=new int[getIngredients().size()];
        if(assign(grid,0,new boolean[grid.getContainerSize()],slots))for(int i:reusable){int slot=slots[i];if(result.get(slot).isEmpty())result.set(slot,grid.getItem(slot).copyWithCount(1));}
        return result;
    }
    public static final class Serializer implements RecipeSerializer<ShapelessReusableRecipe>{
        @Override public ShapelessReusableRecipe fromJson(ResourceLocation id,JsonObject json){
            var ingredients=NonNullList.<Ingredient>create();var reusable=new HashSet<Integer>();
            var array=GsonHelper.getAsJsonArray(json,"ingredients");
            for(int i=0;i<array.size();i++) {var element=array.get(i);ingredients.add(Ingredient.fromJson(element));if(element.isJsonObject()&&GsonHelper.getAsBoolean(element.getAsJsonObject(),"reusable",false))reusable.add(i);}
            if(json.has("reusable"))for(var value:json.getAsJsonArray("reusable"))reusable.add(value.getAsInt());
            if(ingredients.isEmpty()||ingredients.size()>9||ingredients.stream().anyMatch(Ingredient::isEmpty)||reusable.stream().anyMatch(i->i<0||i>=ingredients.size()))throw new JsonSyntaxException("Invalid reusable crafting ingredients");
            return new ShapelessReusableRecipe(id,GsonHelper.getAsString(json,"group",""),CraftingBookCategory.MISC,ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json,"result")),ingredients,reusable);
        }
        @Override public ShapelessReusableRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buf){
            String group=buf.readUtf(256);int size=buf.readVarInt();if(size<1||size>9)throw new IllegalArgumentException("Invalid recipe size");
            var ingredients=NonNullList.<Ingredient>create();var reusable=new HashSet<Integer>();
            for(int i=0;i<size;i++){ingredients.add(Ingredient.fromNetwork(buf));if(buf.readBoolean())reusable.add(i);}return new ShapelessReusableRecipe(id,group,CraftingBookCategory.MISC,buf.readItem(),ingredients,reusable);
        }
        @Override public void toNetwork(FriendlyByteBuf buf,ShapelessReusableRecipe recipe){
            buf.writeUtf(recipe.getGroup(),256);buf.writeVarInt(recipe.getIngredients().size());for(int i=0;i<recipe.getIngredients().size();i++){recipe.getIngredients().get(i).toNetwork(buf);buf.writeBoolean(recipe.reusable.contains(i));}buf.writeItem(recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY));
        }
    }
}
