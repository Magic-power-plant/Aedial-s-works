package com.mpp.aedialsworks.smoke;

import com.mpp.aedialsworks.common.config.AWConfigs;
import com.mpp.aedialsworks.common.network.AWNetwork;
import com.mpp.aedialsworks.common.network.PacketMenuData;
import com.mpp.aedialsworks.common.registry.AWCreativeTabs;
import com.mpp.aedialsworks.common.registry.AWItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("aedialsworks")
@PrefixGameTestTemplate(false)
public final class P0GameTests {
    @GameTest(template = "empty")
    public static void serverConfigAndRegistries(GameTestHelper helper) {
        helper.assertTrue(AWConfigs.SERVER_SPEC.isLoaded(), "Server spec must load on a dedicated server");
        helper.assertTrue(AWConfigs.COMMON_SPEC.isLoaded(), "Common spec must load on a dedicated server");
        helper.assertTrue(AWConfigs.SERVER.cellterminal.network.maxChunkBytes.get() == 524288, "Legacy chunk default");
        helper.assertTrue(AWItems.ITEMS.getEntries().size() == 179, "P1 + P2 + P3 register 179 real items");
        helper.assertTrue(AWCreativeTabs.MAIN.isPresent(), "Creative tab registration");
        helper.assertTrue(AWCreativeTabs.MAIN.get().shouldDisplay(), "Empty tab must remain visible");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void commonNetworkHasNoClientClassloading(GameTestHelper helper) throws Exception {
        // Resolving both packet classes on a physical server catches unsafe client references.
        Class.forName(PacketMenuData.class.getName());
        AWNetwork.init(); // Idempotent: commonSetup has already registered these IDs.
        helper.assertTrue(AWNetwork.PROTOCOL_VERSION.equals("6"), "Protocol version");
        helper.succeed();
    }
}
