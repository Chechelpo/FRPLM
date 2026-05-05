package chechelpo.frplm.utils;

import chechelpo.frplm.utils.encryption.EncryptedSecret;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

@Service
public final class EncryptorService {
    private static final String ENV_KEY_NAME = "FRPLM_SECRET_KEY_B64";

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";

    private static final int AES_256_KEY_BYTES = 32;
    private static final int GCM_NONCE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private static final int CURRENT_KEY_VERSION = 1;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptorService() {
        this(loadKeyFromEnvironment());
    }

    public EncryptorService(byte @NotNull [] rawKey) {
        Objects.requireNonNull(rawKey, "rawKey cannot be null");

        if (rawKey.length != AES_256_KEY_BYTES) {
            throw new IllegalArgumentException(
                    "Encryption key must be exactly 32 bytes for AES-256"
            );
        }

        this.keySpec = new SecretKeySpec(rawKey, KEY_ALGORITHM);
    }

    public @NotNull EncryptedSecret encrypt(@NotNull String plaintext) {
        Objects.requireNonNull(plaintext, "plaintext cannot be null");

        try {
            byte[] nonce = new byte[GCM_NONCE_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    keySpec,
                    new GCMParameterSpec(GCM_TAG_BITS, nonce)
            );

            byte[] ciphertext = cipher.doFinal(
                    plaintext.getBytes(StandardCharsets.UTF_8)
            );

            return new EncryptedSecret(ciphertext, nonce, CURRENT_KEY_VERSION);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt secret", e);
        }
    }

    public @NotNull String decrypt(@NotNull EncryptedSecret secret) {
        Objects.requireNonNull(secret, "secret cannot be null");

        return decrypt(
                secret.ciphertext(),
                secret.nonce(),
                secret.keyVersion()
        );
    }

    public @NotNull String decrypt(
            byte @NotNull [] ciphertext,
            byte @NotNull [] nonce,
            int keyVersion
    ) {
        Objects.requireNonNull(ciphertext, "ciphertext cannot be null");
        Objects.requireNonNull(nonce, "nonce cannot be null");

        if (keyVersion != CURRENT_KEY_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported encryption key version: " + keyVersion
            );
        }

        if (nonce.length != GCM_NONCE_BYTES) {
            throw new IllegalArgumentException(
                    "AES-GCM nonce must be exactly " + GCM_NONCE_BYTES + " bytes"
            );
        }

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    keySpec,
                    new GCMParameterSpec(GCM_TAG_BITS, nonce)
            );

            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt secret", e);
        }
    }

    public @Nullable EncryptedSecret encryptNullable(@Nullable String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }

        return encrypt(plaintext);
    }

    public @Nullable String decryptNullable(
            byte @Nullable [] ciphertext,
            byte @Nullable [] nonce,
            Integer keyVersion
    ) {
        if (ciphertext == null || ciphertext.length == 0) {
            return null;
        }

        if (nonce == null || nonce.length == 0) {
            return null;
        }

        if (keyVersion == null) {
            return null;
        }

        return decrypt(ciphertext, nonce, keyVersion);
    }

    private static byte @NotNull [] loadKeyFromEnvironment() {
        String encoded = System.getenv(ENV_KEY_NAME);

        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException(
                    "Missing environment variable " + ENV_KEY_NAME
            );
        }

        byte[] rawKey;

        try {
            rawKey = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    ENV_KEY_NAME + " must be a valid Base64-encoded 32-byte key",
                    e
            );
        }

        if (rawKey.length != AES_256_KEY_BYTES) {
            throw new IllegalStateException(
                    ENV_KEY_NAME + " must decode to exactly 32 bytes"
            );
        }

        return rawKey;
    }

    public static @NotNull String generateBase64Key() {
        byte[] key = new byte[AES_256_KEY_BYTES];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}