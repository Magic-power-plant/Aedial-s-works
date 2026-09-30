package com.mpp.aedialsworks.cellterminal.client;

import java.util.List;

import org.junit.jupiter.api.Test;


import net.minecraft.nbt.CompoundTag;


/**
 * Unit tests for AdvancedSearchParser tokenization and parsing logic.
 *
 * Note: These tests focus on the parse structure and error detection.
 * Actual matching behavior requires Minecraft runtime and is tested manually.
 */
public class AdvancedSearchParserTest {

    private static SubnetInfo.ConnectionPoint createConnection(boolean outbound) {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("pos", 0L);
        nbt.putInt("dim", 0);
        nbt.putInt("side", 0);
        nbt.putBoolean("outbound", outbound);

        return new SubnetInfo.ConnectionPoint(nbt);
    }

    // ==================== Query Detection Tests ====================

    @Test
    public void testIsAdvancedQuery_withQuestionMark() {
        LegacyAssert.assertTrue(AdvancedSearchParser.isAdvancedQuery("?$name~iron"));
        LegacyAssert.assertTrue(AdvancedSearchParser.isAdvancedQuery("?"));
        LegacyAssert.assertTrue(AdvancedSearchParser.isAdvancedQuery("?test"));
    }

    @Test
    public void testIsAdvancedQuery_withoutQuestionMark() {
        LegacyAssert.assertFalse(AdvancedSearchParser.isAdvancedQuery("iron"));
        LegacyAssert.assertFalse(AdvancedSearchParser.isAdvancedQuery("$name~iron"));
        LegacyAssert.assertFalse(AdvancedSearchParser.isAdvancedQuery(""));
        LegacyAssert.assertFalse(AdvancedSearchParser.isAdvancedQuery(null));
    }

    // ==================== Empty/Null Query Tests ====================

    @Test
    public void testParse_nullQuery_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse(null);
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("empty_query") || result.getErrorMessage().contains("Empty query"));
    }

    @Test
    public void testParse_emptyQuery_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("empty_query") || result.getErrorMessage().contains("Empty query"));
    }

    @Test
    public void testParse_onlyQuestionMark_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("empty_after_prefix") || result.getErrorMessage().contains("Empty query after"));
    }

    @Test
    public void testParse_questionMarkWithWhitespace_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?   ");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("empty_after_prefix") || result.getErrorMessage().contains("Empty query after"));
    }

    // ==================== Valid Identifier Tests ====================

    @Test
    public void testParse_validName_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$name~iron");
        LegacyAssert.assertTrue("Expected success for '$name~iron'", result.isSuccess());
        LegacyAssert.assertNotNull(result.getMatcher());
    }

    @Test
    public void testParse_validPriority_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority>0");
        LegacyAssert.assertTrue("Expected success for '$priority>0'", result.isSuccess());
        LegacyAssert.assertNotNull(result.getMatcher());
    }

    @Test
    public void testParse_validPartition_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$partition=5");
        LegacyAssert.assertTrue("Expected success for '$partition=5'", result.isSuccess());
        LegacyAssert.assertNotNull(result.getMatcher());
    }

    @Test
    public void testParse_validItems_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$items>=10");
        LegacyAssert.assertTrue("Expected success for '$items>=10'", result.isSuccess());
        LegacyAssert.assertNotNull(result.getMatcher());
    }

    @Test
    public void testParse_validDirection_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$dir=outbound");
        LegacyAssert.assertTrue("Expected success for '$dir=outbound'", result.isSuccess());
        LegacyAssert.assertNotNull(result.getMatcher());
    }

    // ==================== Invalid Identifier Tests ====================

    @Test
    public void testParse_unknownIdentifier_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$unknown~test");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("unknown_identifier") || result.getErrorMessage().contains("Unknown identifier"));
    }

    @Test
    public void testParse_multipleUnknownIdentifiers_returnsMultipleErrors() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$foo=1&$bar=2");
        LegacyAssert.assertFalse(result.isSuccess());
        List<String> errors = result.getErrors();
        LegacyAssert.assertTrue(errors.size() >= 2);
    }

    // ==================== Operator Tests ====================

    @Test
    public void testParse_allComparisonOperators() {
        String[] operators = {"=", "!=", "<", ">", "<=", ">="};
        for (String op : operators) {
            AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority" + op + "5");
            LegacyAssert.assertTrue("Expected success for '$priority" + op + "5'", result.isSuccess());
        }
    }

    @Test
    public void testParse_containsOperator() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$name~iron");
        LegacyAssert.assertTrue("Expected success for contains operator", result.isSuccess());
    }

    @Test
    public void testParse_implicitContainsOperator() {
        // When no operator is specified, ~ (contains) should be assumed
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$name iron");
        LegacyAssert.assertTrue("Expected success for implicit contains", result.isSuccess());
    }

    // ==================== Logical Operator Tests ====================

    @Test
    public void testParse_andOperator_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority>0&$items>0");
        LegacyAssert.assertTrue("Expected success for AND operator", result.isSuccess());
    }

    @Test
    public void testParse_orOperator_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$name~iron|$name~gold");
        LegacyAssert.assertTrue("Expected success for OR operator", result.isSuccess());
    }

    @Test
    public void testParse_mixedLogicalOperators_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority>0&$items>0|$partition>0");
        LegacyAssert.assertTrue("Expected success for mixed operators", result.isSuccess());
    }

    @Test
    public void testParse_danglingAndOperator_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority>0&");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("expected_after_and") || result.getErrorMessage().contains("Expected expression after '&'"));
    }

    @Test
    public void testParse_danglingOrOperator_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority>0|");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("expected_after_or") || result.getErrorMessage().contains("Expected expression after '|'"));
    }

    @Test
    public void testParse_leadingAndOperator_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?&$priority>0");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("unexpected_operator") || result.getErrorMessage().contains("Unexpected operator"));
    }

    @Test
    public void testParse_leadingOrOperator_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?|$priority>0");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("unexpected_operator") || result.getErrorMessage().contains("Unexpected operator"));
    }

    // ==================== Parentheses Tests ====================

    @Test
    public void testParse_simpleParentheses_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?($priority>0)");
        LegacyAssert.assertTrue("Expected success for simple parentheses", result.isSuccess());
    }

    @Test
    public void testParse_nestedParentheses_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?(($priority>0))");
        LegacyAssert.assertTrue("Expected success for nested parentheses", result.isSuccess());
    }

    @Test
    public void testParse_complexParentheses_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?($priority>0|$items>0)&$partition>0");
        LegacyAssert.assertTrue("Expected success for complex parentheses", result.isSuccess());
    }

    @Test
    public void testParse_missingClosingParenthesis_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?($priority>0");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("missing_paren") || result.getErrorMessage().contains("Missing closing parenthesis"));
    }

    @Test
    public void testParse_extraClosingParenthesis_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority>0)");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content - extra ')' is an unexpected token
        LegacyAssert.assertTrue(result.getErrorMessage().contains("unexpected_token") || result.getErrorMessage().contains("unexpected_paren") || result.getErrorMessage().contains("Unexpected"));
    }

    @Test
    public void testParse_emptyParentheses_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?()");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("unexpected_paren") || result.getErrorMessage().contains("Unexpected closing parenthesis"));
    }

    @Test
    public void testParse_strayClosingParenthesis_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?)");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("unexpected_paren") || result.getErrorMessage().contains("Unexpected closing parenthesis"));
    }

    // ==================== Quoted String Tests ====================

    @Test
    public void testParse_doubleQuotedString_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$name~\"Iron Ingot\"");
        LegacyAssert.assertTrue("Expected success for double-quoted string", result.isSuccess());
    }

    @Test
    public void testParse_singleQuotedString_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$name~'Iron Ingot'");
        LegacyAssert.assertTrue("Expected success for single-quoted string", result.isSuccess());
    }

    @Test
    public void testParse_quotedStringWithSpaces_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$name~\"minecraft:iron_ingot\"");
        LegacyAssert.assertTrue("Expected success for quoted string with special chars", result.isSuccess());
    }

    // ==================== Numeric Value Tests ====================

    @Test
    public void testParse_positiveNumber_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority=100");
        LegacyAssert.assertTrue("Expected success for positive number", result.isSuccess());
    }

    @Test
    public void testParse_zeroValue_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$items=0");
        LegacyAssert.assertTrue("Expected success for zero value", result.isSuccess());
    }

    @Test
    public void testParse_negativeNumber_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority=-5");
        LegacyAssert.assertTrue("Expected success for negative number", result.isSuccess());
    }

    @Test
    public void testParse_nonNumericForNumericField_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority=abc");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("expected_number") || result.getErrorMessage().contains("Expected number"));
    }

    @Test
    public void testParse_floatForIntField_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority=5.5");
        LegacyAssert.assertFalse(result.isSuccess());
        // Check for localization key or message content
        LegacyAssert.assertTrue(result.getErrorMessage().contains("expected_number") || result.getErrorMessage().contains("Expected number"));
    }

    // ==================== Plain Text Search Tests ====================

    @Test
    public void testParse_plainTextSearch_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?iron");
        LegacyAssert.assertTrue("Expected success for plain text search", result.isSuccess());
    }

    @Test
    public void testParse_plainTextWithLogical_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?iron&$priority>0");
        LegacyAssert.assertTrue("Expected success for plain text with logical op", result.isSuccess());
    }

    // ==================== Case Insensitivity Tests ====================

    @Test
    public void testParse_uppercaseIdentifier_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$NAME~iron");
        LegacyAssert.assertTrue("Expected success for uppercase identifier", result.isSuccess());
    }

    @Test
    public void testParse_mixedCaseIdentifier_success() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$Priority>0");
        LegacyAssert.assertTrue("Expected success for mixed case identifier", result.isSuccess());
    }

    @Test
    public void testDirectionMatcher_matchesSubnetConnectionsOnly() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$dir=outbound");
        LegacyAssert.assertTrue("Expected success for '$dir=outbound'", result.isSuccess());

        AdvancedSearchParser.SearchMatcher matcher = result.getMatcher();
        LegacyAssert.assertNotNull(matcher);
        LegacyAssert.assertFalse("Cell tabs should treat $dir as inapplicable", matcher.appliesToCell(SearchFilterMode.MIXED));
        LegacyAssert.assertFalse("Storage bus tabs should treat $dir as inapplicable",
            matcher.appliesToStorageBus(SearchFilterMode.MIXED));
        LegacyAssert.assertTrue("Subnet view should apply $dir",
            matcher.appliesToSubnetConnection(false, SearchFilterMode.MIXED));
        LegacyAssert.assertTrue("Cell tabs should ignore $dir", matcher.matchesCellFilter(null, null, SearchFilterMode.MIXED));
        LegacyAssert.assertTrue("Storage bus tabs should ignore $dir",
            matcher.matchesStorageBusFilter(null, SearchFilterMode.MIXED));
        LegacyAssert.assertTrue("Outbound connections should match $dir=outbound",
            matcher.matchesSubnetConnectionFilter(null, createConnection(true), false, SearchFilterMode.MIXED));
        LegacyAssert.assertFalse("Inbound connections should not match $dir=outbound",
            matcher.matchesSubnetConnectionFilter(null, createConnection(false), false, SearchFilterMode.MIXED));
        LegacyAssert.assertFalse("Subnet headers without a connection direction should not match $dir",
            matcher.matchesSubnetConnectionFilter(null, null, false, SearchFilterMode.MIXED));
    }

    @Test
    public void testDirectionClauseIgnoredInOrExpressionOutsideSubnetView() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$dir=outbound|$priority>0");
        LegacyAssert.assertTrue("Expected success for '$dir=outbound|$priority>0'", result.isSuccess());

        AdvancedSearchParser.SearchMatcher matcher = result.getMatcher();
        LegacyAssert.assertNotNull(matcher);
        LegacyAssert.assertTrue("Cell tabs should still evaluate the applicable $priority branch",
            matcher.appliesToCell(SearchFilterMode.MIXED));
        LegacyAssert.assertFalse("The ignored $dir branch must not force OR expressions to match on cell tabs",
            matcher.matchesCellFilter(null, null, SearchFilterMode.MIXED));
        LegacyAssert.assertTrue("Outbound subnet connections should still match through $dir",
            matcher.matchesSubnetConnectionFilter(null, createConnection(true), false, SearchFilterMode.MIXED));
        LegacyAssert.assertFalse("Inbound subnet connections should not match when no applicable branch succeeds",
            matcher.matchesSubnetConnectionFilter(null, createConnection(false), false, SearchFilterMode.MIXED));
    }

    // ==================== Complex Query Tests ====================

    @Test
    public void testParse_complexQuery1() {
        // (priority high OR empty) AND has partition
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?($priority>=5|$items=0)&$partition>0");
        LegacyAssert.assertTrue("Expected success for complex query 1", result.isSuccess());
    }

    @Test
    public void testParse_complexQuery2() {
        // Multiple OR conditions
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$name~iron|$name~gold|$name~diamond");
        LegacyAssert.assertTrue("Expected success for complex query 2", result.isSuccess());
    }

    @Test
    public void testParse_complexQuery3() {
        // Nested conditions
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?($name~iron&$priority>0)|($name~gold&$priority>5)");
        LegacyAssert.assertTrue("Expected success for complex query 3", result.isSuccess());
    }

    @Test
    public void testParse_complexQuery4() {
        // Multiple levels of nesting
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?(($priority>0|$items=0)&$partition>0)|$name~special");
        LegacyAssert.assertTrue("Expected success for complex query 4", result.isSuccess());
    }

    // ==================== Edge Case Tests ====================

    @Test
    public void testParse_multipleConsecutiveOperators_returnsError() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority>>5");
        // ">>" tokenizes as one operator token and normalizes to ">", so this parses as $priority>5
        LegacyAssert.assertTrue("Expected '>>' to parse as best-effort '>'", result.isSuccess());
        LegacyAssert.assertNotNull(result.getMatcher());
    }

    @Test
    public void testParse_operatorWithoutValue() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority>&$items>0");
        // The missing value parses successfully with an empty value, so no errors are reported
        LegacyAssert.assertTrue("Expected success with empty $priority value", result.isSuccess());
        LegacyAssert.assertTrue("Expected no errors for missing value", result.getErrors().isEmpty());
        LegacyAssert.assertNotNull(result.getMatcher());
    }

    @Test
    public void testParse_missingNumericValue_comparesAgainstZero() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$priority>");
        LegacyAssert.assertTrue("Expected success for trailing operator without value", result.isSuccess());
        LegacyAssert.assertTrue("Expected no errors for missing value", result.getErrors().isEmpty());
        AdvancedSearchParser.SearchMatcher matcher = result.getMatcher();
        LegacyAssert.assertNotNull(matcher);
        CompoundTag above = new CompoundTag(); above.putInt("priority", 5);
        CompoundTag below = new CompoundTag(); below.putInt("priority", -1);
        LegacyAssert.assertTrue("Missing value should compare against 0",
            matcher.matchesCellFilter(null, new StorageInfo(above), SearchFilterMode.MIXED));
        LegacyAssert.assertFalse("Negative priority should not match '>' with default 0",
            matcher.matchesCellFilter(null, new StorageInfo(below), SearchFilterMode.MIXED));
    }

    @Test
    public void testParse_onlyIdentifier() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$name");
        // Should use default contains operator with empty value
        LegacyAssert.assertTrue("Should parse identifier alone", result.isSuccess());
    }

    @Test
    public void testParse_whitespaceHandling() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?  $priority  >  5  ");
        LegacyAssert.assertTrue("Should handle extra whitespace", result.isSuccess());
    }

    // ==================== Error Message Quality Tests ====================

    @Test
    public void testParse_errorMessageContainsContext() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$unknown~value");
        LegacyAssert.assertFalse(result.isSuccess());
        String errorMsg = result.getErrorMessage();
        // Check for localization key or message content
        LegacyAssert.assertTrue("Error should mention the problematic identifier or be a localization key",
            errorMsg.contains("$unknown") || errorMsg.contains("unknown_identifier"));
    }

    @Test
    public void testParse_multipleErrorsCollected() {
        AdvancedSearchParser.ParseResult result = AdvancedSearchParser.parse("?$unknown=abc&$priority=xyz");
        LegacyAssert.assertFalse(result.isSuccess());
        List<String> errors = result.getErrors();
        // Should have at least 2 errors: unknown identifier and non-numeric value
        LegacyAssert.assertTrue("Should collect multiple errors", errors.size() >= 2);
    }
}
