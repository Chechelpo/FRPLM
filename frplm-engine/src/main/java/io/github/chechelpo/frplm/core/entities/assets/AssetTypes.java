package io.github.chechelpo.frplm.core.entities.assets;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum AssetTypes {
    AVATAR(
            "avatar.webp",
            "image/webp",
            false
    ),

    BACKGROUND(
            "background.webp",
            "image/webp",
            false
    );

    public final String fileName;
    public final String contentType;
    public final boolean isMultiple;

    AssetTypes(
            String fileName,
            String contentType,
            boolean isMultiple
    ) {
        this.fileName = Objects.requireNonNull(fileName);
        this.contentType = Objects.requireNonNull(contentType);
        this.isMultiple = isMultiple;
    }

    /**
     * Public API value, independent of the physical filename.
     */
    public String wireValue() {
        return name().toLowerCase();
    }

    public static Optional<AssetTypes> fromString(String value) {
        if (value == null) {
            return Optional.empty();
        }

        String normalized = value.trim();

        return Arrays.stream(values())
                .filter(type ->
                        type.wireValue()
                                .equalsIgnoreCase(normalized)
                )
                .findFirst();
    }

    public static AssetTypes requireValue(String value) {
        return fromString(value)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown asset type: " + value
                ));
    }
}