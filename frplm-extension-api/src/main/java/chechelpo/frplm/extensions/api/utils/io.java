package chechelpo.frplm.extensions.api.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class io {
    public record WebAsset(
            byte @NotNull [] bytes,
            @NotNull String contentType
    ) {}
    public static @NotNull Optional<WebAsset> getAssetFromClassPath(Class<?> clazz, @NotNull String resourcePath,@NotNull String assetName) {
        String normalized = normalizeAssetName(assetName);

        if (normalized == null) {
            return Optional.empty();
        }

        String fullPath = joinResourcePath(resourcePath, normalized);

        return loadResourceBytes(clazz, fullPath)
                .map(bytes -> new WebAsset(bytes, detectContentType(normalized)));
    }

    private static @Nullable String normalizeAssetName(@NotNull String assetName) {
        String name = assetName.replace('\\', '/');

        while (name.startsWith("/")) {
            name = name.substring(1);
        }

        if (name.isBlank()) {
            return null;
        }

        // Prevent path traversal attacks like ../../secret.txt
        if (name.contains("..")) {
            return null;
        }

        return name;
    }

    private static @NotNull String joinResourcePath(
            @NotNull String basePath,
            @NotNull String assetName
    ) {
        String base = basePath;

        if (!base.startsWith("/")) {
            base = "/" + base;
        }

        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        return base + "/" + assetName;
    }

    protected static @NotNull Optional<byte[]> loadResourceBytes(
            @NotNull Class<?> anchor,
            @NotNull String absoluteResourcePath
    ) {
        try (var in = anchor.getResourceAsStream(absoluteResourcePath)) {
            if (in == null) {
                return Optional.empty();
            }

            return Optional.of(in.readAllBytes());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(
                    "Could not load resource: " + absoluteResourcePath,
                    e
            );
        }
    }

    private static @NotNull String detectContentType(@NotNull String assetName) {
        String lower = assetName.toLowerCase(java.util.Locale.ROOT);

        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (lower.endsWith(".mjs")) return "text/javascript; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".svg")) return "image/svg+xml; charset=utf-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".woff2")) return "font/woff2";

        return "application/octet-stream";
    }
}
