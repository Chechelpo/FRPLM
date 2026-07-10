package io.github.chechelpo.frplm.utils.encryption;

import io.github.chechelpo.frplm.utils.encryption.EncryptedSecret;
import io.github.chechelpo.frplm.utils.encryption.EncryptorService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptorServiceTest {
    private final EncryptorService encryptorService = new EncryptorService();

    @Test
    void lifecycle() {
        String apiKey = EncryptorService.generateBase64Key();

        EncryptedSecret encryptedSecret = encryptorService.encrypt(apiKey);
        assertNotNull(encryptedSecret);
        assertNotEquals(apiKey.trim().getBytes(),  encryptedSecret.nonce());
        assertNotEquals(apiKey.trim().getBytes(),  encryptedSecret.ciphertext());

        String decryptedSecret = encryptorService.decrypt(encryptedSecret);
        assertNotNull(decryptedSecret);
        assertEquals(apiKey, decryptedSecret);
    }
}