package com.mpp.aedialsworks.data;

import java.util.function.Consumer;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.LanguageProvider;

/** Feature domains implement only the data they own; register modules in AWDataGenerators. */
public interface AWDataModule {
    default void language(String locale, LanguageProvider provider) {}
    default void recipes(Consumer<FinishedRecipe> output) {}
    default void blockStates(BlockStateProvider provider) {}
    default void itemModels(ItemModelProvider provider) {}
    default void blockTags(AWBlockTags provider) {}
    default void itemTags(AWItemTags provider) {}
    default void blockLoot(AWBlockLoot provider) {}
}
