package com.mpp.aedialsworks.smoke;

import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import com.mpp.aedialsworks.common.util.AWIds;
import com.mpp.aedialsworks.common.recipe.ShapelessReusableRecipe;

/** Runtime-only proof that the vanilla JEI crafting category accepts reusable recipes. */
@JeiPlugin
public final class JeiSmokeProbe implements IModPlugin {
    @Override public ResourceLocation getPluginUid(){return AWIds.id("p2_acceptance");}
    @Override public void onRuntimeAvailable(IJeiRuntime runtime){
        P2ClientSmoke.jeiShow=()->{
            var manager=runtime.getRecipeManager();
            var recipes=manager.createRecipeLookup(RecipeTypes.CRAFTING).get()
                    .filter(recipe->recipe.getId().equals(AWIds.id("storage_level_alarm_locator"))).toList();
            if(recipes.size()!=1||!(recipes.get(0) instanceof ShapelessReusableRecipe))
                throw new IllegalStateException("Reusable recipe missing from JEI crafting category");
            runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(RecipeTypes.CRAFTING),recipes,List.of());
        };
    }
}
