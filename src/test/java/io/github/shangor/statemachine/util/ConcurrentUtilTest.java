package io.github.shangor.statemachine.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentUtilTest {

    /**
     * 019624eb-1c94-7cde-bbb1-c137ca6b68fe
     */
    @Test
    void uuidV7() {
        var str = ConcurrentUtil.uuidV7().toString();
        assertEquals(36, str.length());
        assertEquals('7', str.charAt(14));
    }
}