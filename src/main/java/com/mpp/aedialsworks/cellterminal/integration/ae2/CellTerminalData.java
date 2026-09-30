package com.mpp.aedialsworks.cellterminal.integration.ae2;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import appeng.core.definitions.*;
import com.mpp.aedialsworks.common.registry.AWItems;
import com.mpp.aedialsworks.common.util.AWIds;
import com.mpp.aedialsworks.data.AWDataModule;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.*;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.LanguageProvider;
public final class CellTerminalData implements AWDataModule {
    @Override public void language(String locale,LanguageProvider provider){
        try(var input=CellTerminalData.class.getResourceAsStream("/assets/aedialsworks/cellterminal/"+locale+".json")){
            if(input==null)throw new IllegalStateException("Missing cell terminal translations: "+locale);
            var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            json.entrySet().forEach(entry->provider.add(entry.getKey(),entry.getValue().getAsString()));
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    @Override public void itemModels(ItemModelProvider provider){
        provider.withExistingParent("cell_terminal",new net.minecraft.resources.ResourceLocation("ae2","item/display_base"))
            .texture("front",AWIds.id("item/cellterminal/cell_terminal_outline"))
            .texture("front_bright",AWIds.id("item/cellterminal/cell_terminal_overlay"))
            .texture("front_medium",AWIds.id("item/cellterminal/cell_terminal_background"))
            .texture("front_dark",AWIds.id("item/cellterminal/cell_terminal_corners"));
        provider.withExistingParent("wireless_cell_terminal","item/generated").texture("layer0",AWIds.id("item/cellterminal/wireless_cell_terminal"));
    }
    @Override public void recipes(Consumer<FinishedRecipe> output){
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,AWItems.CELL_TERMINAL.get())
            .requires(AEParts.TERMINAL).requires(AEItems.LOGIC_PROCESSOR).requires(AEItems.ITEM_CELL_HOUSING)
            .unlockedBy("has_terminal",InventoryChangeTrigger.TriggerInstance.hasItems(AEParts.TERMINAL))
            .save(output,AWIds.id("cell_terminal"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,AWItems.WIRELESS_CELL_TERMINAL.get())
            .pattern("W").pattern("T").pattern("E").define('W',AEItems.WIRELESS_RECEIVER).define('T',AWItems.CELL_TERMINAL.get()).define('E',AEBlocks.DENSE_ENERGY_CELL)
            .unlockedBy("has_cell_terminal",InventoryChangeTrigger.TriggerInstance.hasItems(AWItems.CELL_TERMINAL.get()))
            .save(output,AWIds.id("wireless_cell_terminal"));
    }
}
