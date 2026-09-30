package com.mpp.aedialsworks.data;

import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.common.registry.AWCreativeTabs;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;

/** A runnable pipeline even before P1 registers any content; no placeholder game objects. */
public final class AWDataGenerators {
    // Add each completed feature's module here in stable order. Not gated by user config.
    private static final List<AWDataModule> MODULES = List.of(new com.mpp.aedialsworks.cellterminal.integration.ae2.CellTerminalData(),new com.mpp.aedialsworks.powertools.integration.ae2.PowerToolsData(),new com.mpp.aedialsworks.cells.integration.ae2.CellsData());

    private AWDataGenerators() {}

    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var existing = event.getExistingFileHelper();
        var lookup = event.getLookupProvider();
        for (String locale : List.of("en_us", "zh_cn", "ru_ru")) {
            generator.addProvider(event.includeClient(), new LanguageProvider(output, Aedialsworks.MODID, locale) {
                @Override protected void addTranslations() {
                    add(AWCreativeTabs.TITLE_KEY, "Aedial's Works");
                    add("gui.aedialsworks.widgets.scroll", switch (locale) {
                        case "zh_cn" -> "\u6eda\u52a8\u5217\u8868";
                        case "ru_ru" -> "\u041f\u0440\u043e\u043a\u0440\u0443\u0442\u043a\u0430 \u0441\u043f\u0438\u0441\u043a\u0430";
                        default -> "Scroll list";
                    });
                    MODULES.forEach(module -> module.language(locale, this));
                }
            });
        }
        generator.addProvider(event.includeClient(), new BlockStateProvider(output, Aedialsworks.MODID, existing) {
            @Override protected void registerStatesAndModels() {
                MODULES.forEach(module -> module.blockStates(this));
            }
        });
        generator.addProvider(event.includeClient(), new ItemModelProvider(output, Aedialsworks.MODID, existing) {
            @Override protected void registerModels() {
                MODULES.forEach(module -> module.itemModels(this));
            }
        });
        generator.addProvider(event.includeServer(), new RecipeProvider(output) {
            @Override protected void buildRecipes(Consumer<FinishedRecipe> recipes) {
                MODULES.forEach(module -> module.recipes(recipes));
            }
        });
        var blocks = new AWBlockTags(output, lookup, existing, MODULES);
        generator.addProvider(event.includeServer(), blocks);
        generator.addProvider(event.includeServer(), new AWItemTags(output, lookup, blocks.contentsGetter(), existing, MODULES));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(() -> new AWBlockLoot(MODULES), LootContextParamSets.BLOCK))));
    }
}
