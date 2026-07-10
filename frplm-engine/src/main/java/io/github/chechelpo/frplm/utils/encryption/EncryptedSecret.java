package io.github.chechelpo.frplm.utils.encryption;

import org.jetbrains.annotations.NotNull;

public record EncryptedSecret(byte @NotNull [] ciphertext, byte @NotNull [] nonce, int keyVersion) {}
