package com.mpp.aedialsworks.common.registry;

import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.common.util.AWIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class AWCreativeTabs {
    public static final String TITLE_KEY = AWIds.translation("itemGroup", "main");
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Aedialsworks.MODID);
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable(TITLE_KEY))
                    .icon(() -> new ItemStack(AWItems.CELL_TERMINAL.get()))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .withTabFactory(VisibleTab::new)
                    .displayItems((parameters, output) ->
                            AWItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());

    private AWCreativeTabs() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    /** Vanilla hides empty category tabs. Forge's factory keeps P0 visible without fake items. */
    private static final class VisibleTab extends CreativeModeTab {
        private VisibleTab(Builder builder) { super(builder); }

        @Override
        public boolean shouldDisplay() { return true; }
    }
}
