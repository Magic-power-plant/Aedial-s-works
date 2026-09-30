package com.mpp.aedialsworks.common.recipe;
import com.google.gson.JsonObject;
import com.mpp.aedialsworks.common.registry.AWRecipeSerializers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
/** Multi-machine assembly accepts pristine ingredients; use one-to-one conversion for dismantled machines. */
public final class MachineAssemblyRecipe extends ShapelessRecipe {
    private MachineAssemblyRecipe(ShapelessRecipe source){super(source.getId(),source.getGroup(),source.category(),source.getResultItem(RegistryAccess.EMPTY),source.getIngredients());}
    @Override public boolean matches(CraftingContainer grid,Level level){for(int i=0;i<grid.getContainerSize();i++)if(grid.getItem(i).hasTag())return false;return super.matches(grid,level);}
    @Override public net.minecraft.world.item.ItemStack assemble(CraftingContainer grid,RegistryAccess access){for(int i=0;i<grid.getContainerSize();i++)if(grid.getItem(i).hasTag())return net.minecraft.world.item.ItemStack.EMPTY;return super.assemble(grid,access);}
    @Override public RecipeSerializer<?> getSerializer(){return AWRecipeSerializers.MACHINE_ASSEMBLY.get();}
    public static final class Serializer implements RecipeSerializer<MachineAssemblyRecipe>{
        private final ShapelessRecipe.Serializer delegate=new ShapelessRecipe.Serializer();
        public MachineAssemblyRecipe fromJson(ResourceLocation id,JsonObject json){return new MachineAssemblyRecipe(delegate.fromJson(id,json));}
        public MachineAssemblyRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return new MachineAssemblyRecipe(delegate.fromNetwork(id,b));}
        public void toNetwork(FriendlyByteBuf b,MachineAssemblyRecipe recipe){delegate.toNetwork(b,recipe);}
    }
}
