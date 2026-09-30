package com.mpp.aedialsworks.cellterminal.screen;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import com.mpp.aedialsworks.cellterminal.client.AdvancedSearchParser;
import com.mpp.aedialsworks.cellterminal.client.SearchFilterMode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Row-snapshot matching for H-8: plain text queries must match without going through advanced syntax. */
public class CellTerminalScreenSearchTest {

    private static CompoundTag entry(String name, String storageName) {
        CompoundTag entry = new CompoundTag();
        entry.putString("name", name);
        entry.putString("storageName", storageName);
        return entry;
    }

    private static AdvancedSearchParser.SearchMatcher matcherFor(String query) {
        AdvancedSearchParser.ParseResult parsed = AdvancedSearchParser.isAdvancedQuery(query) ? AdvancedSearchParser.parse(query) : null;

        return parsed != null && parsed.isSuccess() ? parsed.getMatcher() : null;
    }

    @Test
    public void plainTextQueryWithSpace_matchesNameSubstring() {
        var row = entry("Iron Ingot", "Drive Alpha");
        assertTrue(CellTerminalScreen.matchesQuery("iron ingot", matcherFor("iron ingot"), false, false, row, SearchFilterMode.MIXED));
        assertFalse(CellTerminalScreen.matchesQuery("gold block", matcherFor("gold block"), false, false, row, SearchFilterMode.MIXED));
    }

    @Test
    public void plainTextQuery_matchesStorageNameSubstring() {
        var row = entry("—", "Drive Alpha");
        assertTrue(CellTerminalScreen.matchesQuery("drive alpha", matcherFor("drive alpha"), false, false, row, SearchFilterMode.MIXED));
    }

    @Test
    public void emptyQuery_matchesEverything() {
        var row = entry("Iron Ingot", "Drive Alpha");
        assertTrue(CellTerminalScreen.matchesQuery("", matcherFor(""), false, false, row, SearchFilterMode.MIXED));
    }

    @Test
    public void advancedQuery_usesMatcherAndFailedParseMatchesNothing() {
        var row = entry("Iron Ingot", "Drive Alpha");
        row.putInt("priority", 5);
        assertTrue(CellTerminalScreen.matchesQuery("?$priority>0", matcherFor("?$priority>0"), false, false, row, SearchFilterMode.MIXED));
        row.putInt("priority", -1);
        assertFalse(CellTerminalScreen.matchesQuery("?$priority>0", matcherFor("?$priority>0"), false, false, row, SearchFilterMode.MIXED));
        // Parse failure surfaces as queryError in the screen; rows must not match
        assertFalse(CellTerminalScreen.matchesQuery("?$priority>0)", matcherFor("?$priority>0)"), false, false, row, SearchFilterMode.MIXED));
    }
}
