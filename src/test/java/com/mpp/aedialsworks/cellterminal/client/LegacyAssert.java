package com.mpp.aedialsworks.cellterminal.client;
/** Keep the original upstream assertions and messages unchanged on JUnit 5. */
final class LegacyAssert {
 static void assertTrue(boolean value) { org.junit.jupiter.api.Assertions.assertTrue(value); }
 static void assertTrue(String message, boolean value) { org.junit.jupiter.api.Assertions.assertTrue(value, message); }
 static void assertFalse(boolean value) { org.junit.jupiter.api.Assertions.assertFalse(value); }
 static void assertFalse(String message, boolean value) { org.junit.jupiter.api.Assertions.assertFalse(value, message); }
 static void assertNotNull(Object value) { org.junit.jupiter.api.Assertions.assertNotNull(value); }
 static void assertNotNull(String message, Object value) { org.junit.jupiter.api.Assertions.assertNotNull(value, message); }
}
