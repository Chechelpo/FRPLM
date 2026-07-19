package io.github.chechelpo.frplm.utils.encryption;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

final class MasterKeyProvider {

    private static final String ENV_KEY_NAME = "FRPLM_KEY";
    private static final int KEY_BYTES = 32;

    private MasterKeyProvider() {
    }

    static byte @NotNull [] loadOrCreate() {
        String environmentValue = System.getenv(ENV_KEY_NAME);

        if (environmentValue != null && !environmentValue.isBlank()) {
            return decodeAndValidate(environmentValue, ENV_KEY_NAME);
        }

        Path keyPath = resolveDefaultKeyPath();
        return loadOrCreateFileKey(keyPath);
    }

    private static Path resolveDefaultKeyPath() {
        String osName = System.getProperty("os.name")
                .toLowerCase();

        String userHome = System.getProperty("user.home");

        if (osName.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");

            if (localAppData != null && !localAppData.isBlank()) {
                return Path.of(localAppData, "frplm", "master.key");
            }

            return Path.of(userHome, "AppData", "Local", "frplm", "master.key");
        }

        if (osName.contains("mac")) {
            return Path.of(
                    userHome,
                    "Library",
                    "Application Support",
                    "frplm",
                    "master.key"
            );
        }

        String xdgDataHome = System.getenv("XDG_DATA_HOME");

        if (xdgDataHome != null && !xdgDataHome.isBlank()) {
            return Path.of(xdgDataHome, "frplm", "master.key");
        }

        return Path.of(
                userHome,
                ".local",
                "share",
                "frplm",
                "master.key"
        );
    }

    private static byte[] loadOrCreateFileKey(Path keyPath) {
        try {
            if (Files.exists(keyPath)) {
                if (!Files.isRegularFile(keyPath)) {
                    throw new IllegalStateException(
                            "Encryption key path is not a regular file: " + keyPath
                    );
                }

                String encoded = Files.readString(
                        keyPath,
                        StandardCharsets.US_ASCII
                ).trim();

                return decodeAndValidate(encoded, keyPath.toString());
            }

            Files.createDirectories(keyPath.getParent());

            byte[] generatedKey = new byte[KEY_BYTES];
            new SecureRandom().nextBytes(generatedKey);

            String encoded = Base64.getEncoder()
                    .encodeToString(generatedKey);

            try {
                Files.writeString(
                        keyPath,
                        encoded,
                        StandardCharsets.US_ASCII,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE
                );
            } catch (java.nio.file.FileAlreadyExistsException e) {
                /*
                 * Another process may have created the key between the
                 * existence check and CREATE_NEW.
                 */
                String existing = Files.readString(
                        keyPath,
                        StandardCharsets.US_ASCII
                ).trim();

                return decodeAndValidate(existing, keyPath.toString());
            }

            restrictPermissions(keyPath);

            return generatedKey;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to load or create encryption key at " + keyPath,
                    e
            );
        }
    }

    private static byte[] decodeAndValidate(
            String encoded,
            String source
    ) {
        final byte[] rawKey;

        try {
            rawKey = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Encryption key from " + source + " is not valid Base64",
                    e
            );
        }

        if (rawKey.length != KEY_BYTES) {
            throw new IllegalStateException(
                    "Encryption key from " + source +
                    " must decode to exactly " + KEY_BYTES + " bytes"
            );
        }

        return rawKey;
    }

    private static void restrictPermissions(Path keyPath) throws IOException {
        if (!Files.getFileStore(keyPath)
                .supportsFileAttributeView("posix")) {
            return;
        }

        Set<PosixFilePermission> permissions = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
        );

        Files.setPosixFilePermissions(keyPath, permissions);
    }
}