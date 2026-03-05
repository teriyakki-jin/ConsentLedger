package com.consentledger.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class HashGeneratorTest {

    @Test
    void printHashes() {
        var encoder = new BCryptPasswordEncoder();
        System.out.println("=== BCrypt Hashes ===");
        System.out.println("admin1234: " + encoder.encode("admin1234"));
        System.out.println("user1234:  " + encoder.encode("user1234"));

        // Verify the existing hash
        boolean adminOk = encoder.matches("admin1234",
                "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        System.out.println("Existing hash matches 'admin1234': " + adminOk);
    }
}
