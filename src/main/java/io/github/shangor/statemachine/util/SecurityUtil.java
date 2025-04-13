
package io.github.shangor.statemachine.util;

import java.security.SecureRandom;
import java.util.Base64;

public class SecurityUtil {

    public static String sanitizeSQL(String sql) {
        return sql.replaceAll("'", "");
    }

    public static String generateRandomKey() {
        SecureRandom rand = new SecureRandom();
        byte[] randBytes = new byte[64];
        rand.nextBytes(randBytes);
        return Base64.getEncoder().encodeToString(randBytes);
    }
    private SecurityUtil() {}
}
