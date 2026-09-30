package com.mpp.aedialsworks.common.registry;

import com.mpp.aedialsworks.Aedialsworks;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Central registration point; feature domains declare their entries here. */
public final class AWRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Aedialsworks.MODID);

    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.common.recipe.ShapelessReusableRecipe.Serializer> SHAPELESS_REUSABLE=RECIPE_SERIALIZERS.register("shapeless_reusable",com.mpp.aedialsworks.common.recipe.ShapelessReusableRecipe.Serializer::new);
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<com.mpp.aedialsworks.common.recipe.CellComponentRecipe>> CELL_COMPONENT=RECIPE_SERIALIZERS.register("cell_component",()->new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(com.mpp.aedialsworks.common.recipe.CellComponentRecipe::new));
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.common.recipe.MachineConversionRecipe.Serializer> MACHINE_CONVERSION=RECIPE_SERIALIZERS.register("machine_conversion",com.mpp.aedialsworks.common.recipe.MachineConversionRecipe.Serializer::new);
    public static final net.minecraftforge.registries.RegistryObject<com.mpp.aedialsworks.common.recipe.MachineAssemblyRecipe.Serializer> MACHINE_ASSEMBLY=RECIPE_SERIALIZERS.register("machine_assembly",com.mpp.aedialsworks.common.recipe.MachineAssemblyRecipe.Serializer::new);
    private AWRecipeSerializers() {}

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }
}
