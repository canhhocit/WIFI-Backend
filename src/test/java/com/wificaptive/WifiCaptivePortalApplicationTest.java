package com.wificaptive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WifiCaptivePortalApplicationTest {

    @Test
    void testLoadDotEnv_ExecutesSafely() {
        assertDoesNotThrow(WifiCaptivePortalApplication::loadDotEnv);
    }

    @Test
    void testLoadDotEnv_PreservesExistingSystemProperties() {
        System.setProperty("TEST_PRESERVED_KEY", "initial_val");
        WifiCaptivePortalApplication.loadDotEnv();
        org.junit.jupiter.api.Assertions.assertEquals("initial_val", System.getProperty("TEST_PRESERVED_KEY"));
    }
}
