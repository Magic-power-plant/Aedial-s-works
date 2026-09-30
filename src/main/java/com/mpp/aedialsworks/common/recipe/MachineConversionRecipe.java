package com.mpp.aedialsworks.common.recipe;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import com.mpp.aedialsworks.common.registry.AWRecipeSerializers;
/** Block/part conversion preserves the complete dismantled machine payload, including long buffers and upgrades. */
public final class MachineConversionRecipe extends CustomRecipe {
    private final Ingredient input;private final ItemStack output;
    public MachineConversionRecipe(ResourceLocation id,Ingredient input,ItemStack output){super(id,CraftingBookCategory.MISC);this.input=input;this.output=output;}
    private ItemStack input(CraftingContainer inv){ItemStack found=ItemStack.EMPTY;for(int i=0;i<inv.getContainerSize();i++){var s=inv.getItem(i);if(!s.isEmpty()){if(!found.isEmpty()||!input.test(s))return ItemStack.EMPTY;found=s;}}return found;}
    @Override public boolean matches(CraftingContainer inv,Level level){return !input(inv).isEmpty();}
    @Override public ItemStack assemble(CraftingContainer inv,RegistryAccess access){var s=input(inv);if(s.isEmpty())return ItemStack.EMPTY;var result=output.copy();if(s.hasTag())result.setTag(s.getTag().copy());return result;}
    @Override public ItemStack getResultItem(RegistryAccess access){return output;}
    @Override public boolean canCraftInDimensions(int w,int h){return w*h>=1;}
    @Override public RecipeSerializer<?> getSerializer(){return AWRecipeSerializers.MACHINE_CONVERSION.get();}
    public static final class Serializer implements RecipeSerializer<MachineConversionRecipe>{
        public MachineConversionRecipe fromJson(ResourceLocation id,JsonObject n){var ingredients=GsonHelper.getAsJsonArray(n,"ingredients");if(ingredients.size()!=1||ingredients.get(0).isJsonNull())throw new JsonSyntaxException("machine_conversion requires exactly one non-null ingredient");return new MachineConversionRecipe(id,Ingredient.fromJson(ingredients.get(0)),ShapedRecipe.itemStackFromJson(n.getAsJsonObject("result")));}
        public MachineConversionRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return new MachineConversionRecipe(id,Ingredient.fromNetwork(b),b.readItem());}
        public void toNetwork(FriendlyByteBuf b,MachineConversionRecipe r){r.input.toNetwork(b);b.writeItem(r.output);}
    }
}
