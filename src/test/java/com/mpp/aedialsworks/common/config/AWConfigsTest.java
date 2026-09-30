package com.mpp.aedialsworks.common.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AWConfigsTest {
    @Test void serverPreservesLegacyDefaultsAndCorrectsInvalidValues() {
        var pair = new ForgeConfigSpec.Builder().configure(AWServerConfig::new);
        var data = CommentedConfig.inMemory();
        pair.getRight().correct(data);
        pair.getRight().setConfig(data);
        var server = pair.getLeft();
        assertEquals(524288, server.cellterminal.network.maxChunkBytes.get());
        assertEquals(10, server.cellterminal.network.minRefreshIntervalTicks.get());
        assertFalse(server.cellterminal.polling.storageBusPollingEnabled.get());
        assertEquals(1, server.powertools.crafter.baseCraftsPerOperation.get());
        assertEquals(2, server.powertools.maintainer.maxConcurrentCalculations.get());
        assertEquals(63, server.cells.general.hdItemMaxTypes.get());
        assertEquals(4, server.cells.general.compactingCellUpgradeSlots.get());
        assertEquals(6.0, server.cells.idleDrain.compactingIdleDrain.get());
        assertEquals("2147483647", server.cells.interfaces.interfaceMaxSlotSizeLimit.get());
        data.set("cellterminal.network.maxChunkBytes", 1);
        data.set("powertools.maintainer.maxConcurrentCalculations", 100);
        pair.getRight().correct(data);
        pair.getRight().afterReload();
        assertEquals(4096, server.cellterminal.network.maxChunkBytes.get());
        assertEquals(32, server.powertools.maintainer.maxConcurrentCalculations.get());
    }

    @Test void clientPreferencesAndCommonAvailabilityRemainSeparate() {
        var clientPair = new ForgeConfigSpec.Builder().configure(AWClientConfig::new);
        var data = CommentedConfig.inMemory();
        clientPair.getRight().correct(data);
        clientPair.getRight().setConfig(data);
        var client = clientPair.getLeft();
        assertEquals("SMALL", client.cellterminal.gui.terminalStyle.get());
        assertEquals("LIMIT_64", client.cellterminal.gui.subnetSlotLimit.get());
        assertEquals("DONT_SHOW", client.cellterminal.gui.subnetVisibility.get());
        assertEquals("SHOW_ALL", client.cellterminal.filters.cellItemCells.get());
        assertEquals(200, client.powertools.scanner.adaptiveTextScaleMaxPercent.get());
        assertFalse(client.cells.hidden.showControlsHelp.get());
        var commonPair = new ForgeConfigSpec.Builder().configure(AWCommonConfig::new);
        var commonData = CommentedConfig.inMemory();
        commonPair.getRight().correct(commonData);
        commonPair.getRight().setConfig(commonData);
        assertTrue(commonPair.getLeft().cells.enabledCells.enableCompactingCells.get());
        assertFalse(data.contains("cells.enabled_cells"));
    }

    @Test void reloadIsVisibleWithoutAStaleStaticSnapshot() {
        var pair = new ForgeConfigSpec.Builder().configure(AWServerConfig::new);
        var data = CommentedConfig.inMemory();
        pair.getRight().correct(data);
        pair.getRight().setConfig(data);
        assertEquals(20, pair.getLeft().cellterminal.polling.pollingInterval.get());
        data.set("cellterminal.polling.pollingInterval", 60);
        pair.getRight().afterReload();
        assertEquals(60, pair.getLeft().cellterminal.polling.pollingInterval.get());
    }

    @Test void decimalLongValidationRetainsAll64Bits() {
        assertTrue(ConfigValidators.positiveLong(Long.toString(Long.MAX_VALUE)));
        assertTrue(ConfigValidators.positiveLongOrUnlimited("-1"));
        assertFalse(ConfigValidators.positiveLongOrUnlimited("0"));
        assertFalse(ConfigValidators.positiveLongOrUnlimited("-2"));
        assertFalse(ConfigValidators.positiveLong("9223372036854775808"));
        assertFalse(ConfigValidators.positiveLong("1.5"));
        assertFalse(ConfigValidators.positiveLong(null));
        assertTrue(ConfigValidators.signedLong(Long.toString(Long.MIN_VALUE)));
    }
}
