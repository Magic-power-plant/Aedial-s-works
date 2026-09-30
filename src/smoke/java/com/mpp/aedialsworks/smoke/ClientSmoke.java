package com.mpp.aedialsworks.smoke;

import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.common.config.AWConfigs;
import com.mpp.aedialsworks.common.registry.AWCreativeTabs;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in dev launch: reaches/render-checks the title screen, verifies the registered creative tab, then exits. */
@Mod.EventBusSubscriber(modid = Aedialsworks.MODID, value = Dist.CLIENT)
public final class ClientSmoke {
    private static int readyTicks;
    private static int elapsedTicks;
    private static boolean finished;

    private ClientSmoke() {}

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) throws Exception {
        if (!Boolean.getBoolean("aedialsworks.clientSmoke") || finished || event.phase != TickEvent.Phase.END) return;
        if (Boolean.getBoolean("aedialsworks.p3Smoke")) { P3ClientSmoke.tick(); return; }
        if (Boolean.getBoolean("aedialsworks.p2Smoke")) { P2ClientSmoke.tick(); return; }
        if (Boolean.getBoolean("aedialsworks.p1Smoke")) { P1ClientSmoke.tick(); return; }
        var minecraft = Minecraft.getInstance();
        if (++elapsedTicks > 2400) throw new IllegalStateException("P0 client smoke did not reach the title screen");
        if (!(minecraft.screen instanceof TitleScreen) || minecraft.getOverlay() != null) { readyTicks = 0; return; }
        if (++readyTicks < 40) return;
        var tab = AWCreativeTabs.MAIN.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.REGISTRY.allFlags(), true,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
        if (!tab.shouldDisplay() || tab.getDisplayItems().size() != com.mpp.aedialsworks.common.registry.AWItems.ITEMS.getEntries().size() || tab.getIconItem().isEmpty()) {
            throw new IllegalStateException("P0 tab must be visible, contain all registered items, and have an icon");
        }
        if (I18n.get(AWCreativeTabs.TITLE_KEY).equals(AWCreativeTabs.TITLE_KEY)) {
            throw new IllegalStateException("Generated language assets are missing");
        }
        if (!AWConfigs.COMMON_SPEC.isLoaded() || !AWConfigs.CLIENT_SPEC.isLoaded()) {
            throw new IllegalStateException("Client/common configuration did not load");
        }
        Path report = Path.of(System.getProperty("aedialsworks.smokeReportDir"));
        Files.createDirectories(report);
        try (var image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            image.writeToFile(report.resolve("title-screen.png"));
        }
        Files.writeString(report.resolve("client.txt"),
                "PASS: title screen rendered; registered creative tab visible; icon and generated translation present; common/client config loaded.\n",
                StandardCharsets.UTF_8);
        Aedialsworks.LOGGER.info("P0_CLIENT_SMOKE_PASS: main menu, registered creative tab, generated assets and configs verified");
        finished = true;
        minecraft.stop();
    }
}
