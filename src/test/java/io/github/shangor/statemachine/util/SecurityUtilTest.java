package io.github.shangor.statemachine.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilTest {

    @Test
    void testGenerateRandomKey() {
        String randomKey = SecurityUtil.generateRandomKey();
        assertNotNull(randomKey);
        assertEquals(88, randomKey.length()); // 64 bytes encoded in Base64 results in 88 characters
    }
}